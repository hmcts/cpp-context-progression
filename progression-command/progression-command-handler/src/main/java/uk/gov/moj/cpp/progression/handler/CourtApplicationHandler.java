package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.ProsecutingAuthority.Builder;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.AddBreachApplication;
import uk.gov.justice.core.courts.AddCourtApplicationToCase;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationDefendantUpdateRequested;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.CreateCourtApplication;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.EditCourtApplicationProceedings;
import uk.gov.justice.core.courts.HearingResultedUpdateApplication;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.UpdateApplicationDefendant;
import uk.gov.justice.core.courts.UpdateCourtApplicationToHearing;
import uk.gov.justice.progression.courts.ApproveApplicationSummons;
import uk.gov.justice.progression.courts.RejectApplicationSummons;
import uk.gov.justice.progression.courts.SendNotificationForAutoApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"pmd:NullAssignment"})
public class CourtApplicationHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationHandler.class.getName());
    private static final String APPLICATION_ID = "applicationId";
    private static final UUID TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER = UUID.fromString("8b1cff00-a456-40da-9ce4-f11c20959084");
    private static final String WORDING_RESENTENCED = "Resentenced Original code : %1$s, Original details: %2$s";
    private static final String WORDING_SUSPENDED_RESENTENCED = "Activation of a suspended sentence order. Original code : %1$s, Original details: %2$s";

    private static final String WORDING_RE_SENTENCED_CLONED_OFFENCE = "Original CaseURN: %1$s, Re-sentenced Original code : %2$s, Original details: %3$s";
    private static final String WORDING_SUSPENDED_RE_SENTENCED_CLONED_OFFENCE = "Activation of a suspended sentence order. Original CaseURN: %1$s, Original code : %2$s, Original details: %3$s";

    private static final String PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY = "contactEmailAddress";
    private static final String PROSECUTOR_OUCODE_KEY = "oucode";
    private static final String PROSECUTOR_MAJOR_CREDITOR_CODE_KEY = "majorCreditorCode";

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
    private RefDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Handles("progression.command.create-court-application")
    public void handle(final Envelope<CreateCourtApplication> courtApplicationEnv) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.create-court-application {}", courtApplicationEnv.payload());
        }

        final CourtApplication application = courtApplicationEnv.payload().getCourtApplication();
        final UUID oldApplicationId = courtApplicationEnv.payload().getOldApplicationId();
        final EventStream eventStream = eventSource.getStreamById(application.getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.createCourtApplication(application, oldApplicationId);
        appendEventsToStream(courtApplicationEnv, eventStream, events);
    }

    @Handles("progression.command.list-or-refer-court-application")
    public void listOrReferApplication(final Envelope<JsonObject> envelope) throws EventStreamException {
        final JsonObject jsonObject = envelope.payload();
        final UUID applicationId = UUID.fromString(jsonObject.getString("id"));
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.referApplication();
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.add-court-application-to-case")
    public void courtApplicationAddedToCase(final Envelope<AddCourtApplicationToCase> addCourtApplicationToCaseEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-court-application-to-case {}", addCourtApplicationToCaseEnvelope.payload());
        }

        final AddCourtApplicationToCase addCourtApplicationToCase = addCourtApplicationToCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(addCourtApplicationToCase.getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.addApplicationToCase(addCourtApplicationToCase.getCourtApplication());
        appendEventsToStream(addCourtApplicationToCaseEnvelope, eventStream, events);
    }

    @Handles("progression.command.initiate-court-proceedings-for-application")
    public void initiateCourtApplicationProceedings(final Envelope<InitiateCourtApplicationProceedings> initiateCourtApplicationProceedingsEnv) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.initiate-court-proceedings-for-application {}", initiateCourtApplicationProceedingsEnv.payload());
        }

        final boolean applicationReferredToNewHearing = isApplicationReferredToNewHearing(initiateCourtApplicationProceedingsEnv.payload());
        final InitiateCourtApplicationProceedings initiateCourtProceedingsForApplication = rebuildInitiateCourtApplicationProceedings(initiateCourtApplicationProceedingsEnv.payload(), applicationReferredToNewHearing, initiateCourtApplicationProceedingsEnv);

        final EventStream eventStream = eventSource.getStreamById(initiateCourtProceedingsForApplication.getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);

        if (validateInitiateCourtApplicationProceedings(initiateCourtProceedingsForApplication)) {
            final ProsecutionCase prosecutionCase = getProsecutioncase(initiateCourtApplicationProceedingsEnv.metadata(), initiateCourtProceedingsForApplication.getCourtApplication());
            final boolean applicationCreatedForSJPCase = isApplicationCreatedForSJPCase(initiateCourtProceedingsForApplication.getCourtApplication().getCourtApplicationCases());
            final Stream<Object> events = applicationAggregate.initiateCourtApplicationProceedings(initiateCourtProceedingsForApplication, applicationReferredToNewHearing, applicationCreatedForSJPCase,prosecutionCase);
            appendEventsToStream(initiateCourtApplicationProceedingsEnv, eventStream, events);
        } else {
            final Stream<Object> events = applicationAggregate.ignoreApplicationProceedings(initiateCourtProceedingsForApplication);
            appendEventsToStream(initiateCourtApplicationProceedingsEnv, eventStream, events);
        }
    }

    private ProsecutionCase getProsecutioncase(final Metadata metadata, final CourtApplication courtApplication) {
        ProsecutionCase prosecutionCase = null;
        if(isNotEmpty(courtApplication.getCourtApplicationCases()) && nonNull(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId())){
            final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadata, JsonValue.NULL);
            final Optional<JsonObject> optionalProsecutionCase = prosecutionCaseQueryService.getProsecutionCase(jsonEnvelope,
                    courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId().toString());
            if (optionalProsecutionCase.isPresent()) {
                prosecutionCase = jsonObjectToObjectConverter.convert(optionalProsecutionCase.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
            }
        }
        return prosecutionCase;
    }

    @Handles("progression.command.send-notification-for-application")
    public void sendNotificationForApplication(final Envelope<SendNotificationForApplicationInitiated> sendNotificationForApplicationEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.send-notification-for-application {}", sendNotificationForApplicationEnvelope.payload());
        }

        final EventStream eventStream = eventSource.getStreamById(sendNotificationForApplicationEnvelope.payload().getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);

        final Stream<Object> events = applicationAggregate.sendNotificationForApplication(sendNotificationForApplicationEnvelope.payload());
        appendEventsToStream(sendNotificationForApplicationEnvelope, eventStream, events);

    }

    @Handles("progression.command.send-notification-for-auto-application")
    public void sendNotificationForAutopplication(final Envelope<SendNotificationForAutoApplication> sendNotificationForAutoApplicationEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.send-notification-for-auto-application {}", sendNotificationForAutoApplicationEnvelope.payload());
        }

        final EventStream eventStream = eventSource.getStreamById(sendNotificationForAutoApplicationEnvelope.payload().getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);

        final Stream<Object> events = applicationAggregate.sendNotificationForAutoApplication(sendNotificationForAutoApplicationEnvelope.payload().getCourtApplication(), sendNotificationForAutoApplicationEnvelope.payload().getCourtCentre(),
                sendNotificationForAutoApplicationEnvelope.payload().getJurisdictionType(), sendNotificationForAutoApplicationEnvelope.payload().getHearingStartDateTime());
        appendEventsToStream(sendNotificationForAutoApplicationEnvelope, eventStream, events);

    }

    @Handles("progression.command.edit-court-proceedings-for-application")
    public void editCourtApplicationProceedings(final Envelope<EditCourtApplicationProceedings> editCourtApplicationProceedingsEnvelope) throws EventStreamException {
        final EditCourtApplicationProceedings editCourtApplicationProceedings = rebuildEditCourtApplicationProceedings(editCourtApplicationProceedingsEnvelope.payload(), editCourtApplicationProceedingsEnvelope);
        final EventStream eventStream = eventSource.getStreamById(editCourtApplicationProceedings.getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final ProsecutionCase prosecutionCase = getProsecutioncase(editCourtApplicationProceedingsEnvelope.metadata(), editCourtApplicationProceedings.getCourtApplication());
        final Stream<Object> events = applicationAggregate.editCourtApplicationProceedings(editCourtApplicationProceedings,prosecutionCase);
        appendEventsToStream(editCourtApplicationProceedingsEnvelope, eventStream, events);
    }

    @Handles("progression.command.application-referred-to-court-hearing")
    public void initiateCourtHearingFromHearingResult(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.referApplicationToCourtHearing();
        appendEventsToStream(command, eventStream, events);
    }

    @Handles("progression.command.approve-application-summons")
    public void courtApplicationSummonsApproved(final Envelope<ApproveApplicationSummons> approveApplicationSummonsEnvelope) throws EventStreamException {
        final ApproveApplicationSummons approveApplicationSummons = approveApplicationSummonsEnvelope.payload();
        final UUID applicationId = approveApplicationSummons.getApplicationId();
        final SummonsApprovedOutcome summonsApprovedOutcome = approveApplicationSummons.getSummonsApprovedOutcome();
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.approveSummons(summonsApprovedOutcome);
        appendEventsToStream(approveApplicationSummonsEnvelope, eventStream, events);
    }

    @Handles("progression.command.reject-application-summons")
    public void courtApplicationSummonsRejected(final Envelope<RejectApplicationSummons> rejectApplicationSummonsEnvelope) throws EventStreamException {
        final UUID applicationId = rejectApplicationSummonsEnvelope.payload().getApplicationId();
        final SummonsRejectedOutcome summonsRejectedOutcome = rejectApplicationSummonsEnvelope.payload().getSummonsRejectedOutcome();
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.rejectSummons(summonsRejectedOutcome);
        appendEventsToStream(rejectApplicationSummonsEnvelope, eventStream, events);
    }

    @Handles("progression.command.add-breach-application")
    public void addBreachApplication(final Envelope<AddBreachApplication> addBreachApplicationEnvelope) throws EventStreamException {
        LOGGER.info("progression.command.add-breach-application {}", addBreachApplicationEnvelope.payload());
        final AddBreachApplication command = addBreachApplicationEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.addBreachApplication(command);
        appendEventsToStream(addBreachApplicationEnvelope, eventStream, events);
    }

    @Handles("progression.command.hearing-resulted-update-application")
    public void hearingResultedUpdateApplication(final Envelope<HearingResultedUpdateApplication> hearingResultedUpdateApplicationEnvelope) throws EventStreamException {
        LOGGER.info("progression.command.hearing-resulted-update-application {} ", hearingResultedUpdateApplicationEnvelope.payload().getCourtApplication().getId());
        final CourtApplication courtApplication = hearingResultedUpdateApplicationEnvelope.payload().getCourtApplication();
        final EventStream eventStream = eventSource.getStreamById(courtApplication.getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.hearingResulted(courtApplication);
        appendEventsToStream(hearingResultedUpdateApplicationEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-court-application-to-hearing")
    public void hearingUpdatedWithApplication(final Envelope<UpdateCourtApplicationToHearing> updateCourtApplicationToHearingEnvelope) throws EventStreamException {
        LOGGER.info("progression.command.update-court-application-to-hearing {} {} ", updateCourtApplicationToHearingEnvelope.payload().getCourtApplication().getId(),
                updateCourtApplicationToHearingEnvelope.payload().getHearingId());
        final CourtApplication courtApplication = updateCourtApplicationToHearingEnvelope.payload().getCourtApplication();
        final UUID hearingId = updateCourtApplicationToHearingEnvelope.payload().getHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateApplication(courtApplication);
        appendEventsToStream(updateCourtApplicationToHearingEnvelope, eventStream, events);
    }

   @Handles("progression.command.update.hearing.application.defendant")
    public void updateApplicationHearing(final Envelope<ApplicationDefendantUpdateRequested> applicationDefendantUpdateRequestedEnvelope) throws EventStreamException {
       if (LOGGER.isInfoEnabled()) {
           LOGGER.info("progression.command.update.hearing.application.defendant {} {} ", applicationDefendantUpdateRequestedEnvelope.payload().getDefendant().getMasterDefendantId(),
                   applicationDefendantUpdateRequestedEnvelope.payload().getHearingId());
       }
        final DefendantUpdate defendantUpdate = applicationDefendantUpdateRequestedEnvelope.payload().getDefendant();
        final UUID hearingId = applicationDefendantUpdateRequestedEnvelope.payload().getHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateApplicationHearing(defendantUpdate);
        appendEventsToStream(applicationDefendantUpdateRequestedEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-application-defendant")
    public void updateApplicationDefendant(final Envelope<UpdateApplicationDefendant> updateApplicationDefendantEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.command.update-application-defendant command received {}", updateApplicationDefendantEnvelope.payload().getCourtApplication().getId());
        }
        final UUID applicationId = updateApplicationDefendantEnvelope.payload().getCourtApplication().getId();
        final CourtApplication courtApplication = updateApplicationDefendantEnvelope.payload().getCourtApplication();
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.updateApplicationDefendant(courtApplication);
        appendEventsToStream(updateApplicationDefendantEnvelope, eventStream, events);
    }

    private InitiateCourtApplicationProceedings rebuildInitiateCourtApplicationProceedings(final InitiateCourtApplicationProceedings initiateCourtProceedingsForApplication,
                                                                                           final boolean applicationReferredToNewHearing, final Envelope<InitiateCourtApplicationProceedings> envelope) {
        final boolean summonsApprovalRequired = isSummonsApprovalRequired(initiateCourtProceedingsForApplication.getBoxHearing(), initiateCourtProceedingsForApplication.getCourtApplication());
        final InitiateCourtApplicationProceedings.Builder initiateCourtProceedingsForApplicationBuilder = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(rebuildCourtApplication(initiateCourtProceedingsForApplication.getCourtApplication(), envelope))
                .withSummonsApprovalRequired(summonsApprovalRequired)
                .withIsAmended(initiateCourtProceedingsForApplication.getIsAmended())
                .withOldApplicationId(initiateCourtProceedingsForApplication.getOldApplicationId())
                .withIsWelshTranslationRequired(initiateCourtProceedingsForApplication.getIsWelshTranslationRequired())
                .withIssueDate(initiateCourtProceedingsForApplication.getIssueDate());

        if (nonNull(initiateCourtProceedingsForApplication.getBoxHearing())) {
            initiateCourtProceedingsForApplicationBuilder
                    .withBoxHearing(BoxHearingRequest.boxHearingRequest()
                            .withValuesFrom(initiateCourtProceedingsForApplication.getBoxHearing())
                            .withId(UUID.randomUUID())
                            .build())
                    .build();
        }

        if (applicationReferredToNewHearing || summonsApprovalRequired) {
            if (nonNull(initiateCourtProceedingsForApplication.getCourtHearing())) {
                initiateCourtProceedingsForApplicationBuilder
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest()
                                .withValuesFrom(initiateCourtProceedingsForApplication.getCourtHearing())
                                .withId(randomUUID())
                                .build());
            }
        } else {
            initiateCourtProceedingsForApplicationBuilder.withCourtHearing(initiateCourtProceedingsForApplication.getCourtHearing());
        }

        return initiateCourtProceedingsForApplicationBuilder.build();
    }

    private EditCourtApplicationProceedings rebuildEditCourtApplicationProceedings(final EditCourtApplicationProceedings editCourtApplicationProceedings, final Envelope<EditCourtApplicationProceedings> envelope) {
        final boolean summonsApprovalRequired = isSummonsApprovalRequired(editCourtApplicationProceedings.getBoxHearing(), editCourtApplicationProceedings.getCourtApplication());
        final EditCourtApplicationProceedings.Builder editCourtProceedingsForApplicationBuilder = EditCourtApplicationProceedings.editCourtApplicationProceedings()
                .withValuesFrom(editCourtApplicationProceedings)
                .withCourtApplication(rebuildCourtApplication(editCourtApplicationProceedings.getCourtApplication(), envelope))
                .withSummonsApprovalRequired(summonsApprovalRequired);

        return editCourtProceedingsForApplicationBuilder.build();
    }

    private CourtApplication rebuildCourtApplication(final CourtApplication courtApplication, final Envelope<?> envelope) {

        final CourtApplication.Builder courtApplicationBuilder = CourtApplication.courtApplication()
                .withValuesFrom(courtApplication);

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);

        updateClonedCourtApplicationParties(courtApplicationBuilder, courtApplication, jsonEnvelope);
        updateClonedOffenceApplicationCases(courtApplicationBuilder, courtApplication);
        updateClonedOffenceCourtOrder(courtApplicationBuilder, courtApplication);

        return courtApplicationBuilder.build();
    }

    private String getOffenceWordingPattern(CourtApplication courtApplication) {
        final String wordingPattern;
        if (nonNull(courtApplication.getCourtOrder()) && TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER.equals(courtApplication.getCourtOrder().getJudicialResultTypeId())) {
            wordingPattern = WORDING_SUSPENDED_RESENTENCED;
        } else {
            wordingPattern = WORDING_RESENTENCED;
        }
        return wordingPattern;
    }

    private String getOffenceWordingPatternForClonedOffence(CourtApplication courtApplication) {
        final String wordingPattern;
        if (nonNull(courtApplication.getCourtOrder()) && TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER.equals(courtApplication.getCourtOrder().getJudicialResultTypeId())) {
            wordingPattern = WORDING_SUSPENDED_RE_SENTENCED_CLONED_OFFENCE;
        } else {
            wordingPattern = WORDING_RE_SENTENCED_CLONED_OFFENCE;
        }
        return wordingPattern;
    }

    private boolean hasActivationCode(CourtApplicationType type) {
        return StringUtils.isNotEmpty(type.getResentencingActivationCode());
    }

    private void updateClonedOffenceApplicationCases(final CourtApplication.Builder courtApplicationBuilder, final CourtApplication courtApplication) {
        if (isNull(courtApplication.getCourtApplicationCases())) {
            return;
        }

        if(LinkType.FIRST_HEARING == courtApplication.getType().getLinkType()) {
            // no cloning of offence required for first hearing scenario
            return;
        }

        final String wordingPattern = getOffenceWordingPattern(courtApplication);

        final String resentencingActivationCode = courtApplication.getType().getResentencingActivationCode();

        final List<CourtApplicationCase> courtApplicationCases = courtApplication.getCourtApplicationCases().stream()
                .map(courtApplicationCase -> {
                    if(activeCase(courtApplicationCase.getCaseStatus())) {
                        return courtApplicationCase()
                                .withValuesFrom(courtApplicationCase)
                                .withOffences(null)
                                .build();
                    } else {
                        return courtApplicationCase()
                                .withValuesFrom(courtApplicationCase)
                                .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                        .map(courtApplicationOffence -> updateOffence(courtApplication.getType(), courtApplicationOffence, wordingPattern, resentencingActivationCode))
                                        .collect(collectingAndThen(toList(), getListOrNull())))
                                .build();
                    }
                })
                .collect(toList());

        courtApplicationBuilder.withCourtApplicationCases(courtApplicationCases);
    }

    private boolean activeCase(final String caseStatus) {
        return nonNull(caseStatus) && !"INACTIVE".equalsIgnoreCase(caseStatus) && !"CLOSED".equalsIgnoreCase(caseStatus);
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    private void updateClonedOffenceCourtOrder(final CourtApplication.Builder courtApplicationBuilder, final CourtApplication courtApplication) {

        if (isNull(courtApplication.getCourtOrder())) {
            return;
        }

        final String wordingPattern = getOffenceWordingPatternForClonedOffence(courtApplication);

        final String resentencingActivationCode = courtApplication.getType().getResentencingActivationCode();

        final CourtOrder newCourtOrder = ofNullable(courtApplication.getCourtOrder())
                .map(courtOrder -> CourtOrder.courtOrder().withValuesFrom(courtOrder)
                        .withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence().withValuesFrom(courtOrderOffence)
                                        .withOffence(updateClonedOffence(courtApplication, courtOrderOffence.getProsecutionCaseIdentifier(), courtOrderOffence.getOffence(), wordingPattern, resentencingActivationCode))
                                        .build())
                                .collect(toList()))
                        .build())
                .orElse(null);

        courtApplicationBuilder.withCourtOrder(newCourtOrder);
    }

    private void updateClonedCourtApplicationParties(final CourtApplication.Builder courtApplicationBuilder, final CourtApplication courtApplication, final JsonEnvelope jsonEnvelope) {

        if (nonNull(courtApplication.getApplicant().getProsecutingAuthority())) {
            courtApplicationBuilder.withApplicant(enrichCourtApplicationParty(courtApplication.getApplicant(), jsonEnvelope));
        }
        if (nonNull(courtApplication.getSubject().getProsecutingAuthority())) {
            courtApplicationBuilder.withSubject(enrichCourtApplicationParty(courtApplication.getSubject(), jsonEnvelope));
        }

        ofNullable(courtApplication.getRespondents())
                .ifPresent(courtApplicationParties -> courtApplicationBuilder.withRespondents(courtApplicationParties.stream()
                        .map(respondent -> enrichCourtApplicationParty(respondent, jsonEnvelope))
                        .collect(toList())));


        final List<CourtApplicationParty> thirdParties = new ArrayList<>();
        ofNullable(courtApplication.getThirdParties()).ifPresent(thirdParties::addAll);

        final boolean prosecutorsAsThirdParty = checkForThirdPartyProsecutors(courtApplication);
        if (prosecutorsAsThirdParty) {
            ofNullable(buildThirdPartiesFromProsecutors(courtApplication)).ifPresent(thirdParties::addAll);
        }

        if (isNotEmpty(thirdParties)) {
            courtApplicationBuilder.withThirdParties(thirdParties.stream()
                    .map(thirdParty -> enrichCourtApplicationParty(thirdParty, jsonEnvelope))
                    .collect(toList()));
        }
    }

    private CourtApplicationParty enrichCourtApplicationParty(final CourtApplicationParty courtApplicationParty, final JsonEnvelope jsonEnvelope) {
        final CourtApplicationParty.Builder builder = CourtApplicationParty.courtApplicationParty().withValuesFrom(courtApplicationParty);
        if (isNameInformationEmpty(courtApplicationParty.getProsecutingAuthority())) {
            builder.withProsecutingAuthority(fetchProsecutingAuthorityInformation(courtApplicationParty.getProsecutingAuthority(), jsonEnvelope));
        }
        return builder.build();
    }

    @SuppressWarnings("pmd:NullAssignment")
    private ProsecutingAuthority fetchProsecutingAuthorityInformation(final ProsecutingAuthority prosecutingAuthority, final JsonEnvelope jsonEnvelope) {

        final Builder prosecutingAuthorityBuilder = prosecutingAuthority().withValuesFrom(prosecutingAuthority);

        final Optional<JsonObject> optionalProsecutorJson = referenceDataService.getProsecutor(jsonEnvelope, prosecutingAuthority.getProsecutionAuthorityId(), requester);
        if (optionalProsecutorJson.isPresent()) {
            final JsonObject jsonObject = optionalProsecutorJson.get();
            prosecutingAuthorityBuilder.withName(jsonObject.getString("fullName"))
                    .withWelshName(jsonObject.getString("nameWelsh", null))
                    .withAddress(isNull(jsonObject.getJsonObject("address")) ? null : jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("address"), Address.class));

            if (jsonObject.containsKey(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY)) {
                prosecutingAuthorityBuilder.withContact(contactNumber()
                        .withPrimaryEmail(jsonObject.getString(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY))
                        .build());
            }

            if (jsonObject.containsKey(PROSECUTOR_OUCODE_KEY)) {
                prosecutingAuthorityBuilder.withProsecutionAuthorityOUCode(jsonObject.getString(PROSECUTOR_OUCODE_KEY));
            }

            if (jsonObject.containsKey(PROSECUTOR_MAJOR_CREDITOR_CODE_KEY)) {
                prosecutingAuthorityBuilder.withMajorCreditorCode(jsonObject.getString(PROSECUTOR_MAJOR_CREDITOR_CODE_KEY));
            }
        }
        return prosecutingAuthorityBuilder.build();
    }

    private boolean isNameInformationEmpty(final ProsecutingAuthority prosecutingAuthority) {
        return nonNull(prosecutingAuthority) && isBlank(prosecutingAuthority.getFirstName()) && isBlank(prosecutingAuthority.getName());
    }

    private Offence updateOffence(final CourtApplicationType courtApplicationType, final Offence offence, final String wordingPattern, final String resentencingActivationCode) {
        final Offence.Builder offenceBuilder = Offence.offence()
                .withValuesFrom(offence)
                .withJudicialResults(null)
                .withCustodyTimeLimit(null);

        if (!offence.getOffenceCode().equals(resentencingActivationCode) && hasActivationCode(courtApplicationType)) {
            offenceBuilder.withWording(String.format(wordingPattern, offence.getOffenceCode(), offence.getWording()))
                    .withOffenceCode(resentencingActivationCode);

            if (nonNull(offence.getWordingWelsh())) {
                offenceBuilder.withWordingWelsh(String.format(wordingPattern, offence.getOffenceCode(), offence.getWordingWelsh()));
            }
        }

        return offenceBuilder.build();

    }

    private Offence updateClonedOffence(final CourtApplication courtApplication, final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final Offence offence, final String wordingPattern, final String resentencingActivationCode) {
        final Offence.Builder offenceBuilder = Offence.offence()
                .withValuesFrom(offence)
                .withJudicialResults(null)
                .withCustodyTimeLimit(null);

        if (!offence.getOffenceCode().equals(resentencingActivationCode) && hasActivationCode(courtApplication.getType())) {
            offenceBuilder.withWording(String.format(wordingPattern, prosecutionCaseIdentifier.getCaseURN(), offence.getOffenceCode(),  offence.getWording()))
                    .withOffenceCode(resentencingActivationCode);

            if (nonNull(offence.getWordingWelsh())) {
                offenceBuilder.withWordingWelsh(String.format(wordingPattern, prosecutionCaseIdentifier.getCaseURN(), offence.getOffenceCode(), offence.getWordingWelsh()));
            }
        }

        return offenceBuilder.build();

    }

    private boolean isSummonsApprovalRequired(final BoxHearingRequest boxHearing, final CourtApplication courtApplication) {
        final SummonsTemplateType summonsTemplateType = courtApplication.getType().getSummonsTemplateType();
        return summonsTemplateType != SummonsTemplateType.NOT_APPLICABLE && nonNull(boxHearing);
    }

    private boolean isApplicationReferredToNewHearing(final InitiateCourtApplicationProceedings initiateCourtProceedingsForApplication) {
        final CourtHearingRequest courtHearing = initiateCourtProceedingsForApplication.getCourtHearing();
        final BoxHearingRequest boxHearing = initiateCourtProceedingsForApplication.getBoxHearing();
        return nonNull(courtHearing) && isNull(courtHearing.getId()) && isNull(boxHearing);
    }

    private boolean validateInitiateCourtApplicationProceedings(final InitiateCourtApplicationProceedings initiateCourtProceedingsForApplication) {


        if (isNotEmpty(initiateCourtProceedingsForApplication.getCourtApplication().getCourtApplicationCases())) {
            final Predicate<CourtApplicationCase> isSjpCourtApplication = CourtApplicationCase::getIsSJP;
            final Predicate<CourtApplicationCase> isNonSjpCourtApplication = courtApplicationCase -> !courtApplicationCase.getIsSJP();
            final boolean isSjp = initiateCourtProceedingsForApplication.getCourtApplication().getCourtApplicationCases().stream().anyMatch(isSjpCourtApplication);
            final boolean isNonSjp = initiateCourtProceedingsForApplication.getCourtApplication().getCourtApplicationCases().stream().anyMatch(isNonSjpCourtApplication);

            if (isSjp && isNonSjp) {
                return false;
            }
        }
        return true;
    }

    private boolean isApplicationCreatedForSJPCase(final List<CourtApplicationCase> courtApplicationCases) {
        if (isNotEmpty(courtApplicationCases)) {
            final Predicate<CourtApplicationCase> isSjpCourtApplication = CourtApplicationCase::getIsSJP;
            return courtApplicationCases.stream().allMatch(isSjpCourtApplication);
        }
        return false;
    }

    private boolean checkForThirdPartyProsecutors(final CourtApplication courtApplication) {
        return courtApplication.getType().getProsecutorThirdPartyFlag() && hasApplicationCasesOrCourtOrders(courtApplication);
    }

    private boolean hasApplicationCasesOrCourtOrders(final CourtApplication courtApplication) {
        return isNotEmpty(courtApplication.getCourtApplicationCases()) || nonNull(courtApplication.getCourtOrder());
    }

    private List<CourtApplicationParty> buildThirdPartiesFromProsecutors(final CourtApplication courtApplication) {
        final Set<UUID> existingProsecutorIds = new HashSet<>();
        addToExistingProsecutorIds(existingProsecutorIds, Collections.singletonList(courtApplication.getApplicant()));

        if (isNotEmpty(courtApplication.getRespondents())) {
            addToExistingProsecutorIds(existingProsecutorIds, courtApplication.getRespondents());
        }

        if (isNotEmpty(courtApplication.getThirdParties())) {
            addToExistingProsecutorIds(existingProsecutorIds, courtApplication.getThirdParties());
        }

        final List<CourtApplicationParty> thirdParties = new ArrayList<>();

        if (isNotEmpty(courtApplication.getCourtApplicationCases())) {
            courtApplication.getCourtApplicationCases().stream()
                    .map(CourtApplicationCase::getProsecutionCaseIdentifier)
                    .filter(prosecutionCaseIdentifier -> !existingProsecutorIds.contains(prosecutionCaseIdentifier.getProsecutionAuthorityId()))
                    .forEach(prosecutionCaseIdentifier -> addProsecutorToThirdParties(prosecutionCaseIdentifier, thirdParties));
        }

        if (nonNull(courtApplication.getCourtOrder()) && isNotEmpty(courtApplication.getCourtOrder().getCourtOrderOffences())) {
            courtApplication.getCourtOrder().getCourtOrderOffences().stream()
                    .map(CourtOrderOffence::getProsecutionCaseIdentifier)
                    .filter(prosecutionCaseIdentifier -> !existingProsecutorIds.contains(prosecutionCaseIdentifier.getProsecutionAuthorityId()))
                    .forEach(prosecutionCaseIdentifier -> addProsecutorToThirdParties(prosecutionCaseIdentifier, thirdParties));
        }

        return thirdParties.stream().collect(collectingAndThen(toList(), getListOrNull()));
    }

    private void addProsecutorToThirdParties(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final List<CourtApplicationParty> thirdParties) {
        final CourtApplicationParty courtApplicationParty = buildCourtApplicationParty(prosecutionCaseIdentifier);
        thirdParties.add(courtApplicationParty);
    }

    private void addToExistingProsecutorIds(final Set<UUID> existingProsecutorIds, final List<CourtApplicationParty> courtApplicationParties) {
        courtApplicationParties.stream()
                .filter(courtApplicationParty -> nonNull(courtApplicationParty.getProsecutingAuthority()))
                .map(courtApplicationParty -> courtApplicationParty.getProsecutingAuthority().getProsecutionAuthorityId())
                .forEach(existingProsecutorIds::add);
    }

    private CourtApplicationParty buildCourtApplicationParty(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(UUID.randomUUID())
                .withSummonsRequired(true)
                .withNotificationRequired(true)
                .withProsecutingAuthority(buildProsecutingAuthority(prosecutionCaseIdentifier))
                .build();
    }

    private ProsecutingAuthority buildProsecutingAuthority(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return prosecutingAuthority()
                .withProsecutionAuthorityId(prosecutionCaseIdentifier.getProsecutionAuthorityId())
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .withName(prosecutionCaseIdentifier.getProsecutionAuthorityName())
                .withAddress(prosecutionCaseIdentifier.getAddress())
                .withContact(prosecutionCaseIdentifier.getContact())
                .withProsecutorCategory(prosecutionCaseIdentifier.getProsecutorCategory())
                .withMajorCreditorCode(prosecutionCaseIdentifier.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                .build();
    }
}
