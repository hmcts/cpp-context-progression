package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.model.HearingListing;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This processor will split event progression.event.court-proceedings-initiated ( Which is created
 * from command progression.initiate-court-proceedings ) into ProsecutionCases , CourtDocuments and
 * ListHearingRequests to individually call private commands for each one of them
 */
@ServiceComponent(EVENT_PROCESSOR)
public class CourtProceedingsInitiatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtProceedingsInitiatedProcessor.class.getCanonicalName());
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";
    private static final String SEXUAL_OFFENCE_RR_LABEL = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private static final String SEXUAL_OFFENCE_RR_CODE = "YES";
    public static final String ENDORSABLE_FLAG = "endorsableFlag";

    private static final String CASE_STATUS = "caseStatus";
    private static final String CASE_EJECTED = "EJECTED";

    public static final String CASE_ID = "caseId";
    private static final String PUBLIC_PROGRESSION_EVENTS_GROUP_PROSECUTION_CASES_CREATED = "public.progression.group-prosecution-cases-created";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Inject
    private SummonsHearingRequestService summonsHearingRequestService;

    @Handles("progression.event.court-proceedings-initiated")
    public void handle(final JsonEnvelope jsonEnvelope) {
        final JsonObject event = jsonEnvelope.payloadAsJsonObject();

        final CourtReferral courtReferral = jsonObjectToObjectConverter.convert(
                event.getJsonObject("courtReferral"), CourtReferral.class);

        final boolean isGroupCases = isGroupCases(courtReferral.getProsecutionCases());
        LOGGER.info("progression.event.court-proceedings-initiated is executed for isGroupCases {}", isGroupCases);

        if (isGroupCases) {
            final JsonObject payload = createObjectBuilder()
                    .add("groupId", courtReferral.getProsecutionCases().get(0).getGroupId().toString())
                    .build();
            sender.send(Enveloper.envelop(payload)
                    .withName(PUBLIC_PROGRESSION_EVENTS_GROUP_PROSECUTION_CASES_CREATED)
                    .withMetadataFrom(jsonEnvelope));
        }

        enrichOffencesWithReportingRestriction(jsonEnvelope, courtReferral);

        final List<HearingListing> hearingListingList = generateHearingListing(courtReferral.getListHearingRequests());

        LOGGER.info("hearingListingList - {}", hearingListingList);

        if (!isGroupCases) {
            LOGGER.info("progression.event.court-proceedings-initiated is executed for non group cases ");
            summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, hearingListingList);
            progressionService.createProsecutionCases(jsonEnvelope, getProsecutionCasesList(jsonEnvelope, courtReferral.getProsecutionCases()));
        }

        final ListCourtHearing listCourtHearing = prepareListCourtHearing(jsonEnvelope, courtReferral, hearingListingList, isGroupCases);

        progressionService.updateHearingListingStatusToSentForListingWithMultipleRequest(jsonEnvelope,
                listCourtHearing.getHearings(), null,
                hearingListingList,
                isGroupCases, courtReferral.getProsecutionCases().size());

    }

    private List<HearingListing> generateHearingListing(final List<ListHearingRequest> listHearingRequests) {

        final Map<String, List<ListDefendantRequest>> hearings = new HashMap<>();

        if (isEmpty(listHearingRequests)) {
            return List.of();
        }

        final Map<String, List<ListHearingRequest>> hearingRequestList = new HashMap<>();

        listHearingRequests.forEach(listHearingRequest -> {

            final String key = generateKey(listHearingRequest);

            final List<ListDefendantRequest> listDefendantRequests = listHearingRequest.getListDefendantRequests();

            if (hearings.containsKey(key)) {
                final List<ListDefendantRequest> list = listDefendantRequests.stream()
                        .filter(listDefendantRequest -> isDefendantNotExist(listDefendantRequest.getDefendantId(), hearings.get(key)))
                        .toList();

                hearings.get(key).addAll(list);
                hearingRequestList.get(key).add(listHearingRequest);

            } else {
                hearings.put(key, new ArrayList<>(listDefendantRequests));
                hearingRequestList.put(key, new ArrayList<>(List.of(listHearingRequest)));
            }
        });

        return hearings.keySet().stream()
                .filter(key -> !hearings.get(key).isEmpty())
                .map(key -> new HearingListing(randomUUID(), key, hearingRequestList.get(key), hearings.get(key)))
                .toList();
    }

    private boolean isDefendantNotExist(final UUID defendantId, final List<ListDefendantRequest> listDefendantRequests) {
        return listDefendantRequests.parallelStream()
                .noneMatch(listDefendantRequest -> listDefendantRequest.getDefendantId().equals(defendantId));
    }

    private String generateKey(final ListHearingRequest listHearingRequest) {
        return "Location:" + listHearingRequest.getCourtCentre().getCourtHearingLocation() + ',' +
                "RoomId:" + listHearingRequest.getCourtCentre().getRoomId() + ',' +
                "ListedStartDateTime:" + listHearingRequest.getListedStartDateTime() + ',' +
                "WeekCommencingDate:" + (nonNull(listHearingRequest.getWeekCommencingDate()) ? listHearingRequest.getWeekCommencingDate().getStartDate() : null) + ',' +
                "EstimatedMinutes:" + listHearingRequest.getEstimateMinutes() + ',' +
                "Type:" + listHearingRequest.getHearingType().getId();
    }

    private void enrichOffencesWithReportingRestriction(final JsonEnvelope jsonEnvelope, final CourtReferral courtReferral) {
        final List<String> offenceCodes = courtReferral.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream().flatMap(defendant -> defendant.getOffences().stream()))
                .map(Offence::getOffenceCode)
                .distinct()
                .collect(Collectors.toList());

        final Optional<List<JsonObject>> offencesJsonObjectOptional = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, jsonEnvelope, requester);

        courtReferral.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .forEach(defendant -> populateReportingRestrictionsForOffences(offencesJsonObjectOptional, defendant));


    }

    @SuppressWarnings("squid:S1188")
    private void populateReportingRestrictionsForOffences(final Optional<List<JsonObject>> referenceDataOffencesJsonObjectOptional, final Defendant defendant) {
        final Function<JsonObject, String> offenceKey = offenceJsonObject -> offenceJsonObject.getString("cjsOffenceCode");

        final List<Offence> offencesWithReportingRestriction = new ArrayList<>();
        defendant.getOffences().forEach(offence -> populateReportingRestrictionForOffence(referenceDataOffencesJsonObjectOptional, defendant, offenceKey, offencesWithReportingRestriction, offence));
        if (isNotEmpty(defendant.getOffences())) {
            defendant.getOffences().clear();
            defendant.getOffences().addAll(offencesWithReportingRestriction);
        }
    }

    private void populateReportingRestrictionForOffence(final Optional<List<JsonObject>> referenceDataOffencesJsonObjectOptional, final Defendant defendant, final Function<JsonObject, String> offenceKey, final List<Offence> offencesWithReportingRestriction, final Offence offence) {
        final List<ReportingRestriction> reportingRestrictions = new ArrayList<>();
        populateYouthReportingRestriction(defendant, reportingRestrictions);
        final Offence.Builder builder = new Offence.Builder().withValuesFrom(offence);
        populateSexualReportingRestriction(referenceDataOffencesJsonObjectOptional, offenceKey, offence, reportingRestrictions, builder);

        if (isNotEmpty(offence.getReportingRestrictions())) {
            reportingRestrictions.addAll(offence.getReportingRestrictions());
        }

        if (isNotEmpty(reportingRestrictions)) {
            builder.withReportingRestrictions(dedupReportingRestrictions(reportingRestrictions));
        }

        offencesWithReportingRestriction.add(builder.build());
    }

    private void populateSexualReportingRestriction(final Optional<List<JsonObject>> referenceDataOffencesJsonObjectOptional, final Function<JsonObject, String> offenceKey, final Offence offence, final List<ReportingRestriction> reportingRestrictions, final Offence.Builder builder) {
        if (referenceDataOffencesJsonObjectOptional.isPresent()) {
            final Map<String, JsonObject> offenceCodeMap = referenceDataOffencesJsonObjectOptional.get().stream().collect(Collectors.toMap(offenceKey, Function.identity()));
            final JsonObject referenceDataOffenceInfo = offenceCodeMap.get(offence.getOffenceCode());
            if (nonNull(referenceDataOffenceInfo)) {
                builder.withEndorsableFlag(referenceDataOffenceInfo.getBoolean(ENDORSABLE_FLAG, false));
            }
            if (nonNull(referenceDataOffenceInfo) && equalsIgnoreCase(referenceDataOffenceInfo.getString("reportRestrictResultCode", StringUtils.EMPTY), SEXUAL_OFFENCE_RR_CODE)) {
                reportingRestrictions.add(new ReportingRestriction.Builder()
                        .withId(randomUUID())
                        .withLabel(SEXUAL_OFFENCE_RR_LABEL)
                        .withOrderedDate(LocalDate.now())
                        .build());
            }
        }
    }

    private void populateYouthReportingRestriction(final Defendant defendant, final List<ReportingRestriction> reportingRestrictions) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()) && LocalDateUtils.isYouth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), LocalDate.now()).booleanValue()) {
            reportingRestrictions.add(new ReportingRestriction.Builder()
                    .withId(randomUUID())
                    .withOrderedDate(LocalDate.now())
                    .withLabel(YOUTH_RESTRICTION).build());
        }
    }

    private List<ProsecutionCase> getProsecutionCasesList(final JsonEnvelope jsonEnvelope, final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .filter(pCase -> isNewCase(jsonEnvelope, pCase))
                .collect(toList());
    }

    private boolean isNewCase(final JsonEnvelope jsonEnvelope, final ProsecutionCase pCase) {
        String prosecutionAuthorityReference = pCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
        String caseURN = pCase.getProsecutionCaseIdentifier().getCaseURN();

        return getCaseByReference(jsonEnvelope, pCase,prosecutionAuthorityReference) &&  getCaseByReference(jsonEnvelope, pCase,caseURN) ;
    }

    private boolean getCaseByReference(final JsonEnvelope jsonEnvelope, final ProsecutionCase pCase, final String reference) {
        if (StringUtils.isNotEmpty(reference)) {
            Optional<JsonObject> jsonObject = progressionService.caseExistsByCaseUrn(jsonEnvelope, reference);
            if (jsonObject.isPresent() && !jsonObject.get().isEmpty() && !isCaseEjected(jsonEnvelope, jsonObject.get().getString(CASE_ID), pCase)) {
                LOGGER.info("Prosecution case {} already exists", reference);
                return false;
            }
        }
        return true;
    }


    private boolean isCaseEjected(final JsonEnvelope jsonEnvelope, final String reference, final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getMigrationSourceSystem() != null
                && prosecutionCase.getMigrationSourceSystem().getMigrationSourceSystemName() != null
                && progressionService.prosecutionCaseByCaseId(jsonEnvelope, reference)
                .map(jsonObject -> CASE_EJECTED.equals(jsonObject.getJsonObject("prosecutionCase").getString(CASE_STATUS,"NO_STATUS")))
                .orElse(false);
    }

    private ListCourtHearing prepareListCourtHearing(final JsonEnvelope jsonEnvelope, final CourtReferral courtReferral, final List<HearingListing> hearingListingList, final Boolean isGroupProceedings) {
        return listCourtHearingTransformer
                .transform(jsonEnvelope, courtReferral.getProsecutionCases(), hearingListingList, isGroupProceedings);
    }

    private boolean isGroupCases(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream().filter(prosecutionCase -> nonNull(prosecutionCase.getIsGroupMaster())).anyMatch(ProsecutionCase::getIsGroupMaster);
    }
}
