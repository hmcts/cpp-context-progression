package uk.gov.moj.cpp.progression.processor;


import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds;
import static uk.gov.justice.core.courts.CreateHearingApplicationRequest.createHearingApplicationRequest;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.HearingDay.hearingDay;
import static uk.gov.justice.core.courts.HearingListingStatus.SENT_FOR_LISTING;
import static uk.gov.justice.core.courts.LinkType.LINKED;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsRejected.publicProgressionCourtApplicationSummonsRejected;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.ApplicationReferredToBoxwork;
import uk.gov.justice.core.courts.ApplicationReferredToCourtHearing;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.BreachedApplications;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationSummonsApproved;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.CreateHearingApplicationRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiateCourtHearingAfterSummonsApproved;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsApproved;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.RemoveDefendantCustodialEstablishmentRequested;
import uk.gov.justice.core.courts.SendNotificationForApplication;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.UpdateCpsDefendantId;
import uk.gov.moj.cpp.progression.processor.exceptions.CaseNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsRejectedService;
import uk.gov.moj.cpp.progression.service.ListHearingBoxworkService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SjpService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:S2789", "squid:CallToDeprecatedMethod", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod", "squid:S1192"})
public class CourtApplicationProcessor {

    public static final String PUBLIC_PROGRESSION_APPLICATION_DEFENDANT_CHANGED = "public.progression.application-defendant-changed";
    private static final String COURT_APPLICATION = "courtApplication";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String OLD_APPLICATION_ID = "oldApplicationId";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_CREATED = "public.progression.court-application-created";

    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_PROCEEDINGS_INITIATED = "public.progression.court-application-proceedings-initiated";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_UPDATED = "public.progression.court-application-updated";
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION = "progression.command.create-court-application";
    private static final String PROGRESSION_COMMAND_UPDATE_COURT_APPLICATION_TO_HEARING = "progression.command.update-court-application-to-hearing";
    private static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_ADDRESS_ON_CASE = "progression.command.update-defendant-address-on-case";
    private static final String PUBLIC_PROGRESSION_EVENTS_SJP_PROSECUTION_CASE_CREATED = "public.progression.sjp-prosecution-case-created";
    private static final String LIST_OR_REFER_COURT_APPLICATION = "progression.command.list-or-refer-court-application";
    private static final String REMOVE_DEFENDANT_CUSTODIAL_ESTABLISHMENT_FROM_CASE = "progression.command.remove-defendant-custodial-establishment-from-case";
    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";
    private static final UUID APPLICATION_HEARING_TYPE_ID = fromString("3449743b-95d6-4836-8941-57f588b52068");
    private static final String APPLICATION = "Application";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED = "public.progression.court-application-summons-approved";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED = "public.progression.court-application-summons-rejected";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_APPLICATION_UPDATED = "public.progression.hearing-resulted-application-updated";

