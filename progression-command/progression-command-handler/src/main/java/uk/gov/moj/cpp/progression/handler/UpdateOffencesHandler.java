package uk.gov.moj.cpp.progression.handler;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.DefendantsOffences;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.UpdateDefendantOffences;
import uk.gov.justice.core.courts.UpdateHearingOffenceVerdict;
import uk.gov.justice.core.courts.UpdateIndexForBdf;
import uk.gov.justice.core.courts.UpdateListingNumber;
import uk.gov.justice.core.courts.UpdateOffencesForHearing;
import uk.gov.justice.core.courts.UpdateOffencesForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateOffencesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOffencesHandler.class.getName());
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    private static final String SOW_REF_VALUE = "MoJ";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Handles("progression.command.update-offences-for-prosecution-case")
    public void handle(final Envelope<UpdateOffencesForProsecutionCase> updateDefedantEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-offences-for-prosecution-case {}", updateDefedantEnvelope.payload());

        final UpdateOffencesForProsecutionCase updateDefendantCaseOffences = updateDefedantEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantCaseOffences.getDefendantCaseOffences().getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final UUID prosecutionCaseId = updateDefendantCaseOffences.getDefendantCaseOffences().getProsecutionCaseId();
        final UUID defendantId = updateDefendantCaseOffences.getDefendantCaseOffences().getDefendantId();
        final List<Offence> offenceList = updateDefendantCaseOffences.getDefendantCaseOffences().getOffences();
        final List<Offence> offences = TRUE.equals(updateDefendantCaseOffences.getSwitchedToYouth()) ? offenceList.stream().map(this::addYouthRestrictions).collect(Collectors.toList()) : offenceList;

        final List<String> offenceCodes = offences.stream().map(Offence::getOffenceCode).collect(Collectors.toList());
        final Optional<String> sowRef = getSowRef(caseAggregate.getProsecutionCase());
        final Optional<List<JsonObject>> offencesJsonObjectOptional = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelopeFrom(updateDefedantEnvelope.metadata(), JsonValue.NULL), requester, sowRef);

        final Stream<Object> events = caseAggregate.updateOffences(dedupAllReportingRestrictions(offences),prosecutionCaseId,defendantId,offencesJsonObjectOptional);

        appendEventsToStream(updateDefedantEnvelope, eventStream, events);

    }

    @Handles("progression.command.update-defendant-offences")
    public void handleUpdateDefendantOffences(final Envelope<UpdateDefendantOffences> updateDefendantOffenceEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-offences-for-prosecution-case {}", updateDefendantOffenceEnvelope.payload());

        final UpdateDefendantOffences updateDefendantOffences = updateDefendantOffenceEnvelope.payload();

        for (final DefendantsOffences updateDefendantOffence : updateDefendantOffences.getDefendantsOffences()) {
            final EventStream eventStream = eventSource.getStreamById(updateDefendantOffence.getDefendantCaseOffences().getProsecutionCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final UUID prosecutionCaseId = updateDefendantOffence.getDefendantCaseOffences().getProsecutionCaseId();
            final UUID defendantId = updateDefendantOffence.getDefendantCaseOffences().getDefendantId();
            final List<Offence> offenceList = updateDefendantOffence.getDefendantCaseOffences().getOffences();
            final List<Offence> offences = TRUE.equals(updateDefendantOffence.getSwitchedToYouth()) ? offenceList.stream().map(this::addYouthRestrictions).collect(Collectors.toList()) : offenceList;

            final List<String> offenceCodes = offences.stream().map(Offence::getOffenceCode).collect(Collectors.toList());
            final Optional<String> sowRef = getSowRef(caseAggregate.getProsecutionCase());
            final Optional<List<JsonObject>> offencesJsonObjectOptional = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelopeFrom(updateDefendantOffenceEnvelope.metadata(), JsonValue.NULL), requester, sowRef);

            final Stream<Object> events = caseAggregate.updateOffences(dedupAllReportingRestrictions(offences), prosecutionCaseId, defendantId, offencesJsonObjectOptional);

            appendEventsToStream(updateDefendantOffenceEnvelope, eventStream, events);
        }
    }

    @Handles("progression.command.update-offences-for-hearing")
    public void handleUpdateOffencesForHearing(final Envelope<UpdateOffencesForHearing> updateOffencesForHearingEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-offences-for-hearing {}", updateOffencesForHearingEnvelope.payload());
        final UpdateOffencesForHearing updateOffencesForHearing = updateOffencesForHearingEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateOffencesForHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateOffence(updateOffencesForHearing.getDefendantId(), dedupAllReportingRestrictions(updateOffencesForHearing.getUpdatedOffences()), dedupAllReportingRestrictions(updateOffencesForHearing.getNewOffences()));
        appendEventsToStream(updateOffencesForHearingEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-listing-number")
    public void handleUpdateListingNumberOfOffences(final Envelope<UpdateListingNumber> updateOffencesForHearingEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-offences-for-hearing {}", updateOffencesForHearingEnvelope.payload());

        final UpdateListingNumber updateListingNumber = updateOffencesForHearingEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateListingNumber.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateOffencesWithListingNumber(updateListingNumber.getOffenceListingNumbers());
        appendEventsToStream(updateOffencesForHearingEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-hearing-offence-verdict")
    public void handleUpdateHearingOffenceVerdict(final Envelope<UpdateHearingOffenceVerdict> updateHearingOffenceVerdictEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-hearing-offence-verdict {}", updateHearingOffenceVerdictEnvelope.payload());

        final UpdateHearingOffenceVerdict  updateHearingOffenceVerdict = updateHearingOffenceVerdictEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateHearingOffenceVerdict.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateHearingWithVerdict(updateHearingOffenceVerdict.getVerdict());
        appendEventsToStream(updateHearingOffenceVerdictEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-index-for-bdf")
    public void handleUpdateIndex(final Envelope<UpdateIndexForBdf> updateIndexForBdfEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-index-for-bdf {}", updateIndexForBdfEnvelope.payload());

        final UpdateIndexForBdf  updateIndexForBdf = updateIndexForBdfEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateIndexForBdf.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateIndex(updateIndexForBdf.getHearing(), updateIndexForBdf.getHearingListingStatus(), updateIndexForBdf.getNotifyNCES());
        appendEventsToStream(updateIndexForBdfEnvelope, eventStream, events);
    }

    private Offence addYouthRestrictions(final Offence offence) {
        final uk.gov.justice.core.courts.Offence.Builder builder = new uk.gov.justice.core.courts.Offence.Builder().withValuesFrom(offence);
        final List<ReportingRestriction> reportingRestrictions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(offence.getReportingRestrictions())) {
            reportingRestrictions.addAll(offence.getReportingRestrictions());
            if (offence.getReportingRestrictions().stream().noneMatch(reportingRestriction -> reportingRestriction.getLabel().equalsIgnoreCase(YOUTH_RESTRICTION))) {
                final ReportingRestriction youthRestriction = new ReportingRestriction(UUID.randomUUID(), null, YOUTH_RESTRICTION, LocalDate.now());
                reportingRestrictions.add(youthRestriction);
            }
        } else {
            reportingRestrictions.add(new ReportingRestriction(UUID.randomUUID(), null, YOUTH_RESTRICTION, LocalDate.now()));
        }
        return builder.withReportingRestrictions(dedupReportingRestrictions(reportingRestrictions)).build();
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

    private static Optional<String> getSowRef(final ProsecutionCase prosecutionCase) {
        boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();
        return isCivil ? Optional.of(SOW_REF_VALUE) : Optional.empty();
    }
}
