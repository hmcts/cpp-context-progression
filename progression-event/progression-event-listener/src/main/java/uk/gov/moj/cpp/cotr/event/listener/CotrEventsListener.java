package uk.gov.moj.cpp.cotr.event.listener;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.DefendantCotrServed;
import uk.gov.justice.cpp.progression.event.CotrArchived;
import uk.gov.justice.cpp.progression.event.CotrCreated;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrServed;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrUpdated;
import uk.gov.justice.progression.event.CotrNotes;
import uk.gov.justice.progression.event.DefendantAddedToCotr;
import uk.gov.justice.progression.event.DefendantRemovedFromCotr;
import uk.gov.justice.progression.event.FurtherInfoForDefenceCotrAdded;
import uk.gov.justice.progression.event.FurtherInfoForProsecutionCotrAdded;
import uk.gov.justice.progression.event.ReviewNoteType;
import uk.gov.justice.progression.event.ReviewNotesUpdated;
import uk.gov.justice.progression.query.Certification;
import uk.gov.justice.progression.query.ProsecutionFormData;
import uk.gov.justice.progression.query.ProsecutionQuestions;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.PolarQuestion;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRProsecutionFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefenceFurtherInfoRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefendantRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRProsecutionFurtherInfoRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776", "pmd:NullAssignment", "squid:S1125"})
@ServiceComponent(EVENT_LISTENER)
public class CotrEventsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CotrEventsListener.class);
    private static final String RECEIVED_EVENT_WITH_PAYLOAD = "Received '{}' event with payload {}";
    private static final String USER_ID_NOT_SUPPLIED = "User id Not Supplied to persist COTR Served by information";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd");
    private static final String YES = "Yes";

    @Inject
    private COTRDetailsRepository cotrDetailsRepository;

    @Inject
    private COTRDefendantRepository cotrDefendantRepository;

    @Inject
    private COTRProsecutionFurtherInfoRepository cotrProsecutionFurtherInfoRepository;

    @Inject
    private COTRDefenceFurtherInfoRepository cotrDefenceFurtherInfoRepository;


    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.cotr-created")
    public void cotrCreated(final Envelope<CotrCreated> event) {
        final CotrCreated cotrCreated = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.cotr-created", cotrCreated.getCotrId());
        }

        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setId(cotrCreated.getCotrId());
        cotrDetailsEntity.setArchived(false);
        cotrDetailsEntity.setHearingId(cotrCreated.getHearingId());
        cotrDetailsEntity.setProsecutionCaseId(cotrCreated.getCaseId());

        cotrDetailsRepository.save(cotrDetailsEntity);

        cotrCreated.getDefendants().stream().forEach(defendant -> {
            final COTRDefendantEntity cotrDefendantEntity = new COTRDefendantEntity();
            cotrDefendantEntity.setId(randomUUID());
            cotrDefendantEntity.setCotrId(cotrCreated.getCotrId());
            cotrDefendantEntity.setDefendantId(defendant.getId());
            cotrDefendantEntity.setdNumber(defendant.getDnumber());
            cotrDefendantRepository.save(cotrDefendantEntity);
        });
    }

    @Handles("progression.event.prosecution-cotr-served")
    public void serveProsecutionCotr(final Envelope<ProsecutionCotrServed> event) {
        final ProsecutionCotrServed serveProsecutionCotrCreated = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.prosecution-cotr-served", serveProsecutionCotrCreated.getCotrId());
        }
        final COTRDetailsEntity cotrDetailsEntity = cotrDetailsRepository.findBy(serveProsecutionCotrCreated.getCotrId());

        final ProsecutionFormData prosecutionFormData = ProsecutionFormData.prosecutionFormData()
                .withProsecutionQuestions(ProsecutionQuestions.prosecutionQuestions()
                        .withFurtherInformationToAssistTheCourt(serveProsecutionCotrCreated.getFurtherInformationToAssistTheCourt())
                        .withHasAllDisclosureBeenProvided(serveProsecutionCotrCreated.getHasAllDisclosureBeenProvided())
                        .withHasAllEvidenceToBeReliedOnBeenServed(serveProsecutionCotrCreated.getHasAllEvidenceToBeReliedOnBeenServed())
                        .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(serveProsecutionCotrCreated.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed())
                        .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(serveProsecutionCotrCreated.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement())
                        .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(serveProsecutionCotrCreated.getHaveEditedAbeInterviewsBeenPreparedAndAgreed())
                        .withHaveInterpretersForWitnessesBeenArranged(serveProsecutionCotrCreated.getHaveInterpretersForWitnessesBeenArranged())
                        .withHaveOtherDirectionsBeenCompliedWith(serveProsecutionCotrCreated.getHaveOtherDirectionsBeenCompliedWith())
                        .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(serveProsecutionCotrCreated.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved())
                        .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(serveProsecutionCotrCreated.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend())
                        .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(serveProsecutionCotrCreated.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury())
                        .withIsTheTimeEstimateCorrect(serveProsecutionCotrCreated.getIsTheTimeEstimateCorrect())
                        .withLastRecordedTimeEstimate(serveProsecutionCotrCreated.getLastRecordedTimeEstimate())
                        .build())
                .withCertification(Certification.certification()
                        .withApplyForThePtrToBeVacated(serveProsecutionCotrCreated.getApplyForThePtrToBeVacated())
                        .withCertificationDate(serveProsecutionCotrCreated.getCertificationDate())
                        .withCertifyThatTheProsecutionIsTrialReady(serveProsecutionCotrCreated.getCertifyThatTheProsecutionIsTrialReady())
                        .withFormCompletedOnBehalfOfTheProsecutionBy(serveProsecutionCotrCreated.getFormCompletedOnBehalfOfTheProsecutionBy())
                        .build()).build();

        cotrDetailsEntity.setProsecutionFormData(objectToJsonObjectConverter.convert(prosecutionFormData).toString());
        cotrDetailsRepository.save(cotrDetailsEntity);
    }

    @Handles("progression.event.prosecution-cotr-updated")
    public void updateProsecutionCotr(final Envelope<ProsecutionCotrUpdated> event) {
        final ProsecutionCotrUpdated prosecutionCotrUpdated = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.prosecution-cotr-served", prosecutionCotrUpdated.getCotrId());
        }
        final COTRDetailsEntity cotrDetailsEntity = cotrDetailsRepository.findBy(prosecutionCotrUpdated.getCotrId());
        if(nonNull(cotrDetailsEntity) && nonNull(cotrDetailsEntity.getProsecutionFormData())) {
            final JsonObject prosecutionFormDataJson = stringToJsonObjectConverter.convert(cotrDetailsEntity.getProsecutionFormData());
            final ProsecutionFormData prosecutionFormDataEntity = jsonObjectConverter.convert(prosecutionFormDataJson, ProsecutionFormData.class);

            final ProsecutionFormData prosecutionFormData = ProsecutionFormData.prosecutionFormData()
                    .withProsecutionQuestions(ProsecutionQuestions.prosecutionQuestions()
                            .withValuesFrom(prosecutionFormDataEntity.getProsecutionQuestions())
                            .withFormCompletedOnBehalfOfProsecutionBy(nonNull(prosecutionFormDataEntity.getProsecutionQuestions().getFormCompletedOnBehalfOfProsecutionBy())  && nonNull(prosecutionCotrUpdated.getFormCompletedOnBehalfOfProsecutionBy())?
                                    prosecutionCotrUpdated.getFormCompletedOnBehalfOfProsecutionBy() :
                                    prosecutionFormDataEntity.getProsecutionQuestions().getFormCompletedOnBehalfOfProsecutionBy())
                            .build())
                    .withCertification(Certification.certification()
                            .withValuesFrom(prosecutionFormDataEntity.getCertification())
                            .withCertificationDate(prosecutionCotrUpdated.getCertificationDate())
                            .withCertifyThatTheProsecutionIsTrialReady(nonNull(prosecutionCotrUpdated.getCertifyThatTheProsecutionIsTrialReady()) ? PolarQuestion.polarQuestion()
                                    .withAnswer(prosecutionCotrUpdated.getCertifyThatTheProsecutionIsTrialReady().getAnswer())
                                    .withDetails(prosecutionFormDataEntity.getCertification().getCertifyThatTheProsecutionIsTrialReady().getDetails())
                                    .build() : null)
                            .withFormCompletedOnBehalfOfTheProsecutionBy(prosecutionFormDataEntity.getCertification().getFormCompletedOnBehalfOfTheProsecutionBy())
                            .build()).build();
            cotrDetailsEntity.setProsecutionFormData(objectToJsonObjectConverter.convert(prosecutionFormData).toString());
            cotrDetailsRepository.save(cotrDetailsEntity);

            final COTRProsecutionFurtherInfoEntity cotrProsecutionFurtherInfoEntity = new COTRProsecutionFurtherInfoEntity();
            cotrProsecutionFurtherInfoEntity.setId(randomUUID());
            cotrProsecutionFurtherInfoEntity.setCotrId(prosecutionCotrUpdated.getCotrId());
            cotrProsecutionFurtherInfoEntity.setFurtherInformation(nonNull(prosecutionCotrUpdated.getFurtherProsecutionInformationProvidedAfterCertification()) ? prosecutionCotrUpdated.getFurtherProsecutionInformationProvidedAfterCertification().getDetails():null);

            if(nonNull(prosecutionCotrUpdated.getCertificationDate())) {
                final ZonedDateTime zdt = LocalDate.parse(prosecutionCotrUpdated.getCertificationDate().getDetails(), dtf).atStartOfDay(ZoneOffset.UTC);
                cotrProsecutionFurtherInfoEntity.setAddedOn(zdt);
            } else {
                cotrProsecutionFurtherInfoEntity.setAddedOn(null);
            }

            cotrProsecutionFurtherInfoEntity.setInfoAddedBy(randomUUID());
            cotrProsecutionFurtherInfoEntity.setInfoAddedByName(nonNull(prosecutionCotrUpdated.getFormCompletedOnBehalfOfProsecutionBy()) ?
                    prosecutionCotrUpdated.getFormCompletedOnBehalfOfProsecutionBy().getDetails() : null);
            cotrProsecutionFurtherInfoEntity.setIsCertificationReady(nonNull(prosecutionCotrUpdated.getCertifyThatTheProsecutionIsTrialReady()) ?
                    checkCertifyThatTheProsecutionIsTrialReady(prosecutionCotrUpdated.getCertifyThatTheProsecutionIsTrialReady().getAnswer()) : null);

            cotrProsecutionFurtherInfoRepository.save(cotrProsecutionFurtherInfoEntity);

        }
    }

    private boolean checkCertifyThatTheProsecutionIsTrialReady(final String answer) {
        return YES.equalsIgnoreCase(answer)?true:false;
    }

    @Handles("progression.event.cotr-archived")
    public void archiveCotr(final Envelope<CotrArchived> event) {
        final CotrArchived cotrArchived = event.payload();
        final UUID cotrId = cotrArchived.getCotrId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "public.progression.cotr-archived", cotrId);
        }
        final COTRDetailsEntity cotrDetailsEntity = cotrDetailsRepository.findBy(cotrId);
        cotrDetailsEntity.setArchived(true);
        cotrDetailsRepository.save(cotrDetailsEntity);
    }

    @Handles("progression.event.defendant-cotr-served")
    public void serveDefendantCotr(final Envelope<DefendantCotrServed> event) {
        final DefendantCotrServed defendantCotrServed = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.defendant-cotr-served", defendantCotrServed.getCotrId());
        }
        final UUID userId = getUserId(event.metadata().userId());
        final List<COTRDefendantEntity> cotrDefendantEntities = cotrDefendantRepository.findByCotrIdAndDefendantId(defendantCotrServed.getCotrId(), defendantCotrServed.getDefendantId());
        cotrDefendantEntities.stream().forEach(cotrDefendantEntity -> {
            cotrDefendantEntity.setDefendantForm(defendantCotrServed.getDefendantFormData());
            cotrDefendantEntity.setServedBy(userId);
            cotrDefendantEntity.setServedByName(defendantCotrServed.getServedByName());
            cotrDefendantEntity.setServedOn(ZonedDateTime.now());
            cotrDefendantRepository.save(cotrDefendantEntity);
        } );
    }

    @Handles("progression.event.defendant-added-to-cotr")
    public void handleDefendantAddedToCotrEvent(final Envelope<DefendantAddedToCotr> event) {

        final DefendantAddedToCotr defendantAddedToCotr = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.defendant-added-to-cotr", defendantAddedToCotr.getCotrId());
        }

        final COTRDefendantEntity cotrDefendantEntity = new COTRDefendantEntity();
        cotrDefendantEntity.setId(randomUUID());
        cotrDefendantEntity.setCotrId(defendantAddedToCotr.getCotrId());
        cotrDefendantEntity.setDefendantId(defendantAddedToCotr.getDefendantId());
        cotrDefendantEntity.setdNumber(defendantAddedToCotr.getDefendantNumber());
        cotrDefendantRepository.save(cotrDefendantEntity);

    }

    @Handles("progression.event.defendant-removed-from-cotr")
    public void handleDefendantRemovedFromCotrEvent(final Envelope<DefendantRemovedFromCotr> event) {

        final DefendantRemovedFromCotr defendantRemovedFromCotr = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.defendant-removed-from-cotr", defendantRemovedFromCotr.getCotrId());
        }

        final COTRDefendantEntity cotrDefendantEntity = cotrDefendantRepository.findByCotrIdAndDefendantId(defendantRemovedFromCotr.getCotrId(), defendantRemovedFromCotr.getDefendantId()).get(0);
        cotrDefendantRepository.remove(cotrDefendantEntity);

    }

    @Handles("progression.event.further-info-for-prosecution-cotr-added")
    public void handleFurtherInfoForProsecutionCotrAdded(final Envelope<FurtherInfoForProsecutionCotrAdded> event) {
        final FurtherInfoForProsecutionCotrAdded furtherInfoForProsecutionCotrAdded = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.further-info-for-prosecution-cotr-added", furtherInfoForProsecutionCotrAdded.getCotrId());
        }

        final COTRProsecutionFurtherInfoEntity cotrProsecutionFurtherInfoEntity = new COTRProsecutionFurtherInfoEntity();
        cotrProsecutionFurtherInfoEntity.setId(randomUUID());
        cotrProsecutionFurtherInfoEntity.setCotrId(furtherInfoForProsecutionCotrAdded.getCotrId());
        cotrProsecutionFurtherInfoEntity.setFurtherInformation(furtherInfoForProsecutionCotrAdded.getMessage());
        cotrProsecutionFurtherInfoEntity.setInfoAddedBy(furtherInfoForProsecutionCotrAdded.getAddedBy());
        cotrProsecutionFurtherInfoEntity.setInfoAddedByName(furtherInfoForProsecutionCotrAdded.getAddedByName());
        cotrProsecutionFurtherInfoEntity.setIsCertificationReady(furtherInfoForProsecutionCotrAdded.getIsCertificationReady());
        cotrProsecutionFurtherInfoEntity.setAddedOn(ZonedDateTime.now());

        cotrProsecutionFurtherInfoRepository.save(cotrProsecutionFurtherInfoEntity);

    }

    @Handles("progression.event.further-info-for-defence-cotr-added")
    public void handleFurtherInfoForDefenceCotrAdded(final Envelope<FurtherInfoForDefenceCotrAdded> event) {
        final FurtherInfoForDefenceCotrAdded furtherInfoForDefenceCotrAdded = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.further-info-for-defence-cotr-added", furtherInfoForDefenceCotrAdded.getCotrId());
        }

        final COTRDefenceFurtherInfoEntity cotrDefenceFurtherInfoEntity = new COTRDefenceFurtherInfoEntity();
        cotrDefenceFurtherInfoEntity.setId(randomUUID());
        cotrDefenceFurtherInfoEntity.setCotrDefendant(furtherInfoForDefenceCotrAdded.getDefendantId());
        cotrDefenceFurtherInfoEntity.setFurtherInformation(furtherInfoForDefenceCotrAdded.getMessage());
        cotrDefenceFurtherInfoEntity.setIsCertificationReady(furtherInfoForDefenceCotrAdded.getIsCertificationReady());
        cotrDefenceFurtherInfoEntity.setInfoAddedBy(furtherInfoForDefenceCotrAdded.getAddedBy());
        cotrDefenceFurtherInfoEntity.setInfoAddedByName(furtherInfoForDefenceCotrAdded.getAddedByName());
        cotrDefenceFurtherInfoEntity.setAddedOn(ZonedDateTime.now());

        cotrDefenceFurtherInfoRepository.save(cotrDefenceFurtherInfoEntity);

    }

    @Handles("progression.event.review-notes-updated")
    public void handleReviewNotesUpdated(final Envelope<ReviewNotesUpdated> event) {
        final ReviewNotesUpdated reviewNotesUpdated = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.review-notes-updated", reviewNotesUpdated.getCotrId());
        }

        final COTRDetailsEntity cotrDetailsEntity = cotrDetailsRepository.findBy(reviewNotesUpdated.getCotrId());

        final JsonArrayBuilder reviewNotesArrayBuilder = createArrayBuilder();
        for (final CotrNotes cotrNote : reviewNotesUpdated.getCotrNotes()) {
            cotrNote.getReviewNotes().forEach(reviewNote -> {
                final JsonObject reviewNotesJson = createObjectBuilder()
                        .add("id", reviewNote.getId().toString())
                        .add("comment", reviewNote.getComment())
                        .build();
                reviewNotesArrayBuilder.add(reviewNotesJson);
            });
            final JsonArray reviewNotes = reviewNotesArrayBuilder.build();
            if (cotrNote.getReviewNoteType() == ReviewNoteType.CASE_PROGRESSION) {
                cotrDetailsEntity.setCaseProgressionReviewNote(reviewNotes.toString());
            } else if (cotrNote.getReviewNoteType() == ReviewNoteType.LISTING) {
                cotrDetailsEntity.setListingReviewNotes(reviewNotes.toString());
            } else if (cotrNote.getReviewNoteType() == ReviewNoteType.JUDGE) {
                cotrDetailsEntity.setJudgeReviewNotes(reviewNotes.toString());
            }
        }
        cotrDetailsRepository.save(cotrDetailsEntity);

    }

    private UUID getUserId(final Optional<String> userId) {
        final String id = userId.orElseThrow(() -> new IllegalArgumentException(USER_ID_NOT_SUPPLIED));
        return UUID.fromString(id);
    }
}