    private static final String PUBLIC_PROGRESSION_EVENTS_WELSH_TRANSLATION_REQUIRED = "public.progression.welsh-translation-required";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationProcessor.class.getCanonicalName());
    public static final String HEARING_ID = "hearingId";
    public static final String PUBLIC_PROGRESSION_EVENTS_BREACH_APPLICATIONS_TO_BE_ADDED_TO_HEARING = "public.progression.breach-applications-to-be-added-to-hearing";
    public static final String INACTIVE = "INACTIVE";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_APPLICATION_DEFENDANT = "progression.command.update.hearing.application.defendant";
    public static final String PUBLIC_PROGRESSION_DEFENDANT_ADDRESS_CHANGED = "public.progression.defendant-address-changed";


    @Inject
    private ListingService listingService;

    @Inject
    private SjpService sjpService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private SummonsHearingRequestService summonsHearingRequestService;

    @Inject
    private SummonsRejectedService summonsRejectedService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private ListHearingBoxworkService listHearingBoxworkService;

    public static final String MATERIAL_ID = "materialId";

    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";

    private static final String DEFENDANT_NAME = "defendantName";

    private static final String CASE_URN = "caseURN";

    @Handles("progression.event.court-application-created")
    public void processCourtApplicationCreated(final JsonEnvelope event) {

        LOGGER.info("Converting courtApplication Created payload");
        final CourtApplicationCreated courtApplicationCreated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class);
        LOGGER.info("Converted courtApplication Created payload  {}", courtApplicationCreated);
        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();
        LOGGER.info("CourtApplication  {}", courtApplication);

        final JsonObject publicEvent = createObjectBuilder()
                .add(COURT_APPLICATION, objectToJsonObjectConverter.convert(courtApplication))
                .build();

        LOGGER.info("Raising public event for CourtApplication {}", courtApplication);
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_COURT_APPLICATION_CREATED).build(), publicEvent));

        sendUpdateCpsDefendantIdCommand(event, courtApplicationCreated);

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_DEFENDANT_ADDRESS_CHANGED).build(), publicEvent));

        final JsonObject command = createObjectBuilder()
                .add("id", courtApplication.getId().toString())
                .build();
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(LIST_OR_REFER_COURT_APPLICATION).build(), command));

    }


    @Handles("progression.event.remove-defendant-custodial-establishment-requested")
    public void processRemoveDefendantCustodialEstablishmentRequested(final JsonEnvelope event) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Converting remove-defendant-custodial-establishment-requested payload");
        }
        final RemoveDefendantCustodialEstablishmentRequested removeDefendantCustodialEstablishmentRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), RemoveDefendantCustodialEstablishmentRequested.class);
        final CourtApplication courtApplication = removeDefendantCustodialEstablishmentRequested.getCourtApplication();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("CourtApplication  {}", courtApplication.getId());
        }

        if(isNotEmpty(courtApplication.getCourtApplicationCases())){
            courtApplication.getCourtApplicationCases()
                    .stream()
                    .filter(courtApplicationCase -> INACTIVE.equals(courtApplicationCase.getCaseStatus()))
                    .forEach(prosecutionCase -> {
                        final JsonObject caseCommand = createObjectBuilder()
                                .add("prosecutionCaseId", prosecutionCase.getProsecutionCaseId().toString())
                                .add("defendantId", courtApplication.getSubject().getMasterDefendant().getDefendantCase().get(0).getDefendantId().toString())
                                .add("masterDefendantId", courtApplication.getSubject().getMasterDefendant().getMasterDefendantId().toString())
                                .build();
                        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(REMOVE_DEFENDANT_CUSTODIAL_ESTABLISHMENT_FROM_CASE).build(), caseCommand));
                    });
        }
    }

    private void sendUpdateCpsDefendantIdCommand(final JsonEnvelope event, final CourtApplicationCreated courtApplicationCreated) {
        ofNullable(courtApplicationCreated.getCourtApplication().getRespondents()).orElse(emptyList()).stream()
                .filter(courtApplicationParty -> nonNull(courtApplicationParty.getMasterDefendant()))
                .flatMap(courtApplicationParty -> Stream.of(courtApplicationParty.getMasterDefendant()))
                .filter(masterDefendant -> nonNull(masterDefendant.getCpsDefendantId()))
                .filter(masterDefendant -> nonNull(masterDefendant.getDefendantCase()))
                .forEach(masterDefendant -> masterDefendant.getDefendantCase().stream().findFirst().ifPresent(defendantCase -> {
                    final UpdateCpsDefendantId updateCpsDefendantId = UpdateCpsDefendantId.updateCpsDefendantId()
                            .withCpsDefendantId(masterDefendant.getCpsDefendantId().toString())
                            .withCaseId(defendantCase.getCaseId())
                            .withDefendantId(masterDefendant.getMasterDefendantId())
                            .build();

                    sender.send(envelopeFrom(metadataFrom(event.metadata()).withName("progression.command.update-cps-defendant-id").build(), updateCpsDefendantId));
                }));
    }

    @Handles("progression.event.listed-court-application-changed")
    public void processCourtApplicationChanged(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.listed-court-application-changed", event.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_COURT_APPLICATION_CHANGED).build(),
                event.payloadAsJsonObject()));
    }

    @Handles("progression.event.court-application-updated")
    public void processCourtApplicationUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.court-application-updated", event.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_COURT_APPLICATION_UPDATED).build(),
                event.payloadAsJsonObject()));
    }

    @Handles("progression.event.court-application-proceedings-initiated")
    public void processCourtApplicationInitiated(final JsonEnvelope event) {
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class);

        if (Boolean.TRUE.equals(courtApplicationProceedingsInitiated.getIsSJP())) {
            initiateSJPCase(event, courtApplicationProceedingsInitiated);
        } else {
            initiateCourtApplication(event, courtApplicationProceedingsInitiated.getCourtApplication(), courtApplicationProceedingsInitiated.getOldApplicationId());
        }
        final JsonObjectBuilder courtApplicationWithCase = createObjectBuilder();
        courtApplicationWithCase.add("courtApplication", objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated.getCourtApplication()));
        final BoxHearingRequest boxHearing = courtApplicationProceedingsInitiated.getBoxHearing();
        if (nonNull(boxHearing)) {
            courtApplicationWithCase.add("boxHearing", objectToJsonObjectConverter.convert(boxHearing));
        }
        final Boolean summonsRequired = courtApplicationProceedingsInitiated.getSummonsApprovalRequired();
        if (nonNull(summonsRequired)) {
            courtApplicationWithCase.add("summonsApprovalRequired", summonsRequired);
        }

        final CourtHearingRequest courtHearing = courtApplicationProceedingsInitiated.getCourtHearing();
        if (nonNull(courtHearing)) {
            courtApplicationWithCase.add("courtHearing", objectToJsonObjectConverter.convert(courtHearing));
        }

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        if (nonNull(courtApplicationProceedingsInitiated.getCourtApplication()) && nonNull(courtApplicationProceedingsInitiated.getCourtApplication().getCourtApplicationCases())) {
            courtApplicationProceedingsInitiated.getCourtApplication().getCourtApplicationCases().forEach(courtApplicationCase ->
                    progressionService.getProsecutionCase(event, courtApplicationCase.getProsecutionCaseId().toString()).ifPresent(pc -> {
                        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(pc.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                        updateDefendantAddressOnCase(event, courtApplicationProceedingsInitiated.getCourtApplication(), courtApplicationCase.getProsecutionCaseId().toString(), prosecutionCase.getDefendants());
                        final ProsecutionCase enrichedCase = enrichProsecutionCaseWithAddressFromApplication(prosecutionCase, courtApplicationProceedingsInitiated.getCourtApplication());
                        prosecutionCases.add(enrichedCase);
                    }));
        } else if (nonNull(courtApplicationProceedingsInitiated.getCourtApplication()) && nonNull(courtApplicationProceedingsInitiated.getCourtApplication().getCourtOrder())) {
            courtApplicationProceedingsInitiated.getCourtApplication().getCourtOrder().getCourtOrderOffences().stream().map(CourtOrderOffence::getProsecutionCaseId).collect(Collectors.toSet())
            .forEach(prosecutionCaseId ->
                progressionService.getProsecutionCase(event, prosecutionCaseId.toString()).ifPresent(pc -> {
                    final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(pc.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    updateDefendantAddressOnCase(event, courtApplicationProceedingsInitiated.getCourtApplication(), prosecutionCaseId.toString(), prosecutionCase.getDefendants());
                    final ProsecutionCase enrichedCase = enrichProsecutionCaseWithAddressFromApplication(prosecutionCase, courtApplicationProceedingsInitiated.getCourtApplication());
                    prosecutionCases.add(enrichedCase);
                }));
        }
        final JsonArrayBuilder prosecutionsWithCaseArray = createArrayBuilder();
        prosecutionCases.forEach(prosecutionCase -> prosecutionsWithCaseArray.add(objectToJsonObjectConverter.convert(prosecutionCase)));
        courtApplicationWithCase.add("prosecutionCases", prosecutionsWithCaseArray);
        final JsonObject courtApplicationWithCaseJsonObject = courtApplicationWithCase.build();
        LOGGER.info("Raising event {} with jsonObject {} ", PUBLIC_PROGRESSION_COURT_APPLICATION_PROCEEDINGS_INITIATED, courtApplicationWithCaseJsonObject);
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_COURT_APPLICATION_PROCEEDINGS_INITIATED), courtApplicationWithCaseJsonObject));
    }

    @SuppressWarnings({"squid:S3776"})
    private ProsecutionCase enrichProsecutionCaseWithAddressFromApplication(final ProsecutionCase prosecutionCase, final CourtApplication courtApplication) {
        final List<Defendant> enrichedDefendants = new ArrayList<>();
        ofNullable(prosecutionCase.getDefendants()).ifPresent(defendants -> defendants.forEach(defendant -> {
            boolean isCaseDefendantEnriched = false;
            final Defendant.Builder defendantBuilder = Defendant.defendant();
            final boolean isOrgDefendant = nonNull(defendant.getLegalEntityDefendant());
            final UUID defendantId = ofNullable(defendant.getMasterDefendantId()).orElse(defendant.getId());
            final Optional<MasterDefendant> defendantIsApplicant = ofNullable(courtApplication.getApplicant()).map(CourtApplicationParty::getMasterDefendant).filter(def -> nonNull(def.getMasterDefendantId()) && def.getMasterDefendantId().equals(defendantId));
            if (defendantIsApplicant.isPresent()) {
                isCaseDefendantEnriched = enrichCaseDefendantWithApplicationAddress(defendant, defendantBuilder, isOrgDefendant, defendantIsApplicant.get(), enrichedDefendants);
            } else {
                if (nonNull(courtApplication.getRespondents())) {
                    final Optional<MasterDefendant> defendantIsRespondent = courtApplication.getRespondents().stream().map(CourtApplicationParty::getMasterDefendant).filter(Objects::nonNull).filter(def -> nonNull(def.getMasterDefendantId()) && def.getMasterDefendantId().equals(defendantId)).findFirst();
                    if (defendantIsRespondent.isPresent()) {
                        isCaseDefendantEnriched = enrichCaseDefendantWithApplicationAddress(defendant, defendantBuilder, isOrgDefendant, defendantIsRespondent.get(), enrichedDefendants);
                    }
                }
            }
            if (!isCaseDefendantEnriched) {
                enrichedDefendants.add(defendant);
            }
        }));
        return ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase).withDefendants(enrichedDefendants).build();
    }

    private static boolean enrichCaseDefendantWithApplicationAddress(final Defendant defendant, final Defendant.Builder defendantBuilder,
                                                                     final boolean isOrgDefendant, final MasterDefendant defendantOnApplication, final List<Defendant> enrichedDefendants) {
        if (isOrgDefendant) {
            defendantBuilder.withValuesFrom(defendant)
                    .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                            .withValuesFrom(defendant.getLegalEntityDefendant())
                            .withOrganisation(Organisation.organisation()
                                    .withValuesFrom(defendant.getLegalEntityDefendant().getOrganisation())
                                    .withAddress(defendantOnApplication.getLegalEntityDefendant().getOrganisation().getAddress())
                                    .build())
                            .build())
                    .build();
        } else {
            defendantBuilder.withValuesFrom(defendant)
                    .withPersonDefendant(PersonDefendant.personDefendant()
                            .withValuesFrom(defendant.getPersonDefendant())
                            .withPersonDetails(Person.person()
                                    .withValuesFrom(defendant.getPersonDefendant().getPersonDetails())
                                    .withAddress(defendantOnApplication.getPersonDefendant().getPersonDetails().getAddress())
                                    .build())
                            .build())
                    .build();
        }
        enrichedDefendants.add(defendantBuilder.build());
        return true;
    }

    private void updateDefendantAddressOnCase(final JsonEnvelope event, final CourtApplication courtApplication, final String prosecutionCaseId, final List<Defendant> defendants) {
        if (nonNull(courtApplication.getApplicant()) && nonNull(courtApplication.getApplicant().getMasterDefendant())) {
            DefendantUpdate defendantUpdate = getDefendantUpdate(prosecutionCaseId, courtApplication.getApplicant());
            defendantUpdate = enrichDefendantUpdateWithDefendantId(defendants, defendantUpdate);
            buildAndSendCommandForDefendantAddressUpdate(event, defendantUpdate, prosecutionCaseId);
        }

        if (nonNull(courtApplication.getRespondents())) {
            courtApplication.getRespondents().stream().filter(r -> nonNull(r.getMasterDefendant())).forEach(respondent -> {
                DefendantUpdate defendantUpdate = getDefendantUpdate(prosecutionCaseId, respondent);
                defendantUpdate = enrichDefendantUpdateWithDefendantId(defendants, defendantUpdate);
                buildAndSendCommandForDefendantAddressUpdate(event, defendantUpdate, prosecutionCaseId);
            });
        }
    }

    private static DefendantUpdate enrichDefendantUpdateWithDefendantId(final List<Defendant> defendants, DefendantUpdate defendantUpdate) {
        final Optional<UUID> defendantId = getMatchingDefendantIdFromDefendantList(defendants, defendantUpdate);
        if(defendantId.isPresent()){
            defendantUpdate = DefendantUpdate.defendantUpdate().withValuesFrom(defendantUpdate).withId(defendantId.get()).build();
        }
        return defendantUpdate;
    }

    private static Optional<UUID> getMatchingDefendantIdFromDefendantList(final List<Defendant> defendants, final DefendantUpdate defendantUpdate) {
        return Optional.ofNullable(defendants)
                .map(list -> list.stream()
                        .filter(defendant -> defendant.getMasterDefendantId() != null &&
                                defendant.getMasterDefendantId().equals(defendantUpdate.getMasterDefendantId()))
                        .map(Defendant::getId)
                        .findFirst())
                .orElse(Optional.empty());
    }

    private DefendantUpdate getDefendantUpdate(final String prosecutionCaseId, CourtApplicationParty courtApplicationParty) {
        return DefendantUpdate.defendantUpdate()
                .withMasterDefendantId(courtApplicationParty.getMasterDefendant().getMasterDefendantId())
                .withProsecutionCaseId(fromString(prosecutionCaseId))
                .withLegalEntityDefendant(courtApplicationParty.getMasterDefendant().getLegalEntityDefendant())
                .withPersonDefendant(getPersonDefendant(courtApplicationParty))
                .build();
    }

    private PersonDefendant getPersonDefendant(CourtApplicationParty courtApplicationParty) {
        final PersonDefendant personDefendant = courtApplicationParty.getMasterDefendant().getPersonDefendant();
        if (Optional.ofNullable(personDefendant).isPresent()) {
            return PersonDefendant.personDefendant()
                    .withPersonDetails(personDefendant.getPersonDetails())
                    .build();
        }
        return null;
    }

    private void buildAndSendCommandForDefendantAddressUpdate(JsonEnvelope event,
                                                              DefendantUpdate defendantUpdate,
                                                              String prosecutionCaseId) {
        final JsonObjectBuilder updateDefendantForProsecutionCase = createObjectBuilder();
        updateDefendantForProsecutionCase.add("defendant", objectToJsonObjectConverter.convert(defendantUpdate));
        updateDefendantForProsecutionCase.add("prosecutionCaseId", prosecutionCaseId);
        final JsonObject updateDefendantForProsecutionCaseJsonObject = updateDefendantForProsecutionCase.build();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Raising event {} with prosecutionCaseId {}", PROGRESSION_COMMAND_UPDATE_DEFENDANT_ADDRESS_ON_CASE, prosecutionCaseId);
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_UPDATE_DEFENDANT_ADDRESS_ON_CASE), updateDefendantForProsecutionCaseJsonObject));
    }


    @Handles("progression.event.court-application-proceedings-edited")
    public void processCourtApplicationEdited(final JsonEnvelope event) {
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class);
        if (nonNull(courtApplicationProceedingsEdited.getCourtHearing())) {
            final JsonObjectBuilder updateHearingPayload = createObjectBuilder();
            updateHearingPayload.add("courtApplication", objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited.getCourtApplication()));
            updateHearingPayload.add("hearingId", courtApplicationProceedingsEdited.getCourtHearing().getId().toString());
            sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_UPDATE_COURT_APPLICATION_TO_HEARING), updateHearingPayload.build()));
        }

        final BoxHearingRequest boxHearingRequest = courtApplicationProceedingsEdited.getBoxHearing();
        final JsonObjectBuilder publicPayload = createObjectBuilder();
        if (boxHearingRequest != null) {
            final ZonedDateTime sittingDay = nonNull(boxHearingRequest.getVirtualAppointmentTime()) ?
                    boxHearingRequest.getVirtualAppointmentTime() :
                    boxHearingRequest.getApplicationDueDate().atStartOfDay(ZoneOffset.UTC);
            final JsonObject hearingDay = createObjectBuilder().add("sittingDay", sittingDay.format(ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")))
                    .add("listedDurationMinutes", 10).build();
            final JsonArrayBuilder hearingDaysArrayBuilder = createArrayBuilder().add(hearingDay);
            publicPayload.add(HEARING_ID, boxHearingRequest.getId().toString());
            publicPayload.add("hearingDays", hearingDaysArrayBuilder.build());
            publicPayload.add("courtCentre", objectToJsonObjectConverter.convert(boxHearingRequest.getCourtCentre()));
            publicPayload.add("jurisdictionType", boxHearingRequest.getJurisdictionType().toString());
            publicPayload.add(COURT_APPLICATION, objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited.getCourtApplication()));
            publicPayload.add("isBoxWorkRequest", true);
        } else if (courtApplicationProceedingsEdited.getCourtHearing() != null) {
            publicPayload.add(HEARING_ID, courtApplicationProceedingsEdited.getCourtHearing().getId().toString());
            publicPayload.add("courtApplication", objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited.getCourtApplication()));
        }

        if (nonNull(courtApplicationProceedingsEdited.getCourtApplication()) && nonNull(courtApplicationProceedingsEdited.getCourtApplication().getCourtApplicationCases())) {
            courtApplicationProceedingsEdited.getCourtApplication().getCourtApplicationCases().forEach(courtApplicationCase ->
                    progressionService.getProsecutionCase(event, courtApplicationCase.getProsecutionCaseId().toString()).ifPresent(pc -> {
                        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(pc.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                        updateDefendantAddressOnCase(event, courtApplicationProceedingsEdited.getCourtApplication(), courtApplicationCase.getProsecutionCaseId().toString(), prosecutionCase.getDefendants());
                    }));
        } else if (nonNull(courtApplicationProceedingsEdited.getCourtApplication()) && nonNull(courtApplicationProceedingsEdited.getCourtApplication().getCourtOrder())) {
            courtApplicationProceedingsEdited.getCourtApplication().getCourtOrder().getCourtOrderOffences().stream().map(CourtOrderOffence::getProsecutionCaseId).collect(Collectors.toSet())
                    .forEach(prosecutionCaseId ->
                            progressionService.getProsecutionCase(event, prosecutionCaseId.toString()).ifPresent(pc -> {
                                    final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(pc.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                                    updateDefendantAddressOnCase(event, courtApplicationProceedingsEdited.getCourtApplication(), prosecutionCaseId.toString(), prosecutionCase.getDefendants());
                            }));
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED), publicPayload.build()));
    }


    @Handles("progression.event.application-referred-to-boxwork")
    public void processBoxWorkApplication(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final ApplicationReferredToBoxwork applicationReferredToBoxwork = jsonObjectToObjectConverter.convert(payload, ApplicationReferredToBoxwork.class);

        final BoxHearingRequest boxHearingRequest = applicationReferredToBoxwork.getBoxHearing();

        final ZonedDateTime sittingDay = nonNull(boxHearingRequest.getVirtualAppointmentTime()) ?
                boxHearingRequest.getVirtualAppointmentTime() :
                boxHearingRequest.getApplicationDueDate().atStartOfDay(ZoneOffset.UTC);

        final Hearing.Builder hearingBuilder = hearing()
                .withId(boxHearingRequest.getId())
                .withProsecutionCases(getProsecutionCases(jsonEnvelope, applicationReferredToBoxwork.getApplication()))
                .withHearingDays(singletonList(hearingDay()
                        .withListedDurationMinutes(10)
                        .withSittingDay(sittingDay).build()))
                .withCourtCentre(boxHearingRequest.getCourtCentre())
                .withJurisdictionType(boxHearingRequest.getJurisdictionType())
                .withIsBoxHearing(true)
                .withType(HearingType.hearingType().withDescription(APPLICATION).withId(APPLICATION_HEARING_TYPE_ID).build())
                .withCourtApplications(singletonList(applicationReferredToBoxwork.getApplication()));

        if (nonNull(boxHearingRequest.getVirtualAppointmentTime())) {
            hearingBuilder.withIsVirtualBoxHearing(true);
        }

        final Initiate hearingInitiate = Initiate.initiate().withHearing(hearingBuilder.build()).build();

        progressionService.linkApplicationToHearing(jsonEnvelope, hearingInitiate.getHearing(), HearingListingStatus.HEARING_INITIALISED);

        final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(HEARING_INITIATE_COMMAND).build(), hearingInitiateCommand));

        progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);

        final List<CourtApplication> courtApplications = ofNullable(hearingInitiate.getHearing().getCourtApplications()).orElse(new ArrayList<>());
        courtApplications.forEach(courtApplication -> progressionService.updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.IN_PROGRESS));

        LOGGER.info(" Box work Referred with payload {}", hearingInitiateCommand);
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED), hearingInitiateCommand));
    }

    private void triggerRetryOnCaseNotFound(final String prosecutionCase) {
        throw new CaseNotFoundException("Prosecution case not found, so retrying -->>" + prosecutionCase);
    }

    @Handles("progression.event.application-referred-to-court-hearing")
    public void processCourtApplicationReferredToCourtHearing(final JsonEnvelope event) {
        final ApplicationReferredToCourtHearing applicationReferredToCourtHearing = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationReferredToCourtHearing.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing event for court application referred to new court hearing with application id: {}", applicationReferredToCourtHearing.getApplication().getId());
        }

        final CourtApplication application = applicationReferredToCourtHearing.getApplication();

        final CourtHearingRequest courtHearing = applicationReferredToCourtHearing.getCourtHearing();

        final ZonedDateTime earliestStartDateTime = isNull(courtHearing.getEarliestStartDateTime()) ? ZonedDateTime.now() : courtHearing.getEarliestStartDateTime();

        final List<ProsecutionCase> prosecutionCases = getProsecutionCases(event, application);

        final Hearing hearing = hearing()
                .withProsecutionCases(prosecutionCases)
                .withId(courtHearing.getId())
                .withHearingDays(singletonList(hearingDay()
                        .withListedDurationMinutes(10)
                        .withSittingDay(earliestStartDateTime).build()))
                .withCourtCentre(courtHearing.getCourtCentre())
                .withJurisdictionType(courtHearing.getJurisdictionType())
                .withIsBoxHearing(false)
                .withType(courtHearing.getHearingType())
                .withCourtApplications(singletonList(application))
                .withBookingType(courtHearing.getBookingType())
                .withPriority(courtHearing.getPriority())
                .withSpecialRequirements(courtHearing.getSpecialRequirements())
                .build();

        final Initiate hearingInitiate = Initiate.initiate().withHearing(hearing).build();

        progressionService.linkApplicationToHearing(event, hearingInitiate.getHearing(), HearingListingStatus.HEARING_INITIALISED);

        // then update application hearing listing status to hearing initiated
        progressionService.updateHearingListingStatusToHearingInitiated(event, hearingInitiate);

        // then update application status and list hearing
        progressionService.updateCourtApplicationStatus(event, application.getId(),
                nonNull(application.getApplicationStatus()) ? application.getApplicationStatus() : ApplicationStatus.UN_ALLOCATED);

        final ListCourtHearing listCourtHearing = buildDefaultHearingNeeds(applicationReferredToCourtHearing.getCourtHearing(), application, prosecutionCases);
        listingService.listCourtHearing(event, listCourtHearing);
    }

    @Handles("progression.event.court-application-summons-approved")
    public void courtApplicationSummonsApproved(final JsonEnvelope event) {

        final CourtApplicationSummonsApproved courtApplicationSummonsApproved = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationSummonsApproved.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing event for court-application-summons-approved with application id: {} - Link Type: {}", courtApplicationSummonsApproved.getApplicationId(), courtApplicationSummonsApproved.getLinkType());
        }

        if (courtApplicationSummonsApproved.getLinkType() == LinkType.FIRST_HEARING) {
            final PublicProgressionCourtApplicationSummonsApproved summonsApprovedPublicEventPayload = PublicProgressionCourtApplicationSummonsApproved.publicProgressionCourtApplicationSummonsApproved()
                    .withSummonsApprovedOutcome(courtApplicationSummonsApproved.getSummonsApprovedOutcome())
                    .withId(courtApplicationSummonsApproved.getApplicationId())
                    .withProsecutionCaseId(courtApplicationSummonsApproved.getCaseIds().get(0))
                    .build();

            sender.send(envelop(summonsApprovedPublicEventPayload).withName(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED).withMetadataFrom(event));
        }

    }

    @Handles("progression.event.court-application-summons-rejected")
    public void courtApplicationSummonsRejected(final JsonEnvelope jsonEnvelope) {

        final CourtApplicationSummonsRejected courtApplicationSummonsRejected = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CourtApplicationSummonsRejected.class);
        final CourtApplication courtApplication = courtApplicationSummonsRejected.getCourtApplication();
        final LinkType linkType = courtApplication.getType().getLinkType();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing event for court-application-summons-rejected with application id: {} - Link Type: {}", courtApplication.getId(), linkType);
        }

        if (linkType == LinkType.FIRST_HEARING) {
            final PublicProgressionCourtApplicationSummonsRejected summonsRejectedPublicEventPayload = publicProgressionCourtApplicationSummonsRejected()
                    .withId(courtApplication.getId())
                    .withProsecutionCaseId(courtApplicationSummonsRejected.getCaseIds().get(0))
                    .withSummonsRejectedOutcome(courtApplicationSummonsRejected.getSummonsRejectedOutcome())
                    .build();


            sender.send(envelop(summonsRejectedPublicEventPayload).withName(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED).withMetadataFrom(jsonEnvelope));
        }

        // send summons rejection notification to prosecutor
        summonsRejectedService.sendSummonsRejectionNotification(jsonEnvelope, courtApplication, courtApplicationSummonsRejected.getSummonsRejectedOutcome());
    }

    @Handles("progression.event.initiate-court-hearing-after-summons-approved")
    public void initiateCourtHearingAfterSummonsApproved(final JsonEnvelope event) {
        final InitiateCourtHearingAfterSummonsApproved initiateCourtHearingAfterSummonsApproved = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), InitiateCourtHearingAfterSummonsApproved.class);

        LOGGER.info("Processing event for court application referred to new court hearing with application id: {}", initiateCourtHearingAfterSummonsApproved.getApplication().getId());
        final CourtApplication application = initiateCourtHearingAfterSummonsApproved.getApplication();

        final CourtHearingRequest courtHearing = initiateCourtHearingAfterSummonsApproved.getCourtHearing();

        final ZonedDateTime earliestStartDateTime = isNull(courtHearing.getEarliestStartDateTime()) ? ZonedDateTime.now() : courtHearing.getEarliestStartDateTime();

        final List<ProsecutionCase> prosecutionCases = getProsecutionCases(event, application);

        final UUID courtHearingId = courtHearing.getId();
        final Hearing hearing = hearing()
                .withId(courtHearingId)
                .withProsecutionCases(prosecutionCases)
                .withHearingDays(singletonList(hearingDay()
                        .withListedDurationMinutes(10)
                        .withSittingDay(earliestStartDateTime).build()))
                .withCourtCentre(courtHearing.getCourtCentre())
                .withJurisdictionType(courtHearing.getJurisdictionType())
                .withIsBoxHearing(false)
                .withType(courtHearing.getHearingType())
                .withCourtApplications(singletonList(application))
                .withBookingType(courtHearing.getBookingType())
                .withPriority(courtHearing.getPriority())
                .withSpecialRequirements(courtHearing.getSpecialRequirements())
                .build();

        final Initiate hearingInitiate = Initiate.initiate().withHearing(hearing).build();

        progressionService.linkApplicationToHearing(event, hearingInitiate.getHearing(), HearingListingStatus.HEARING_INITIALISED);

        // then update application status and list hearing
        progressionService.updateCourtApplicationStatus(event, application.getId(),
                nonNull(application.getApplicationStatus()) ? application.getApplicationStatus() : ApplicationStatus.UN_ALLOCATED);

        final ListCourtHearing listCourtHearing = buildDefaultHearingNeeds(courtHearing, application, prosecutionCases);
        listingService.listCourtHearing(event, listCourtHearing);

        final SummonsTemplateType summonsTemplateType = application.getType().getSummonsTemplateType();
        if (summonsTemplateType == SummonsTemplateType.BREACH || summonsTemplateType == SummonsTemplateType.GENERIC_APPLICATION) {
            final CreateHearingApplicationRequest createHearingApplicationRequest = buildApplicationHearingRequest(initiateCourtHearingAfterSummonsApproved, application, courtHearingId, summonsTemplateType);
            summonsHearingRequestService.addApplicationRequestToHearing(event, createHearingApplicationRequest);
        }
    }

    @Handles("progression.event.application-referral-to-existing-hearing")
    public void processCourtApplicationReferredToExistingHearing(final JsonEnvelope event) {
        final ApplicationReferredToExistingHearing applicationReferredToExistingHearing = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationReferredToExistingHearing.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing event for court application referred to existing court hearing with courtApplication id: {}", applicationReferredToExistingHearing.getApplication().getId());
        }
        final CourtApplication courtApplication = applicationReferredToExistingHearing.getApplication();
        final CourtHearingRequest courtHearingRequest = applicationReferredToExistingHearing.getCourtHearing();
        final UUID hearingId = courtHearingRequest.getId();

        if (courtApplication.getType().getBreachType() == BreachType.COMMISSION_OF_NEW_OFFENCE_BREACH) {

            sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).build(), createObjectBuilder()
                    .add(HEARING_ID, hearingId.toString())
                    .add(COURT_APPLICATION, objectToJsonObjectConverter.convert(courtApplication))
                    .build()));
        } else {

            final Optional<JsonObject> hearingIdFromQuery = progressionService.getHearing(event, hearingId.toString());

            if (hearingIdFromQuery.isPresent()) {
                progressionService.updateCourtApplicationStatus(event, courtApplication.getId(), ApplicationStatus.LISTED);
                final Hearing hearing = jsonObjectToObjectConverter.convert(hearingIdFromQuery.get().getJsonObject("hearing"), Hearing.class);
                final Hearing updatedHearing = updateHearingWithApplication(event, hearing, courtApplication);
                progressionService.linkApplicationsToHearing(event, updatedHearing, singletonList(courtApplication.getId()), SENT_FOR_LISTING);

                final JsonArrayBuilder prosecutionCasesBuilder = Json.createArrayBuilder();
                final Stream<ProsecutionCase> prosecutionCaseStream = ofNullable(updatedHearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty);
                prosecutionCaseStream.map(prosecutionCase -> objectToJsonObjectConverter.convert(prosecutionCase)).forEach(prosecutionCasesBuilder::add);
                final JsonObjectBuilder hearingExtendedPayloadBuilder = createObjectBuilder()
                        .add(HEARING_ID, hearingId.toString())
                        .add(COURT_APPLICATION, objectToJsonObjectConverter.convert(courtApplication));
                ofNullable(updatedHearing.getProsecutionCases()).ifPresent(cases -> hearingExtendedPayloadBuilder.add("prosecutionCases", prosecutionCasesBuilder.build()));
                sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).build(), hearingExtendedPayloadBuilder.build()));
            } else {
                LOGGER.info("Court Application not found for hearing: {}", hearingId);
            }
        }

        progressionService.populateHearingToProbationCaseworker(event, hearingId);
    }

    @Handles("progression.event.hearing-resulted-application-updated")
    public void processHearingResultedApplicationUpdated(final JsonEnvelope event) {
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResultedApplicationUpdated.class);
        final CourtApplication courtApplication = hearingResultedApplicationUpdated.getCourtApplication();

        if (listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(courtApplication.getJudicialResults())) {
            final NextHearing nextHearing = listHearingBoxworkService.getNextHearingFromLHBWResult(courtApplication.getJudicialResults());
            final ZonedDateTime hearingStartDateTime = ofNullable(nextHearing.getListedStartDateTime()).orElseGet(() -> nextHearing.getWeekCommencingDate().atStartOfDay(ZoneOffset.UTC));
            notificationService.sendNotification(event, courtApplication, false, nextHearing.getCourtCentre(), hearingStartDateTime, nextHearing.getJurisdictionType(), false);
        }

        sender.send(envelop(event.payloadAsJsonObject()).withName(PUBLIC_PROGRESSION_HEARING_RESULTED_APPLICATION_UPDATED).withMetadataFrom(event));
    }

    @Handles("progression.event.send-notification-for-application-initiated")
    public void sendNotificationForApplication(final JsonEnvelope jsonEnvelope) {
        final SendNotificationForApplication sendNotificationForApplication = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SendNotificationForApplication.class);
        final CourtApplication courtApplication = sendNotificationForApplication.getCourtApplication();
        if (sendNotificationForApplication.getIsWelshTranslationRequired()) {
            final String applicantNameFromMasterDefendant = nonNull(courtApplication.getApplicant().getMasterDefendant()) && nonNull(courtApplication.getApplicant().getMasterDefendant().getPersonDefendant()) ? courtApplication.getApplicant().getMasterDefendant().getPersonDefendant().getPersonDetails().getLastName() + " " + courtApplication.getApplicant().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName() : "";
            final String applicationName = nonNull(courtApplication.getApplicant().getPersonDetails()) ? courtApplication.getApplicant().getPersonDetails().getLastName() + " " + courtApplication.getApplicant().getPersonDetails().getFirstName() : applicantNameFromMasterDefendant;
            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add(MASTER_DEFENDANT_ID, courtApplication.getApplicant().getId().toString())
                    .add(DEFENDANT_NAME, applicationName)
                    .add(CASE_URN, courtApplication.getApplicationReference());
            final JsonObjectBuilder welshTranslationRequiredBuilder = createObjectBuilder().add("welshTranslationRequired", jsonObjectBuilder.build());
            sender.send(Enveloper.envelop(welshTranslationRequiredBuilder.build())
                    .withName(PUBLIC_PROGRESSION_EVENTS_WELSH_TRANSLATION_REQUIRED)
                    .withMetadataFrom(jsonEnvelope));
        }
        final CourtHearingRequest courtHearingRequest = sendNotificationForApplication.getCourtHearing();
        if (nonNull(courtHearingRequest) && (isNull(courtHearingRequest.getCourtCentre().getRoomId()) || nonNull(courtHearingRequest.getWeekCommencingDate()))) {
            notificationService.sendNotification(jsonEnvelope, courtApplication, sendNotificationForApplication.getIsWelshTranslationRequired(), courtHearingRequest.getCourtCentre(), courtHearingRequest.getEarliestStartDateTime(), courtHearingRequest.getJurisdictionType(), false);
        }
    }

    @Handles("progression.event.breach-application-creation-requested")
    public void processBreachApplicationCreationRequested(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.breach-application-creation-requested", event.toObfuscatedDebugString());
        }

        final BreachApplicationCreationRequested breachApplicationCreationRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), BreachApplicationCreationRequested.class);
        final UUID hearingId = breachApplicationCreationRequested.getHearingId();

        final Optional<JsonObject> hearingIdFromQuery = progressionService.getHearing(event, hearingId.toString());
        if (hearingIdFromQuery.isPresent()) {

            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingIdFromQuery.get().getJsonObject("hearing"), Hearing.class);
            final UUID masterDefendantId = breachApplicationCreationRequested.getMasterDefendantId();

            final Optional<ProsecutionCase> prosecutionCaseFromQuery = findFirstProsecutionCaseForMasterDefendant(hearing, masterDefendantId);

            if (prosecutionCaseFromQuery.isPresent()) {
                final ProsecutionCase prosecutionCase = prosecutionCaseFromQuery.get();
                final CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withSummonsRequired(false)
                        .withNotificationRequired(false)
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                                .withProsecutionAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                                .build())
                        .build();


                final Optional<Defendant> defendant = findDefendantInProsecutionCase(prosecutionCase, masterDefendantId);

                if (defendant.isPresent()) {
                    final CourtApplicationParty respondent = CourtApplicationParty.courtApplicationParty()
                            .withId(randomUUID())
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .withMasterDefendant(MasterDefendant.masterDefendant()
                                    .withMasterDefendantId(breachApplicationCreationRequested.getMasterDefendantId())
                                    .withPersonDefendant(defendant.get().getPersonDefendant())
                                    .withLegalEntityDefendant(defendant.get().getLegalEntityDefendant())
                                    .withDefendantCase(singletonList(DefendantCase.defendantCase()
                                            .withCaseId(defendant.get().getProsecutionCaseId())
                                            .withDefendantId(defendant.get().getId())
                                            .withCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                                            .build()))
                                    .build())
                            .build();

                    final BreachedApplications breachedApplication = breachApplicationCreationRequested.getBreachedApplications();
                    final List<Offence> offences = nonNull(defendant.get().getOffences()) ? defendant.get().getOffences().stream().collect(toList()) : null;

                    final CourtApplication courtApplication = CourtApplication.courtApplication()
                            .withId(breachedApplication.getId())
                            .withType(breachedApplication.getApplicationType())
                            .withCourtOrder(breachedApplication.getCourtOrder())
                            .withApplicationStatus(ApplicationStatus.DRAFT)
                            .withApplicationReceivedDate(LocalDate.now())
                            .withApplicant(applicant)
                            .withSubject(respondent)
                            .withRespondents(singletonList(respondent))
                            .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                                    .withIsSJP(Boolean.FALSE)
                                    .withProsecutionCaseId(prosecutionCase.getId())
                                    .withOffences(offences)
                                    .withCaseStatus("ACTIVE")
                                    .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                                    .build()))
                            .build();

                    final CourtHearingRequest courtHearingRequest = CourtHearingRequest.courtHearingRequest()
                            .withId(hearing.getId())
                            .build();

                    final JsonObject command = createObjectBuilder()
                            .add("courtApplication", objectToJsonObjectConverter.convert(courtApplication))
                            .add("courtHearing", objectToJsonObjectConverter.convert(courtHearingRequest))
                            .build();

                    sender.send(
                            envelop(command)
                                    .withName("progression.command.initiate-court-proceedings-for-application")
                                    .withMetadataFrom(event));
                }
            }
        }
    }


    private List<ProsecutionCase> getProsecutionCases(final JsonEnvelope event, final CourtApplication application) {
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final Stream<CourtApplicationCase> courtApplicationCases = ofNullable(application.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty);
        if (isAllActiveCases(courtApplicationCases)) {
            ofNullable(application.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty).forEach(courtApplicationCase -> {
                final Optional<JsonObject> prosecutionCaseDetailById = progressionService.getProsecutionCaseDetailById(event, courtApplicationCase.getProsecutionCaseId().toString());
                if (prosecutionCaseDetailById.isPresent()) {
                    final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseDetailById.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
                    final ProsecutionCase updatedProsecutionCase = createProsecutionCase(prosecutionCase, application);
                    if (isNotEmpty(updatedProsecutionCase.getDefendants())) {
                        prosecutionCases.add(updatedProsecutionCase);
                    }
                } else if (LINKED.equals(application.getType().getLinkType())) {
                    triggerRetryOnCaseNotFound(courtApplicationCase.getProsecutionCaseId().toString());
                }
            });
        }
        return isNotEmpty(prosecutionCases) ? prosecutionCases : null;
    }

    private ProsecutionCase createProsecutionCase(final ProsecutionCase prosecutionCase, final CourtApplication courtApplication) {

        final Set<Defendant> defendantsFromCourtApplication = new HashSet<>();

        final List<Defendant> defendants = prosecutionCase.getDefendants().stream()
                .map(defendant -> defendant().withValuesFrom(defendant).withDefendantCaseJudicialResults(null).build())
                .collect(toList());
        Optional.ofNullable(courtApplication).ifPresent(c -> {
            Optional.ofNullable(c.getApplicant()).ifPresent(a -> {
                if (Optional.ofNullable(a.getMasterDefendant()).isPresent()) {
                    defendants.stream().filter(defendant -> defendant.getMasterDefendantId().equals(a.getMasterDefendant().getMasterDefendantId()))
                            .findFirst().ifPresent(d -> defendantsFromCourtApplication.add(d));
                }
            });
            Optional.ofNullable(c.getRespondents()).ifPresent(respondents -> defendantsFromCourtApplication.addAll(defendants.stream()
                    .filter(defendant -> respondents.stream().anyMatch(respondent -> nonNull(respondent.getMasterDefendant()) && defendant.getMasterDefendantId().equals(respondent.getMasterDefendant().getMasterDefendantId())))
                    .collect(Collectors.toSet())));
        }
        );
        if(isEmpty(defendantsFromCourtApplication)) {
            defendantsFromCourtApplication.addAll(defendants);
        }
        final List<Defendant> defendantList = defendantsFromCourtApplication.stream()
                .map(this::updatedDefendant)
                .filter(defendant -> isNotEmpty(defendant.getOffences()))
                .collect(toList());
        return prosecutionCase().withValuesFrom(prosecutionCase).withDefendants(defendantList).build();
    }

    private Defendant updatedDefendant(final Defendant defendant) {
        final Predicate<Offence> offencePredicate = offence -> isNull(offence.getProceedingsConcluded()) || !offence.getProceedingsConcluded();
        final List<Offence> offences = defendant.getOffences().stream().filter(offencePredicate)
                .map(offence -> Offence.offence().withValuesFrom(offence).withJudicialResults(null).build())
                .collect(toList());
        return defendant().withValuesFrom(defendant).withOffences(offences).build();
    }


    @Handles("progression.event.breach-applications-to-be-added-to-hearing")
    public void processBreachApplicationsTobeAddedToHearing(final JsonEnvelope event) {
        sender.send(envelop(event.payloadAsJsonObject()).withName(PUBLIC_PROGRESSION_EVENTS_BREACH_APPLICATIONS_TO_BE_ADDED_TO_HEARING).withMetadataFrom(event));
    }

    @Handles("progression.event.application-defendant-update-requested")
    public void processApplicationDefendantUpdateRequested(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.event.application-defendant-update-requested received");
        }
        sender.send(envelop(event.payloadAsJsonObject()).withName(PROGRESSION_COMMAND_UPDATE_HEARING_APPLICATION_DEFENDANT).withMetadataFrom(event));
    }

    private <T> Predicate<T> distinctByKey(final Function<? super T, Object> keyExtractor) {
        final Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private boolean isAllActiveCases(final Stream<CourtApplicationCase> courtApplicationCases) {
        return courtApplicationCases
                .allMatch(courtApplicationCase -> nonNull(courtApplicationCase.getCaseStatus())
                        && !"INACTIVE".equalsIgnoreCase(courtApplicationCase.getCaseStatus())
                        && !"CLOSED".equalsIgnoreCase(courtApplicationCase.getCaseStatus()));
    }

    private Optional<ProsecutionCase> findFirstProsecutionCaseForMasterDefendant(final Hearing hearing, final UUID masterDefendantId) {

        return hearing.getProsecutionCases().stream()
                .filter(prosecutionCase -> prosecutionCase.getDefendants().stream()
                        .anyMatch(defendant -> defendant.getMasterDefendantId().equals(masterDefendantId)))
                .findFirst();
    }

    private Optional<Defendant> findDefendantInProsecutionCase(final ProsecutionCase prosecutionCase, final UUID masterDefendantId) {
        return prosecutionCase.getDefendants().stream()
                .filter(d -> d.getMasterDefendantId().equals(masterDefendantId))
                .findFirst();
    }

    private void initiateSJPCase(final JsonEnvelope event, final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated) {

        final CourtApplication courtApplication = courtApplicationProceedingsInitiated.getCourtApplication();

        for (final CourtApplicationCase courtApplicationCase : courtApplicationProceedingsInitiated.getCourtApplication().getCourtApplicationCases()) {

            final ProsecutionCase prosecutionCase = sjpService.getProsecutionCase(event, courtApplicationCase.getProsecutionCaseId());
            final ProsecutionCase enrichedCase = enrichProsecutionCaseWithAddressFromApplication(prosecutionCase, courtApplication);
            if (nonNull(enrichedCase)) {
                if (isCaseNotFoundInCC(event, enrichedCase.getProsecutionCaseIdentifier())) {
                    progressionService.createProsecutionCases(event, singletonList(enrichedCase));

                    final JsonObject publicSjpProsecutionCaseCreated = createObjectBuilder()
                            .add(PROSECUTION_CASE, objectToJsonObjectConverter.convert(enrichedCase))
                            .build();

                    sender.send(envelop(publicSjpProsecutionCaseCreated).withName(PUBLIC_PROGRESSION_EVENTS_SJP_PROSECUTION_CASE_CREATED).withMetadataFrom(event));

                } else {
                    LOGGER.warn("prosecutionCase with id {} already exists in CC", courtApplicationCase.getProsecutionCaseId());
                }
            } else {
                LOGGER.warn("prosecutionCase with id {} not found in SJP", courtApplicationCase.getProsecutionCaseId());
            }
        }

        initiateCourtApplication(event, courtApplication, courtApplicationProceedingsInitiated.getOldApplicationId());
    }

    private void initiateCourtApplication(final JsonEnvelope event, final CourtApplication courtApplication, final UUID oldApplicationId) {
        final JsonObjectBuilder commandBuilder = createObjectBuilder()
                .add(COURT_APPLICATION, objectToJsonObjectConverter.convert(courtApplication));

        if (nonNull(oldApplicationId)) {
            commandBuilder.add(OLD_APPLICATION_ID, oldApplicationId.toString());
        }

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_CREATE_COURT_APPLICATION).build(), commandBuilder.build()));
    }

    private Hearing updateHearingWithApplication(final JsonEnvelope event, final Hearing hearing, final CourtApplication courtApplication) {
        final MasterDefendant masterDefendant = courtApplication.getSubject().getMasterDefendant();

        List<CourtApplication> courtApplications = hearing.getCourtApplications();
        if (courtApplications == null) {
            courtApplications = new ArrayList<>();
        }
        courtApplications.add(courtApplication);
        final Hearing.Builder hearingBuilder = hearing();
        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();

        final Stream<CourtApplicationCase> courtApplicationCases = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty);

        if (isAllActiveCases(courtApplicationCases) && isNotEmpty(hearing.getProsecutionCases())) {
            final List<CourtApplicationCase> courtApplicationCasesForWhichWeNeedToCreateHearing = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                    .filter(courtApplicationCase -> hearing.getProsecutionCases().stream().noneMatch(prosecutionCase -> courtApplicationCase.getProsecutionCaseId().equals(prosecutionCase.getId())))
                    .collect(toList());

            courtApplicationCasesForWhichWeNeedToCreateHearing.forEach(courtApplicationCase -> {
                final Optional<JsonObject> prosecutionCaseDetailById = progressionService.getProsecutionCaseDetailById(event, courtApplicationCase.getProsecutionCaseId().toString());
                final Optional<ProsecutionCase> prosecutionCase = prosecutionCaseDetailById.map(jsonObject -> jsonObjectToObjectConverter.convert(prosecutionCaseDetailById.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class));
                prosecutionCase.ifPresent(p -> {
                    if (nonNull(masterDefendant)) {
                        final List<Defendant> defendants = p.getDefendants().stream()
                                .filter(defendant -> defendant.getMasterDefendantId().equals(masterDefendant.getMasterDefendantId()))
                                .map(defendant -> defendant().withValuesFrom(defendant).withDefendantCaseJudicialResults(null).build())
                                .collect(toList());

                        final List<Defendant> defendantList = defendants.stream()
                                .map(this::updatedDefendant)
                                .filter(defendant -> isNotEmpty(defendant.getOffences()))
                                .collect(toList());

                        prosecutionCaseList.add(prosecutionCase().withValuesFrom(p).withDefendants(defendantList).build());
                    }
                });
            });

            if (isNotEmpty(prosecutionCaseList)) {
                hearingBuilder.withProsecutionCases(prosecutionCaseList);
            }
        }
        return hearingBuilder
                .withType(hearing.getType())
                .withCourtApplications(courtApplications)
                .withHearingCaseNotes(hearing.getHearingCaseNotes())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                .withJudiciary(hearing.getJudiciary())
                .withDefenceCounsels(hearing.getDefenceCounsels())
                .withHearingDays(hearing.getHearingDays())
                .withCourtCentre(hearing.getCourtCentre())
                .withProsecutionCounsels(hearing.getProsecutionCounsels())
                .withDefendantAttendance(hearing.getDefendantAttendance())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withId(hearing.getId())
                .withApplicantCounsels(hearing.getApplicantCounsels())
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels())
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial())
                .withHasSharedResults(hearing.getHasSharedResults())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withRespondentCounsels(hearing.getRespondentCounsels())
                .build();
    }

    private ListCourtHearing buildDefaultHearingNeeds(final CourtHearingRequest courtHearingRequest, final CourtApplication courtApplication, final List<ProsecutionCase> prosecutionCases) {
        final HearingListingNeeds.Builder hearingListingNeedsBuilder = HearingListingNeeds.hearingListingNeeds()
                .withBookedSlots(courtHearingRequest.getBookedSlots())
                .withCourtApplications(singletonList(courtApplication))
                .withCourtCentre(courtHearingRequest.getCourtCentre())
                .withEarliestStartDateTime(courtHearingRequest.getEarliestStartDateTime())
                .withEndDate(courtHearingRequest.getEndDate())
                .withEstimatedMinutes(courtHearingRequest.getEstimatedMinutes())
                .withEstimatedDuration(courtHearingRequest.getEstimatedDuration())
                .withId(courtHearingRequest.getId())
                .withJurisdictionType(courtHearingRequest.getJurisdictionType())
                .withListedStartDateTime(courtHearingRequest.getListedStartDateTime())
                .withType(courtHearingRequest.getHearingType())
                .withWeekCommencingDate(courtHearingRequest.getWeekCommencingDate())
                .withJudiciary(courtHearingRequest.getJudiciary())
                .withBookingType(courtHearingRequest.getBookingType())
                .withPriority(courtHearingRequest.getPriority())
                .withSpecialRequirements(courtHearingRequest.getSpecialRequirements())
                .withListingDirections(courtHearingRequest.getListingDirections());

        if (isNotEmpty(prosecutionCases)) {
            hearingListingNeedsBuilder.withProsecutionCases(prosecutionCases);
        }
        if (isNull(courtHearingRequest.getEstimatedMinutes())) {
            hearingListingNeedsBuilder.withEstimatedMinutes(0);
        }

        if (nonNull(courtHearingRequest.getEstimatedDuration())) {
            hearingListingNeedsBuilder.withEstimatedDuration(courtHearingRequest.getEstimatedDuration());
        }

        final HearingListingNeeds hearingListingNeeds = hearingListingNeedsBuilder.build();

        return ListCourtHearing.listCourtHearing()
                .withHearings(singletonList(hearingListingNeeds))
                .build();
    }

    private boolean isCaseNotFoundInCC(final JsonEnvelope event, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        final String reference;
        if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
            reference = prosecutionCaseIdentifier.getCaseURN();
            LOGGER.info("referecence is caseURN: {}", reference);
        } else {
            reference = prosecutionCaseIdentifier.getProsecutionAuthorityReference();
            LOGGER.info("referecence is ProsecutionAuthority {}", reference);
        }
        final Optional<JsonObject> jsonObject = progressionService.caseExistsByCaseUrn(event, reference);
        return jsonObject.isPresent() && jsonObject.get().isEmpty();
    }

    private CreateHearingApplicationRequest buildApplicationHearingRequest(final InitiateCourtHearingAfterSummonsApproved initiateCourtHearingAfterSummonsApproved, final CourtApplication application, final UUID courtHearingId, final SummonsTemplateType summonsTemplateType) {
        return createHearingApplicationRequest()
                .withHearingId(courtHearingId)
                .withApplicationRequests(singletonList(courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(application.getId())
                        .withSummonsRequired(summonsTemplateType == SummonsTemplateType.BREACH ? SummonsType.BREACH : SummonsType.APPLICATION)
                        .withSummonsApprovedOutcome(initiateCourtHearingAfterSummonsApproved.getSummonsApprovedOutcome())
                        .withCourtApplicationPartyId(application.getSubject().getId())
                        .build())).build();
    }
}
