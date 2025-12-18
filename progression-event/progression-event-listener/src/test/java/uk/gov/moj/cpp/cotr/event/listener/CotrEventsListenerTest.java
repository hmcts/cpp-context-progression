package uk.gov.moj.cpp.cotr.event.listener;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.DefendantCotrServed;
import uk.gov.justice.cpp.progression.event.CotrArchived;
import uk.gov.justice.cpp.progression.event.CotrCreated;
import uk.gov.justice.cpp.progression.event.Defendants;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrServed;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrUpdated;
import uk.gov.justice.progression.event.CotrNotes;
import uk.gov.justice.progression.event.DefendantAddedToCotr;
import uk.gov.justice.progression.event.DefendantRemovedFromCotr;
import uk.gov.justice.progression.event.FurtherInfoForDefenceCotrAdded;
import uk.gov.justice.progression.event.FurtherInfoForProsecutionCotrAdded;
import uk.gov.justice.progression.event.ReviewNoteType;
import uk.gov.justice.progression.event.ReviewNotes;
import uk.gov.justice.progression.event.ReviewNotesUpdated;
import uk.gov.justice.progression.query.Certification;
import uk.gov.justice.progression.query.ProsecutionFormData;
import uk.gov.justice.progression.query.ProsecutionQuestions;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.PolarQuestion;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRProsecutionFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefenceFurtherInfoRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefendantRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRProsecutionFurtherInfoRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CotrEventsListenerTest {

    public static final String FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION = "furtherProsecutionInformationProvidedAfterCertification";
    public static final String CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY = "certifyThatTheProsecutionIsTrialReady";
    @Mock
    private Envelope<CotrCreated> cotrCreatedEnvelope;

    @Mock
    private Envelope<CotrArchived> cotrArchivedEnvelope;

    @Mock
    private Envelope<ProsecutionCotrServed> serveProsecutionCotrEnvelope;

    @Mock
    private Envelope<ProsecutionCotrUpdated> prosecutionCotrUpdatedEnvelope;

    @Mock
    private Envelope<DefendantCotrServed> serveDefendentCotrEnvelope;

    @Mock
    private Envelope<DefendantAddedToCotr> defendantAddedToCotrEnvelope;

    @Mock
    private Envelope<DefendantRemovedFromCotr> defendantRemovedFromCotrEnvelope;

    @Mock
    private Envelope<FurtherInfoForProsecutionCotrAdded> furtherInfoForProsecutionCotrAddedEnvelope;

    @Mock
    private Envelope<FurtherInfoForDefenceCotrAdded> furtherInfoForDefenceCotrAddedEnvelope;

    @Mock
    private Envelope<ReviewNotesUpdated> reviewNotesUpdatedEnvelope;

    @Mock
    private Metadata metadata;

    @Mock
    private COTRDetailsRepository cotrDetailsRepository;

    @Mock
    private COTRDefendantRepository cotrDefendantRepository;

    @Mock
    private COTRProsecutionFurtherInfoRepository cotrProsecutionFurtherInfoRepository;

    @Mock
    private COTRDefenceFurtherInfoRepository cotrDefenceFurtherInfoRepository;

    @InjectMocks
    private CotrEventsListener cotrEventsListener;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<COTRDetailsEntity> cotrDetailsEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<COTRDefendantEntity> cotrDefendantEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<COTRProsecutionFurtherInfoEntity> cotrProsecutionFurtherInfoEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<COTRDefenceFurtherInfoEntity> cotrDefenceFurtherInfoEntityArgumentCaptor;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldTestCotrCreated() {
        final CotrCreated cotrCreated = CotrCreated.cotrCreated()
                .withCotrId(randomUUID())
                .withHearingId(randomUUID())
                .withCaseId(randomUUID())
                .withDefendants(Arrays.asList(
                        Defendants.defendants()
                                .withId(randomUUID())
                                .withDnumber(1)
                                .build(),
                        Defendants.defendants()
                                .withId(randomUUID())
                                .withDnumber(2)
                                .build()))
                .build();
        when(cotrCreatedEnvelope.payload()).thenReturn(cotrCreated);
        cotrEventsListener.cotrCreated(cotrCreatedEnvelope);

        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());
        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrCreated.getCotrId()));
        assertThat(savedCOTRDetailsEntity.getArchived(), is(false));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrCreated.getHearingId()));
        assertThat(savedCOTRDetailsEntity.getProsecutionCaseId(), is(cotrCreated.getCaseId()));
        assertNull(savedCOTRDetailsEntity.getProsecutionFormData());

        verify(cotrDefendantRepository, times(2)).save(cotrDefendantEntityArgumentCaptor.capture());
        AtomicInteger dNumberCounter = new AtomicInteger();
        final List<COTRDefendantEntity> cotrDefendantEntity = cotrDefendantEntityArgumentCaptor.getAllValues();
        cotrDefendantEntity.stream().forEach(cotrDefendant -> {
            assertNotNull(cotrDefendant.getId());
            assertThat(cotrDefendant.getCotrId(), is(savedCOTRDetailsEntity.getId()));
            assertNotNull(cotrDefendant.getDefendantId());
            assertThat(cotrDefendant.getdNumber(), is(dNumberCounter.incrementAndGet()));
        });
    }

    @Test
    public void shouldTestServeProsecutionCotr() {
        final ProsecutionCotrServed serveProsecutionCotr = ProsecutionCotrServed.prosecutionCotrServed()
                .withCotrId(randomUUID())
                .withHearingId(randomUUID())
                .build();
        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setId(serveProsecutionCotr.getCotrId());
        cotrDetailsEntity.setHearingId(serveProsecutionCotr.getHearingId());

        when(serveProsecutionCotrEnvelope.payload()).thenReturn(serveProsecutionCotr);
        when(cotrDetailsRepository.findBy(serveProsecutionCotr.getCotrId())).thenReturn(cotrDetailsEntity);

        cotrEventsListener.serveProsecutionCotr(serveProsecutionCotrEnvelope);
        verify(cotrDetailsRepository).findBy(serveProsecutionCotr.getCotrId());
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
    }

    @Test
    public void shouldTestUpdateProsecutionCotr() {
        final ProsecutionCotrUpdated prosecutionCotrUpdated = ProsecutionCotrUpdated.prosecutionCotrUpdated()
                .withCotrId(randomUUID())
                .withHearingId(randomUUID())
                .withFurtherProsecutionInformationProvidedAfterCertification(setPolarQuestion(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION))
                .withFormCompletedOnBehalfOfProsecutionBy(setPolarQuestion("formCompletedOnBehalfOfProsecutionBy"))
                .withCertificationDate(PolarQuestion.polarQuestion()
                        .withDetails("2022-02-02")
                        .build())
                .build();
        ProsecutionFormData prosecutionFormData = getProsecutionFormData();
        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setProsecutionFormData(prosecutionFormData.toString());
        cotrDetailsEntity.setId(prosecutionCotrUpdated.getCotrId());
        cotrDetailsEntity.setHearingId(prosecutionCotrUpdated.getHearingId());

        final JsonObject jsonObject = getJsonObject();
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionFormData.class)).thenReturn(getProsecutionFormData());
        cotrDetailsEntity.setProsecutionFormData(getProsecutionFormData().toString());
        when(prosecutionCotrUpdatedEnvelope.payload()).thenReturn(prosecutionCotrUpdated);
        when(cotrDetailsRepository.findBy(prosecutionCotrUpdated.getCotrId())).thenReturn(cotrDetailsEntity);

        cotrEventsListener.updateProsecutionCotr(prosecutionCotrUpdatedEnvelope);
        verify(cotrDetailsRepository).findBy(prosecutionCotrUpdated.getCotrId());
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertTrue(savedCOTRDetailsEntity.getProsecutionFormData().contains(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION));
    }

    @Test
    public void shouldTestUpdateProsecutionCotr_furtherProsecutionInformationProvidedAfterCertification() {
        final ProsecutionCotrUpdated prosecutionCotrUpdated = ProsecutionCotrUpdated.prosecutionCotrUpdated()
                .withCotrId(randomUUID())
                .withHearingId(randomUUID())
                .withCertifyThatTheProsecutionIsTrialReady(PolarQuestion.polarQuestion()
                        .withAnswer("Y")
                        .build())
                .withFurtherProsecutionInformationProvidedAfterCertification(setPolarQuestion(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION))
                .build();
        ProsecutionFormData prosecutionFormData = getProsecutionFormData();
        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setProsecutionFormData(prosecutionFormData.toString());
        cotrDetailsEntity.setId(prosecutionCotrUpdated.getCotrId());
        cotrDetailsEntity.setHearingId(prosecutionCotrUpdated.getHearingId());
        final JsonObject jsonObject = getJsonObject();
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionFormData.class)).thenReturn(getProsecutionFormData());
        cotrDetailsEntity.setProsecutionFormData(getProsecutionFormData().toString());
        when(prosecutionCotrUpdatedEnvelope.payload()).thenReturn(prosecutionCotrUpdated);
        when(cotrDetailsRepository.findBy(prosecutionCotrUpdated.getCotrId())).thenReturn(cotrDetailsEntity);

        cotrEventsListener.updateProsecutionCotr(prosecutionCotrUpdatedEnvelope);
        verify(cotrDetailsRepository).findBy(prosecutionCotrUpdated.getCotrId());
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertTrue(savedCOTRDetailsEntity.getProsecutionFormData().contains(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION));
        assertTrue(savedCOTRDetailsEntity.getProsecutionFormData().contains(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY));
    }

    @Test
    public void shouldTestUpdateProsecutionCotr_certifyThatTheProsecutionIsTrialReady() {
        final ProsecutionCotrUpdated prosecutionCotrUpdated = ProsecutionCotrUpdated.prosecutionCotrUpdated()
                .withCotrId(randomUUID())
                .withHearingId(randomUUID())
                .withFurtherProsecutionInformationProvidedAfterCertification(setPolarQuestion(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION))
                .build();
        ProsecutionFormData prosecutionFormData = getProsecutionFormData();
        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setProsecutionFormData(prosecutionFormData.toString());
        cotrDetailsEntity.setId(prosecutionCotrUpdated.getCotrId());
        cotrDetailsEntity.setHearingId(prosecutionCotrUpdated.getHearingId());
        final JsonObject jsonObject = getJsonObject();
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionFormData.class)).thenReturn(getProsecutionFormData());
        cotrDetailsEntity.setProsecutionFormData(getProsecutionFormData().toString());
        when(prosecutionCotrUpdatedEnvelope.payload()).thenReturn(prosecutionCotrUpdated);
        when(cotrDetailsRepository.findBy(prosecutionCotrUpdated.getCotrId())).thenReturn(cotrDetailsEntity);

        cotrEventsListener.updateProsecutionCotr(prosecutionCotrUpdatedEnvelope);
        verify(cotrDetailsRepository).findBy(prosecutionCotrUpdated.getCotrId());
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertTrue(savedCOTRDetailsEntity.getProsecutionFormData().contains(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION));
        assertFalse(savedCOTRDetailsEntity.getProsecutionFormData().contains(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY));
    }

    private JsonObject getJsonObject() {
       return JsonObjects.createObjectBuilder()
                .add("prosecutionQuestions",JsonObjects.createObjectBuilder()
                        .add(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION, FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION)
                        .build()
                )
                .build();
    }

    private ProsecutionFormData getProsecutionFormData() {
        ProsecutionFormData prosecutionFormData = ProsecutionFormData.prosecutionFormData()
                .withProsecutionQuestions(ProsecutionQuestions.prosecutionQuestions()
                        .withFurtherProsecutionInformationProvidedAfterCertification(setPolarQuestion(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION))
                        .withFurtherInformationToAssistTheCourt(setPolarQuestion("furtherInformationToAssistTheCourt"))
                        .build())
                .withCertification(Certification.certification().withCertifyThatTheProsecutionIsTrialReady(setPolarQuestion(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY)).build())
                .build();
        return prosecutionFormData;
    }

    private PolarQuestion setPolarQuestion(final String furtherProsecutionInformationProvidedAfterCertification) {
        return PolarQuestion.polarQuestion()
                .withDetails(furtherProsecutionInformationProvidedAfterCertification).build();
    }

    @Test
    public void shouldTestArchiveCotr() {
        final CotrArchived cotrArchived = CotrArchived.cotrArchived().withCotrId(randomUUID()).build();
        when(cotrArchivedEnvelope.payload()).thenReturn(cotrArchived);

        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setId(cotrArchived.getCotrId());
        cotrDetailsEntity.setArchived(false);
        cotrDetailsEntity.setHearingId(randomUUID());

        when(cotrDetailsRepository.findBy(cotrArchived.getCotrId())).thenReturn(cotrDetailsEntity);
        cotrEventsListener.archiveCotr(cotrArchivedEnvelope);

        verify(cotrDetailsRepository).findBy(cotrArchived.getCotrId());
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertThat(savedCOTRDetailsEntity.getArchived(), is(true));
    }

    @Test
    public void shouldTestServeDefendantCotr() {
        final DefendantCotrServed defendantCotrServed = DefendantCotrServed.defendantCotrServed()
                .withCotrId(randomUUID())
                .withDefendantId(randomUUID())
                .withDefendantFormData("formData")
                .build();
        final COTRDefendantEntity cotrDefendantEntity = new COTRDefendantEntity();
        cotrDefendantEntity.setId(defendantCotrServed.getCotrId());
        cotrDefendantEntity.setDefendantId(defendantCotrServed.getDefendantId());
        when(serveDefendentCotrEnvelope.metadata()).thenReturn(metadata);
        when(serveDefendentCotrEnvelope.metadata().userId()).thenReturn(Optional.of(randomUUID().toString()));
        when(serveDefendentCotrEnvelope.payload()).thenReturn(defendantCotrServed);
        when(cotrDefendantRepository.findByCotrIdAndDefendantId(defendantCotrServed.getCotrId(), defendantCotrServed.getDefendantId())).thenReturn(Arrays.asList(cotrDefendantEntity));

        cotrEventsListener.serveDefendantCotr(serveDefendentCotrEnvelope);
        verify(cotrDefendantRepository).findByCotrIdAndDefendantId(defendantCotrServed.getCotrId(), defendantCotrServed.getDefendantId());
        verify(cotrDefendantRepository, times(1)).save(cotrDefendantEntityArgumentCaptor.capture());

        final COTRDefendantEntity savedCotrDefendantEntity = cotrDefendantEntityArgumentCaptor.getValue();
        assertThat(savedCotrDefendantEntity.getId(), is(cotrDefendantEntity.getId()));
        assertThat(savedCotrDefendantEntity.getDefendantId(), is(cotrDefendantEntity.getDefendantId()));
        assertThat(savedCotrDefendantEntity.getDefendantForm(), is("formData"));
        assertNotNull(savedCotrDefendantEntity.getServedBy());
        assertNotNull(savedCotrDefendantEntity.getServedOn());
    }

    @Test
    public void shouldAddDefendantToCotr() {
        final UUID defendantId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();

        final DefendantAddedToCotr defendantAddedToCotr = DefendantAddedToCotr.defendantAddedToCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withDefendantId(defendantId)
                .withDefendantNumber(2)
                .build();

        when(defendantAddedToCotrEnvelope.payload()).thenReturn(defendantAddedToCotr);

        cotrEventsListener.handleDefendantAddedToCotrEvent(defendantAddedToCotrEnvelope);
        verify(cotrDefendantRepository).save(cotrDefendantEntityArgumentCaptor.capture());

        final COTRDefendantEntity savedCOTRDefendantEntity = cotrDefendantEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDefendantEntity.getCotrId(), is(cotrId));
        assertThat(savedCOTRDefendantEntity.getDefendantId(), is(defendantId));
        assertThat(savedCOTRDefendantEntity.getdNumber(), is(2));

    }

    @Test
    public void shouldRemoveDefendantFromCotr() {
        final UUID defendantId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();

        final DefendantRemovedFromCotr defendantRemovedFromCotr = DefendantRemovedFromCotr.defendantRemovedFromCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withDefendantId(defendantId)
                .build();

        final COTRDefendantEntity defendantEntity = new COTRDefendantEntity();
        defendantEntity.setId(randomUUID());
        defendantEntity.setCotrId(cotrId);
        defendantEntity.setDefendantId(defendantId);

        when(defendantRemovedFromCotrEnvelope.payload()).thenReturn(defendantRemovedFromCotr);
        when(cotrDefendantRepository.findByCotrIdAndDefendantId(cotrId, defendantId)).thenReturn(Arrays.asList(defendantEntity));

        cotrEventsListener.handleDefendantRemovedFromCotrEvent(defendantRemovedFromCotrEnvelope);
        verify(cotrDefendantRepository).findByCotrIdAndDefendantId(cotrId, defendantId);
        verify(cotrDefendantRepository).remove(cotrDefendantEntityArgumentCaptor.capture());

        final COTRDefendantEntity removedCOTRDefendantEntity = cotrDefendantEntityArgumentCaptor.getValue();
        assertThat(removedCOTRDefendantEntity.getDefendantId(), is(defendantId));

    }


    @Test
    public void shouldHandleFurtherInfoForProsecutionCotrAdded() {
        final UUID cotrId = randomUUID();
        final UUID userId = randomUUID();
        final String message = "Prosecution Cotr Further Info";

        final FurtherInfoForProsecutionCotrAdded furtherInfoForProsecutionCotrAdded = FurtherInfoForProsecutionCotrAdded.furtherInfoForProsecutionCotrAdded()
                .withCotrId(cotrId)
                .withMessage(message)
                .withAddedByName("erica")
                .withAddedBy(userId)
                .withIsCertificationReady(Boolean.TRUE)
                .build();

        when(furtherInfoForProsecutionCotrAddedEnvelope.payload()).thenReturn(furtherInfoForProsecutionCotrAdded);

        cotrEventsListener.handleFurtherInfoForProsecutionCotrAdded(furtherInfoForProsecutionCotrAddedEnvelope);
        verify(cotrProsecutionFurtherInfoRepository).save(cotrProsecutionFurtherInfoEntityArgumentCaptor.capture());

        final COTRProsecutionFurtherInfoEntity cotrProsecutionFurtherInfoEntity = cotrProsecutionFurtherInfoEntityArgumentCaptor.getValue();
        assertThat(cotrProsecutionFurtherInfoEntity.getCotrId(), is(cotrId));
        assertThat(cotrProsecutionFurtherInfoEntity.getInfoAddedBy(), is(userId));
        assertThat(cotrProsecutionFurtherInfoEntity.getFurtherInformation(), is(message));
        assertThat(cotrProsecutionFurtherInfoEntity.getAddedOn(), is(notNullValue()));
        assertThat(cotrProsecutionFurtherInfoEntity.getId(), is(notNullValue()));

    }

    @Test
    public void shouldHandleFurtherInfoForDefenceCotrAdded() {
        final UUID cotrId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID userId = randomUUID();
        final String userName = "James Turner";
        final String message = "Defence Cotr Further Info";

        final FurtherInfoForDefenceCotrAdded furtherInfoForDefenceCotrAdded = FurtherInfoForDefenceCotrAdded.furtherInfoForDefenceCotrAdded()
                .withCotrId(cotrId)
                .withDefendantId(defendantId)
                .withMessage(message)
                .withIsCertificationReady(Boolean.FALSE)
                .withAddedBy(userId)
                .withAddedByName(userName)
                .build();

        when(furtherInfoForDefenceCotrAddedEnvelope.payload()).thenReturn(furtherInfoForDefenceCotrAdded);

        cotrEventsListener.handleFurtherInfoForDefenceCotrAdded(furtherInfoForDefenceCotrAddedEnvelope);
        verify(cotrDefenceFurtherInfoRepository).save(cotrDefenceFurtherInfoEntityArgumentCaptor.capture());

        final COTRDefenceFurtherInfoEntity cotrDefenceFurtherInfoEntity = cotrDefenceFurtherInfoEntityArgumentCaptor.getValue();
        assertThat(cotrDefenceFurtherInfoEntity.getCotrDefendantId(), is(defendantId));
        assertThat(cotrDefenceFurtherInfoEntity.getFurtherInformation(), is(message));
        assertThat(cotrDefenceFurtherInfoEntity.getIsCertificationReady(), is(Boolean.FALSE));
        assertThat(cotrDefenceFurtherInfoEntity.getInfoAddedBy(), is(userId));
        assertThat(cotrDefenceFurtherInfoEntity.getInfoAddedByName(), is(userName));
        assertThat(cotrDefenceFurtherInfoEntity.getAddedOn(), is(notNullValue()));
        assertThat(cotrDefenceFurtherInfoEntity.getId(), is(notNullValue()));
    }

    @Test
    public void shouldHandleCaseProgressionReviewNotesUpdated() {
        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        final String comments1 = "Value 1";
        final String comments2 = "Value 2";

        final ReviewNotesUpdated reviewNotesUpdated = ReviewNotesUpdated.reviewNotesUpdated()
                .withCotrId(cotrId)
                .withCotrNotes(Arrays.asList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(Arrays.asList(
                                ReviewNotes.reviewNotes()
                                        .withId(id1)
                                        .withComment(comments1)
                                        .build(),
                                ReviewNotes.reviewNotes()
                                        .withId(id2)
                                        .withComment(comments2)
                                        .build()))
                        .build()))
                .build();

        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setId(cotrId);
        cotrDetailsEntity.setHearingId(hearingId);
        cotrDetailsEntity.setProsecutionCaseId(prosecutionCaseId);

        final JsonArrayBuilder reviewNotesArrayBuilder = createArrayBuilder();
        reviewNotesArrayBuilder.add(createObjectBuilder()
                .add("id", id1.toString())
                .add("comment", comments1)
                .build());
        reviewNotesArrayBuilder.add(createObjectBuilder()
                .add("id", id2.toString())
                .add("comment", comments2)
                .build());
        final String reviewNotes = reviewNotesArrayBuilder.build().toString();
        when(reviewNotesUpdatedEnvelope.payload()).thenReturn(reviewNotesUpdated);
        when(cotrDetailsRepository.findBy(cotrId)).thenReturn(cotrDetailsEntity);

        cotrEventsListener.handleReviewNotesUpdated(reviewNotesUpdatedEnvelope);

        verify(cotrDetailsRepository).findBy(cotrId);
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertThat(savedCOTRDetailsEntity.getProsecutionCaseId(), is(cotrDetailsEntity.getProsecutionCaseId()));
        assertThat(savedCOTRDetailsEntity.getCaseProgressionReviewNote(), is(reviewNotes));
    }

    @Test
    public void shouldHandleListingReviewNotesUpdated() {
        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        final String comments1 = "Value 1";
        final String comments2 = "Value 2";

        final ReviewNotesUpdated reviewNotesUpdated = ReviewNotesUpdated.reviewNotesUpdated()
                .withCotrId(cotrId)
                .withCotrNotes(Arrays.asList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(Arrays.asList(
                                ReviewNotes.reviewNotes()
                                        .withId(id1)
                                        .withComment(comments1)
                                        .build(),
                                ReviewNotes.reviewNotes()
                                        .withId(id2)
                                        .withComment(comments2)
                                        .build()))
                        .build(),
                        CotrNotes.cotrNotes()
                                .withReviewNoteType(ReviewNoteType.LISTING)
                                .withReviewNotes(Arrays.asList(
                                        ReviewNotes.reviewNotes()
                                                .withId(id1)
                                                .withComment(comments1)
                                                .build(),
                                        ReviewNotes.reviewNotes()
                                                .withId(id2)
                                                .withComment(comments2)
                                                .build()))
                                .build()))
                .build();

        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setId(cotrId);
        cotrDetailsEntity.setHearingId(hearingId);
        cotrDetailsEntity.setProsecutionCaseId(prosecutionCaseId);

        final JsonArrayBuilder reviewNotesArrayBuilder = createArrayBuilder();
        reviewNotesArrayBuilder.add(createObjectBuilder()
                .add("id", id1.toString())
                .add("comment", comments1)
                .build());
        reviewNotesArrayBuilder.add(createObjectBuilder()
                .add("id", id2.toString())
                .add("comment", comments2)
                .build());
        final String reviewNotes = reviewNotesArrayBuilder.build().toString();
        when(reviewNotesUpdatedEnvelope.payload()).thenReturn(reviewNotesUpdated);
        when(cotrDetailsRepository.findBy(cotrId)).thenReturn(cotrDetailsEntity);

        cotrEventsListener.handleReviewNotesUpdated(reviewNotesUpdatedEnvelope);

        verify(cotrDetailsRepository).findBy(cotrId);
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertThat(savedCOTRDetailsEntity.getProsecutionCaseId(), is(cotrDetailsEntity.getProsecutionCaseId()));
        assertThat(savedCOTRDetailsEntity.getListingReviewNotes(), is(reviewNotes));
    }

    @Test
    public void shouldHandleJudgeReviewNotesUpdated() {
        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        final String comments1 = "Value 1";
        final String comments2 = "Value 2";

        final ReviewNotesUpdated reviewNotesUpdated = ReviewNotesUpdated.reviewNotesUpdated()
                .withCotrId(cotrId)
                .withCotrNotes(Arrays.asList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(Arrays.asList(
                                ReviewNotes.reviewNotes()
                                        .withId(id1)
                                        .withComment(comments1)
                                        .build(),
                                ReviewNotes.reviewNotes()
                                        .withId(id2)
                                        .withComment(comments2)
                                        .build()))
                        .build(),
                        CotrNotes.cotrNotes()
                                .withReviewNoteType(ReviewNoteType.JUDGE)
                                .withReviewNotes(Arrays.asList(
                                        ReviewNotes.reviewNotes()
                                                .withId(id1)
                                                .withComment(comments1)
                                                .build(),
                                        ReviewNotes.reviewNotes()
                                                .withId(id2)
                                                .withComment(comments2)
                                                .build()))
                                .build()))
                .build();

        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setId(cotrId);
        cotrDetailsEntity.setHearingId(hearingId);
        cotrDetailsEntity.setProsecutionCaseId(prosecutionCaseId);

        final JsonArrayBuilder reviewNotesArrayBuilder = createArrayBuilder();
        reviewNotesArrayBuilder.add(createObjectBuilder()
                .add("id", id1.toString())
                .add("comment", comments1)
                .build());
        reviewNotesArrayBuilder.add(createObjectBuilder()
                .add("id", id2.toString())
                .add("comment", comments2)
                .build());
        final String reviewNotes = reviewNotesArrayBuilder.build().toString();
        when(reviewNotesUpdatedEnvelope.payload()).thenReturn(reviewNotesUpdated);
        when(cotrDetailsRepository.findBy(cotrId)).thenReturn(cotrDetailsEntity);

        cotrEventsListener.handleReviewNotesUpdated(reviewNotesUpdatedEnvelope);

        verify(cotrDetailsRepository).findBy(cotrId);
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertThat(savedCOTRDetailsEntity.getId(), is(cotrDetailsEntity.getId()));
        assertThat(savedCOTRDetailsEntity.getHearingId(), is(cotrDetailsEntity.getHearingId()));
        assertThat(savedCOTRDetailsEntity.getProsecutionCaseId(), is(cotrDetailsEntity.getProsecutionCaseId()));
        assertThat(savedCOTRDetailsEntity.getJudgeReviewNotes(), is(reviewNotes));
    }

    @Test
    public void shouldTestUpdateProsecutionCotr_FormCompletedOnBehalfOfTheProsecutionBy() {
        final ProsecutionCotrUpdated prosecutionCotrUpdated = ProsecutionCotrUpdated.prosecutionCotrUpdated()
                .withCotrId(randomUUID())
                .withHearingId(randomUUID())
                .withCertifyThatTheProsecutionIsTrialReady(PolarQuestion.polarQuestion()
                        .withAnswer("Y")
                        .build())
                .withFormCompletedOnBehalfOfProsecutionBy(setPolarQuestion("Test"))
                .build();
        ProsecutionFormData prosecutionFormData = getProsecutionFormData();
        final COTRDetailsEntity cotrDetailsEntity = new COTRDetailsEntity();
        cotrDetailsEntity.setProsecutionFormData(prosecutionFormData.toString());
        cotrDetailsEntity.setId(prosecutionCotrUpdated.getCotrId());
        cotrDetailsEntity.setHearingId(prosecutionCotrUpdated.getHearingId());
        final JsonObject jsonObject = getJsonObject();
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionFormData.class)).thenReturn(getProsecutionFormData());
        cotrDetailsEntity.setProsecutionFormData(getProsecutionFormData().toString());
        when(prosecutionCotrUpdatedEnvelope.payload()).thenReturn(prosecutionCotrUpdated);
        when(cotrDetailsRepository.findBy(prosecutionCotrUpdated.getCotrId())).thenReturn(cotrDetailsEntity);

        cotrEventsListener.updateProsecutionCotr(prosecutionCotrUpdatedEnvelope);
        verify(cotrDetailsRepository).findBy(prosecutionCotrUpdated.getCotrId());
        verify(cotrDetailsRepository).save(cotrDetailsEntityArgumentCaptor.capture());

        final COTRDetailsEntity savedCOTRDetailsEntity = cotrDetailsEntityArgumentCaptor.getValue();
        assertFalse(savedCOTRDetailsEntity.getProsecutionFormData().contains("formCompletedOnBehalfOfTheProsecutionBy"));
    }
}