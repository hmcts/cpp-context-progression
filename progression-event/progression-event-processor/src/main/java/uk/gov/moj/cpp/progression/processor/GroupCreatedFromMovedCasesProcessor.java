package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_RESULTED;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.GroupCreatedFromMovedCases;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the moved cases' current hearing and mirrors its session info into a new, independent
 * hearing for the newly-created group, then marks it SENT_FOR_LISTING immediately — the same
 * private command {@link CourtProceedingsInitiatedProcessor} already drives for brand-new bulk case
 * creation, which both updates progression's own view of the hearing and (via
 * ProsecutionCaseDefendantListingStatusChangedProcessor) notifies the listing context. This is an
 * event processor, not the command handler, because resolving the current hearing requires reading
 * the viewstore (CaseDefendantHearingRepository) — a read a command handler must not perform.
 *
 * <p>Also republishes {@code GroupCreatedFromMovedCases} publicly, unchanged, as
 * {@code public.progression.group-created-from-moved-cases} — the confirmation a caller (e.g. a
 * case-management UI) waits for before treating the move as successfully finished. This is sent
 * first, before hearing resolution: whether a current hearing exists to mirror is independent of
 * whether the move itself succeeded, so the confirmation must not depend on it.</p>
 */
@ServiceComponent(EVENT_PROCESSOR)
public class GroupCreatedFromMovedCasesProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCreatedFromMovedCasesProcessor.class);

    private static final String PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND = "progression.command.update-defendant-listing-status";
    private static final String PUBLIC_GROUP_CREATED_FROM_MOVED_CASES = "public.progression.group-created-from-moved-cases";
    private static final String HEARING_LISTING_STATUS = "hearingListingStatus";
    private static final String HEARING = "hearing";
    private static final String LIST_HEARING_REQUESTS = "listHearingRequests";
    private static final String SENT_FOR_LISTING = "SENT_FOR_LISTING";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.event.group-created-from-moved-cases")
    public void processEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.group-created-from-moved-cases {} ", event.toObfuscatedDebugString());
        }

        final GroupCreatedFromMovedCases groupCreatedFromMovedCases = jsonObjectConverter.convert(event.payloadAsJsonObject(), GroupCreatedFromMovedCases.class);
        final UUID leadCaseId = groupCreatedFromMovedCases.getLeadCase().getId();

        sender.send(enveloper.withMetadataFrom(event, PUBLIC_GROUP_CREATED_FROM_MOVED_CASES).apply(event.payloadAsJsonObject()));

        final Optional<HearingEntity> currentHearing = resolveCurrentHearing(leadCaseId);
        if (currentHearing.isEmpty()) {
            LOGGER.info("No current hearing found for lead case {} of newly-created group {} — nothing to mirror.", leadCaseId, groupCreatedFromMovedCases.getGroupId());
            return;
        }

        final Hearing oldHearing = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(currentHearing.get().getPayload()), Hearing.class);

        final List<ProsecutionCase> movedProsecutionCases = groupCreatedFromMovedCases.getCaseIds().stream()
                .map(this::loadProsecutionCase)
                .collect(Collectors.toList());

        final Hearing newHearing = Hearing.hearing()
                .withValuesFrom(oldHearing)
                .withId(randomUUID())
                .withProsecutionCases(movedProsecutionCases)
                .build();

        final ListHearingRequest listHearingRequest = buildListHearingRequest(oldHearing, movedProsecutionCases);

        final JsonObject hearingListingStatusCommand = createObjectBuilder()
                .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                .add(HEARING, objectToJsonObjectConverter.convert(newHearing))
                .add(LIST_HEARING_REQUESTS, toJsonArray(List.of(listHearingRequest)))
                .build();

        sender.send(enveloper.withMetadataFrom(event, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
    }

    /**
     * Without a populated {@code listHearingRequests},
     * {@code ProsecutionCaseDefendantListingStatusChangedProcessor}'s own
     * {@code !CollectionUtils.isEmpty(listHearingRequests)} guard never fires, so the listing
     * context is never told this hearing exists — it would only ever live inside progression's own
     * store. This mirrors the session info of the hearing being split (court centre, hearing type,
     * the first {@code HearingDay}'s sitting time/duration) onto a single request covering every
     * moved defendant, the same shape {@code CourtProceedingsInitiatedProcessor} sends for brand-new
     * bulk case creation.
     */
    private ListHearingRequest buildListHearingRequest(final Hearing oldHearing, final List<ProsecutionCase> movedProsecutionCases) {
        final List<ListDefendantRequest> listDefendantRequests = movedProsecutionCases.stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream()
                        .map(defendant -> ListDefendantRequest.listDefendantRequest()
                                .withProsecutionCaseId(prosecutionCase.getId())
                                .withDefendantId(defendant.getId())
                                .withDefendantOffences(defendant.getOffences().stream().map(Offence::getId).collect(Collectors.toList()))
                                .build()))
                .collect(Collectors.toList());

        final ListHearingRequest.Builder builder = ListHearingRequest.listHearingRequest()
                .withCourtCentre(oldHearing.getCourtCentre())
                .withHearingType(oldHearing.getType())
                .withJurisdictionType(oldHearing.getJurisdictionType())
                .withEstimatedDuration(oldHearing.getEstimatedDuration())
                .withPriority(oldHearing.getPriority())
                .withReportingRestrictionReason(oldHearing.getReportingRestrictionReason())
                .withSpecialRequirements(oldHearing.getSpecialRequirements())
                .withListDefendantRequests(listDefendantRequests);

        if (isNotEmpty(oldHearing.getHearingDays())) {
            final HearingDay firstHearingDay = oldHearing.getHearingDays().get(0);
            builder.withListedStartDateTime(firstHearingDay.getSittingDay())
                    .withEarliestStartDateTime(firstHearingDay.getSittingDay())
                    .withEstimateMinutes(firstHearingDay.getListedDurationMinutes());
        }

        return builder.build();
    }

    private <T> javax.json.JsonArray toJsonArray(final List<T> items) {
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        items.forEach(item -> arrayBuilder.add(objectToJsonObjectConverter.convert(item)));
        return arrayBuilder.build();
    }

    /**
     * Every case in a group hearing — not just the master — has a {@code case_defendant_hearing} row
     * (confirmed via {@code ProsecutionCaseDefendantListingStatusChangedListener}), so resolving from
     * the new lead case is sufficient; every moved case should currently resolve to the same hearing
     * pre-split. SENT_FOR_LISTING is a valid "current" status here, not excluded alongside
     * HEARING_RESULTED — the hearing being mirrored may itself still be pending listing confirmation.
     */
    private Optional<HearingEntity> resolveCurrentHearing(final UUID leadCaseId) {
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(leadCaseId);
        return caseDefendantHearingEntities.stream()
                .map(CaseDefendantHearingEntity::getHearing)
                .filter(hearingEntity -> HEARING_RESULTED != hearingEntity.getListingStatus())
                .findFirst();
    }

    private ProsecutionCase loadProsecutionCase(final UUID caseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        return jsonObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
    }
}
