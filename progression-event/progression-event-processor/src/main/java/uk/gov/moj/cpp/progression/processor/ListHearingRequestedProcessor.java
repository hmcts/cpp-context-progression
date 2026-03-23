package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.listing.courts.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ListHearingRequestedProcessor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ListHearingRequestedProcessor.class);

    private static final String NEW_HEARING_NOTIFICATION_TEMPLATE_NAME = "NewHearingNotification";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private HearingNotificationHelper hearingNotificationHelper;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;


    @Handles("progression.event.list-hearing-requested")
    public void handle(final JsonEnvelope jsonEnvelope) {

        final ListHearingRequested listHearingRequested = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ListHearingRequested.class);

        final ListCourtHearing listCourtHearing = convertListCourtHearing(listHearingRequested, jsonEnvelope);

        final JsonObject eventPayload = jsonEnvelope.payloadAsJsonObject();
        final boolean sendNotification = eventPayload.getBoolean("sendNotificationToParties", false);

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);

        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        if (sendNotification) {
            sendHearingNotificationsToDefenceAndProsecutor(jsonEnvelope, listHearingRequested);
        } else {
            LOGGER.info("Notification is not sent for HearingId {}  , Notification sent flag {}", listHearingRequested.getHearingId(), false);
        }
    }

    @Handles("public.listing.hearing-requested-for-listing")
    public void handlePublicEvent(final JsonEnvelope jsonEnvelope) {
        final HearingRequestedForListing hearingRequestedForListing = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingRequestedForListing.class);

        final CourtCentre enrichedCourtCentre = progressionService.transformCourtCentre(hearingRequestedForListing.getListNewHearing().getCourtCentre(), jsonEnvelope);

        final HearingRequestedForListing enrichedHearingRequestedForListing = HearingRequestedForListing.hearingRequestedForListing()
                .withValuesFrom(hearingRequestedForListing)
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .withValuesFrom(hearingRequestedForListing.getListNewHearing())
                        .withCourtCentre(enrichedCourtCentre)
                        .build())
                .build();

        sender.send(
                Enveloper.envelop(objectToJsonObjectConverter.convert(enrichedHearingRequestedForListing))
                        .withName("progression.command.list-new-hearing")
                        .withMetadataFrom(jsonEnvelope));

    }

    @Handles("public.listing.hearing-listed")
    public void handlePublicHearingListed(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        if (payload == null || !payload.containsKey("caseUrns")) {
            return;
        }
        final String hearingId = payload.getString("hearingId", null);
        if (hearingId == null) {
            return;
        }

        final JsonArray caseUrns = payload.getJsonArray("caseUrns");
        if (caseUrns == null || caseUrns.isEmpty()) {
            return;
        }

        final List<CaseWithHearingDate> firstListingCases = caseUrns.stream()
                .filter(jsonValue -> jsonValue.getValueType() == JsonValue.ValueType.OBJECT)
                .map(jsonValue -> jsonValue.asJsonObject().getString("caseURN", null))
                .filter(Objects::nonNull)
                .map(caseUrn -> getProsecutionCaseByCaseUrn(jsonEnvelope, caseUrn, hearingId))
                .flatMap(Optional::stream)
                .filter(caseWithHearingDate -> isFirstHearingListing(caseWithHearingDate.prosecutionCase))
                .collect(Collectors.toList());

        if (!firstListingCases.isEmpty()) {
            updateDefendantYouthForFirstListing(jsonEnvelope, firstListingCases);
        }
    }

    private Optional<CaseWithHearingDate> getProsecutionCaseByCaseUrn(final JsonEnvelope jsonEnvelope,
                                                                      final String caseUrn,
                                                                      final String hearingId) {
        final Optional<JsonObject> caseIdJsonObject = progressionService.caseExistsByCaseUrn(jsonEnvelope, caseUrn);
        final String caseId = caseIdJsonObject.map(jsonObject -> jsonObject.getString(ProgressionService.CASE_ID, null))
                .orElse(null);
        if (caseId == null) {
            return Optional.empty();
        }

        final Optional<JsonObject> prosecutionCaseJson = progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId);
        if (prosecutionCaseJson.isEmpty() || !prosecutionCaseJson.get().containsKey("prosecutionCase")) {
            return Optional.empty();
        }

        final JsonObject caseDetail = prosecutionCaseJson.get();
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(
                caseDetail.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        final Optional<ZonedDateTime> earliestSittingDay = getEarliestHearingSittingDay(caseDetail, hearingId);
        if (earliestSittingDay.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new CaseWithHearingDate(prosecutionCase, earliestSittingDay.get()));
    }

    private Optional<ZonedDateTime> getEarliestHearingSittingDay(final JsonObject caseDetail, final String hearingId) {
        if (!caseDetail.containsKey("hearingsAtAGlance")) {
            return Optional.empty();
        }

        final JsonObject hearingsAtAGlance = caseDetail.getJsonObject("hearingsAtAGlance");
        if (hearingsAtAGlance == null || !hearingsAtAGlance.containsKey("hearings")) {
            return Optional.empty();
        }

        return hearingsAtAGlance.getJsonArray("hearings").stream()
                .filter(jsonValue -> jsonValue.getValueType() == JsonValue.ValueType.OBJECT)
                .map(jsonValue -> jsonValue.asJsonObject())
                .filter(hearingJson -> hearingId.equals(hearingJson.getString("id", null)))
                .findFirst()
                .map(hearingJson -> jsonObjectToObjectConverter.convert(hearingJson, Hearing.class))
                .map(Hearing::getHearingDays)
                .map(hearingDays -> hearingDays.stream()
                        .map(HearingDay::getSittingDay)
                        .min(ZonedDateTime::compareTo)
                        .orElse(null));
    }

    private void updateDefendantYouthForFirstListing(final JsonEnvelope jsonEnvelope,
                                                     final List<CaseWithHearingDate> prosecutionCases) {
        prosecutionCases.forEach(caseWithHearingDate -> ofNullable(caseWithHearingDate.prosecutionCase.getDefendants())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .forEach(defendant -> updateDefendantYouthIfEligible(
                        jsonEnvelope,
                        defendant,
                        caseWithHearingDate.prosecutionCase.getId(),
                        caseWithHearingDate.hearingDate.toLocalDate())));
    }

    private void updateDefendantYouthIfEligible(final JsonEnvelope jsonEnvelope,
                                                final Defendant defendant,
                                                final UUID prosecutionCaseId,
                                                final LocalDate firstSittingDay) {
        if (defendant.getPersonDefendant() == null
                || defendant.getPersonDefendant().getPersonDetails() == null
                || defendant.getPersonDefendant().getPersonDetails().getDateOfBirth() == null) {
            return;
        }

        final LocalDate dateOfBirth = defendant.getPersonDefendant().getPersonDetails().getDateOfBirth();
        if (!LocalDateUtils.isYouth(dateOfBirth, firstSittingDay)) {
            return;
        }

        final DefendantUpdate defendantUpdate = transformDefendantFromEntity(defendant, true, prosecutionCaseId);
        final JsonObject updateYouthPayload = createObjectBuilder()
                .add("defendant", objectToJsonObjectConverter.convert(defendantUpdate))
                .add("id", defendant.getId().toString())
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .build();

        sender.send(Enveloper.envelop(updateYouthPayload)
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withMetadataFrom(jsonEnvelope));
    }

    private DefendantUpdate transformDefendantFromEntity(final Defendant defendantEntity, final boolean isYouth, final UUID prosecutionCaseId) {
        return DefendantUpdate.defendantUpdate()
                .withIsYouth(isYouth)
                .withProsecutionCaseId(prosecutionCaseId)
                .withId(defendantEntity.getId())
                .withMasterDefendantId(defendantEntity.getMasterDefendantId())
                .withAssociatedPersons(defendantEntity.getAssociatedPersons())
                .withDefenceOrganisation(defendantEntity.getDefenceOrganisation())
                .withLegalEntityDefendant(defendantEntity.getLegalEntityDefendant())
                .withMitigation(defendantEntity.getMitigation())
                .withMitigationWelsh(defendantEntity.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendantEntity.getNumberOfPreviousConvictionsCited())
                .withPersonDefendant(defendantEntity.getPersonDefendant())
                .withProsecutionAuthorityReference(defendantEntity.getProsecutionAuthorityReference())
                .withWitnessStatement(defendantEntity.getWitnessStatement())
                .withWitnessStatementWelsh(defendantEntity.getWitnessStatementWelsh())
                .withCroNumber(defendantEntity.getCroNumber())
                .withPncId(defendantEntity.getPncId())
                .withAliases(defendantEntity.getAliases())
                .withJudicialResults(defendantEntity.getDefendantCaseJudicialResults())
                .withOffences(defendantEntity.getOffences())
                .withProceedingsConcluded(defendantEntity.getProceedingsConcluded())
                .build();
    }

    private boolean isFirstHearingListing(final ProsecutionCase prosecutionCaseEntity) {
        return ofNullable(prosecutionCaseEntity.getDefendants()).map(Collection::stream).orElseGet(Stream::empty)
                .map(Defendant::getOffences)
                .flatMap(offences -> ofNullable(offences).map(Collection::stream).orElseGet(Stream::empty))
                .map(Offence::getListingNumber)
                .noneMatch(Objects::nonNull);
    }

    private static final class CaseWithHearingDate {
        private final ProsecutionCase prosecutionCase;
        private final ZonedDateTime hearingDate;

        private CaseWithHearingDate(final ProsecutionCase prosecutionCase, final ZonedDateTime hearingDate) {
            this.prosecutionCase = prosecutionCase;
            this.hearingDate = hearingDate;
        }
    }

    @Handles("public.listing.hearing-partially-updated")
    public void handlePublicEventForPartiallyUpdate(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Handling public.listing.hearing-partially-updated {}", jsonEnvelope.payload());

        HearingPartiallyUpdated hearingPartiallyUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingPartiallyUpdated.class);
        UpdateHearingForPartialAllocation updateHearingForPartialAllocation = UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProsecutionCasesToRemove(hearingPartiallyUpdated.getProsecutionCases().stream()
                        .map(prosecutionCase -> ProsecutionCasesToRemove.prosecutionCasesToRemove()
                                .withCaseId(prosecutionCase.getCaseId())
                                .withDefendantsToRemove(prosecutionCase.getDefendants().stream()
                                        .map(defendant -> DefendantsToRemove.defendantsToRemove()
                                                .withDefendantId(defendant.getDefendantId())
                                                .withOffencesToRemove(defendant.getOffences().stream()
                                                        .map(offence -> OffencesToRemove.offencesToRemove()
                                                                .withOffenceId(offence.getOffenceId())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();

        sender.send(
                Enveloper.envelop(objectToJsonObjectConverter.convert(updateHearingForPartialAllocation))
                        .withName("progression.command.update-hearing-for-partial-allocation")
                        .withMetadataFrom(jsonEnvelope));
    }

    private ListCourtHearing convertListCourtHearing(ListHearingRequested listHearingRequested, final JsonEnvelope jsonEnvelope) {
        final Set<UUID> caseIds = listHearingRequested.getListNewHearing().getListDefendantRequests().stream()
                .map(ListDefendantRequest::getProsecutionCaseId)
                .collect(Collectors.toSet());

        final List<ProsecutionCase> cases = caseIds.stream().map(caseId -> progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .collect(Collectors.toList());

        return listCourtHearingTransformer.transform(jsonEnvelope, cases, listHearingRequested.getListNewHearing(), listHearingRequested.getHearingId());
    }

    private void sendHearingNotificationsToDefenceAndProsecutor(final JsonEnvelope jsonEnvelope, final ListHearingRequested listHearingRequested) {

        final HearingNotificationInputData hearingNotificationInputData = new HearingNotificationInputData();
        final Set<UUID> caseIds = listHearingRequested.getListNewHearing().getListDefendantRequests().stream()
                .map(ListDefendantRequest::getProsecutionCaseId)
                .collect(Collectors.toSet());
        hearingNotificationInputData.setCaseIds(new ArrayList<>(caseIds));

        final Set<UUID> defendantIdSet = new HashSet<>();
        final Map<UUID, List<UUID>> defendantOffenceListMap = new HashMap<>();
        listHearingRequested.getListNewHearing().getListDefendantRequests()
                .forEach(listDef -> {
                    defendantIdSet.add(listDef.getDefendantId());
                    defendantOffenceListMap.put(listDef.getDefendantId(), listDef.getDefendantOffences());
                });
        hearingNotificationInputData.setDefendantIds(new ArrayList<>(defendantIdSet));
        hearingNotificationInputData.setDefendantOffenceListMap(defendantOffenceListMap);
        hearingNotificationInputData.setTemplateName(NEW_HEARING_NOTIFICATION_TEMPLATE_NAME);

        hearingNotificationInputData.setHearingId(listHearingRequested.getHearingId());
        hearingNotificationInputData.setHearingDateTime(hearingNotificationHelper.getEarliestStartDateTime(listHearingRequested.getListNewHearing().getEarliestStartDateTime()));
        hearingNotificationInputData.setEmailNotificationTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()));
        hearingNotificationInputData.setCourtCenterId(listHearingRequested.getListNewHearing().getCourtCentre().getId());
        hearingNotificationInputData.setCourtRoomId(listHearingRequested.getListNewHearing().getCourtCentre().getRoomId());
        hearingNotificationInputData.setHearingType(listHearingRequested.getListNewHearing().getHearingType().getDescription());

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, hearingNotificationInputData);

    }

    public ListHearingRequestedProcessor() {
        super();
    }

}