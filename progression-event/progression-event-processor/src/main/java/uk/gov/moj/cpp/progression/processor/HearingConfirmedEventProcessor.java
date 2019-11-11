package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SummonsService;
import uk.gov.moj.cpp.progression.transformer.ProsecutionCasesReferredToCourtTransformer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S3655", "squid:S2629", "squid:CallToDeprecatedMethod"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingConfirmedEventProcessor {

    public static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";
    private static final String PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING = "progression.command-link-prosecution-cases-to-hearing";

    static final String PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE = "progression.command-enrich-hearing-initiate";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingConfirmedEventProcessor.class.getName());
    @Inject
    ProgressionService progressionService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationService notificationService;

    @Inject
    private SummonsService summonsService;

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("public.listing.hearing-confirmed")
    public void processEvent(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.listing.hearing-confirmed event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingConfirmed.class);

        final Initiate hearingInitiate = Initiate.initiate()
                .withHearing(progressionService.transformConfirmedHearing(hearingConfirmed.getConfirmedHearing(), jsonEnvelope))
                .build();

        progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, hearingInitiate);

        final List<UUID> applicationIds = hearingConfirmed.getConfirmedHearing().getCourtApplicationIds();
        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = hearingConfirmed.getConfirmedHearing().getProsecutionCases();

        final Hearing hearing = hearingInitiate.getHearing();
        final ZonedDateTime hearingStartDateTime = getEarliestDate(hearing.getHearingDays());
        LOGGER.info("List of application ids {} ", applicationIds);

        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).orElse(new ArrayList<>());

        courtApplications.forEach(courtApplication -> LOGGER.info("sending notification for Application : {}", objectToJsonObjectConverter.convert(courtApplication)));
        courtApplications.forEach(courtApplication -> notificationService.sendNotification(jsonEnvelope, UUID.randomUUID(), courtApplication, hearing.getCourtCentre(), hearingStartDateTime));

        if (CollectionUtils.isNotEmpty(applicationIds)) {
            LOGGER.info("Update application status to LISTED, associate Hearing with id: {} to Applications with ids {} and generate summons", hearing.getId(), applicationIds);
            progressionService.updateCourtApplicationStatus(jsonEnvelope, applicationIds, ApplicationStatus.LISTED);
            progressionService.linkApplicationsToHearing(jsonEnvelope, hearing, applicationIds, HearingListingStatus.HEARING_INITIALISED);
            summonsService.generateSummonsPayload(jsonEnvelope, hearingConfirmed.getConfirmedHearing());
        }

        if (CollectionUtils.isNotEmpty(confirmedProsecutionCases)){
            confirmedProsecutionCases.forEach(prosecutionCase ->
                    sender.send(enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING).apply(
                            CaseLinkedToHearing.caseLinkedToHearing().withHearingId(hearing.getId()).withCaseId(prosecutionCase.getId()).build()))
            );
            progressionService.prepareSummonsData(jsonEnvelope, hearingConfirmed.getConfirmedHearing());
        }

        final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);
        final JsonEnvelope hearingInitiateTransformedPayload = enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE).apply(hearingInitiateCommand);

        LOGGER.info(" hearing initiate transformed payload {}", hearingInitiateTransformedPayload.toObfuscatedDebugString());


        sender.send(hearingInitiateTransformedPayload);
    }

    @Handles("progression.hearing-initiate-enriched")
    public void processHearingInitiatedEnrichedEvent(JsonEnvelope jsonEnvelope) {

        LOGGER.info(" hearing initiate with payload {}", jsonEnvelope.toObfuscatedDebugString());

        final Initiate hearingInitiate = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), Initiate.class);

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND).apply(objectToJsonObjectConverter.convert(hearingInitiate)));
        if (CollectionUtils.isNotEmpty(hearingInitiate.getHearing().getProsecutionCases())) {
            final List<ProsecutionCasesReferredToCourt> prosecutionCasesReferredToCourts = ProsecutionCasesReferredToCourtTransformer
                    .transform(hearingInitiate, null);

            prosecutionCasesReferredToCourts.stream().forEach(prosecutionCasesReferredToCourt -> {
                final JsonObject prosecutionCasesReferredToCourtJson = objectToJsonObjectConverter.convert(prosecutionCasesReferredToCourt);

                final JsonEnvelope caseReferToCourt = enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT)
                        .apply(prosecutionCasesReferredToCourtJson);

                LOGGER.info(" Prosecution Cases Referred To Courts with payload {}", caseReferToCourt.toObfuscatedDebugString());

                sender.send(caseReferToCourt);
            });
            progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);
        }


    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
