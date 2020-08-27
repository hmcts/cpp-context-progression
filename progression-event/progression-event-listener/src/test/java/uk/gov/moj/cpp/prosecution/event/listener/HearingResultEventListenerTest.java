package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.READY_FOR_REVIEW;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.event.listener.HearingResultEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    CaseDefendantHearingRepository caseDefendantHearingRepository;

    @InjectMocks
    private HearingResultEventListener hearingEventEventListener;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;


    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void hearingResult() {

        final UUID hearingId = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProceedingsConcluded(true)
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(offenceId2)
                        .build()))
                .build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId)
                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                .withDefendants(defendants)
                                .build()))
                        .build())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                        .withDefendants(defendants)
                        .build()))
                .withHasSharedResults(true)
                .build();

        final Hearing hearing2 = Hearing.hearing()
                .withId(hearingId2)
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                        .withDefendants(defendants)
                        .build()))
                .withHasSharedResults(false)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        HearingEntity hearingEntity2 = new HearingEntity();
        hearingEntity2.setHearingId(hearingId2);
        hearingEntity2.setPayload(objectToJsonObjectConverter.convert(hearing2).toString());
        hearingEntity2.setListingStatus(HearingListingStatus.SENT_FOR_LISTING);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setHearing(hearingEntity);
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setHearing(hearingEntity2);
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId2));

        final CaseDefendantHearingEntity caseDefendantHearingEntity3 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity3.setHearing(hearingEntity);
        caseDefendantHearingEntity3.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId2, hearingId));

        final CaseDefendantHearingEntity caseDefendantHearingEntity4 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity4.setHearing(hearingEntity2);
        caseDefendantHearingEntity4.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId2, hearingId2));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity3);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity4);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(3)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity1 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(hearingId))
                .findFirst().get();


        final Hearing savedHearing1 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity1.getPayload()), Hearing.class);

        assertThat(savedHearing1, notNullValue());
        assertThat(savedHearing1.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getProceedingsConcluded(), nullValue());
        assertThat(savedHearingEntity1.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        HearingEntity savedHearingEntity2 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(hearingId2))
                .findFirst().get();


        final Hearing savedHearing2 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity2.getPayload()), Hearing.class);

        assertThat(savedHearing2, notNullValue());
        assertThat(savedHearing2.getProsecutionCases().get(0).getCaseStatus(), is(CaseStatusEnum.INACTIVE.getDescription()));
        assertThat(savedHearing2.getProsecutionCases().get(0).getDefendants().get(0).getProceedingsConcluded(), is(true));
        assertThat(savedHearingEntity2.getListingStatus(), equalTo(HearingListingStatus.SENT_FOR_LISTING));

    }

    @Test
    public void shouldUpdateHearingResultForCourtApplication() {
        final UUID hearingId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .withId(randomUUID())
                .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                .withLinkedCaseId(randomUUID())
                .withOrderingCourt(CourtCentre.courtCentre().build())
                .withRespondents(singletonList(CourtApplicationRespondent.courtApplicationRespondent().build()))
                .build();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withHasSharedResults(true)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .withCourtApplications(singletonList(courtApplication))
                .build();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        final List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();
        final HearingEntity savedHearingEntity1 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(hearingId))
                .findFirst().get();

        final Hearing savedHearing1 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity1.getPayload()), Hearing.class);

        assertThat(savedHearing1, notNullValue());
        assertThat(savedHearing1.getId(), is(hearingId));
        assertThat(savedHearing1.getCourtApplications(), notNullValue());
        assertThat(savedHearing1.getCourtApplications().get(0).getApplicationStatus(), is(ApplicationStatus.IN_PROGRESS));
    }

    @Test
    public void shouldUpdateHearingResultForProsecutionCases() {
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final ProsecutionCase prosecutionCase = getProsecutionCase(prosecutionCaseId);

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withHasSharedResults(true)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .withProsecutionCases(singletonList(prosecutionCase))
                .build();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        final List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();
        final HearingEntity savedHearingEntity1 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(hearingId))
                .findFirst().get();

        final Hearing savedHearing1 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity1.getPayload()), Hearing.class);

        assertThat(savedHearing1, notNullValue());
        assertThat(savedHearing1.getId(), is(hearingId));
        assertThat(savedHearing1.getProsecutionCases(), notNullValue());
        assertThat(savedHearing1.getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceDateCode(), is(4));
    }

    @Test
    public void shouldUpdateHearingResultForCourtApplicationsAndProsecutionCases() {
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final List<CourtApplication> courtApplications = getCourtApplications(courtApplicationId);
        final ProsecutionCase prosecutionCase = getProsecutionCase(prosecutionCaseId);
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withHasSharedResults(true)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .withCourtApplications(courtApplications)
                .withProsecutionCases(singletonList(prosecutionCase))
                .build();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        final List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();
        final HearingEntity savedHearingEntity1 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(hearingId))
                .findFirst().get();

        final Hearing savedHearing1 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity1.getPayload()), Hearing.class);

        assertThat(savedHearing1, notNullValue());
        assertThat(savedHearing1.getId(), is(hearingId));
        assertThat(savedHearing1.getCourtApplications(), notNullValue());
        assertThat(savedHearing1.getCourtApplications().get(0).getId(), is(courtApplicationId));
        assertThat(savedHearing1.getCourtApplications().get(0).getJudicialResults().size(), is(1));
        assertThat(savedHearing1.getCourtApplications().get(0).getJudicialResults().get(0).getLabel(), is("PublishedForNowsFALSE"));

        assertThat(savedHearing1.getProsecutionCases(), notNullValue());
        assertThat(savedHearing1.getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getDefendantCaseJudicialResults().size(), is(1));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getDefendantCaseJudicialResults().get(0).getLabel(),
                is("PublishedForNowsFALSE"));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults().size(), is(1));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults().get(0).getLabel(),
                is("PublishedForNowsFALSE"));
    }

    private List<CourtApplication> getCourtApplications(final UUID courtApplicationId) {
        return singletonList(CourtApplication.courtApplication()
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .withId(courtApplicationId)
                .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                .withLinkedCaseId(randomUUID())
                .withOrderingCourt(CourtCentre.courtCentre().build())
                .withRespondents(singletonList(CourtApplicationRespondent.courtApplicationRespondent().build()))
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult().withLabel("PublishedForNowsTRUE").withPublishedForNows(Boolean.TRUE).build(),
                        JudicialResult.judicialResult().withLabel("PublishedForNowsFALSE").withPublishedForNows(Boolean.FALSE).build()))
                .build());
    }

    private ProsecutionCase getProsecutionCase(final UUID prosecutionCaseId) {
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withDefendantCaseJudicialResults(Arrays.asList(JudicialResult.judicialResult().withLabel("PublishedForNowsTRUE").withPublishedForNows(Boolean.TRUE).build(),
                        JudicialResult.judicialResult().withLabel("PublishedForNowsFALSE").withPublishedForNows(Boolean.FALSE).build()))
                .withOffences(singletonList(
                        Offence.offence()
                        .withId(randomUUID())
                        .withJudicialResults(
                                Arrays.asList(JudicialResult.judicialResult()
                                        .withLabel("PublishedForNowsTRUE")
                                        .withPublishedForNows(Boolean.TRUE)
                                        .build(),
                                JudicialResult.judicialResult()
                                        .withLabel("PublishedForNowsFALSE")
                                        .withPublishedForNows(Boolean.FALSE)
                                        .build())
                        )
                        .withOffenceDateCode(4)
                        .build()))
                .build();
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(singletonList(defendant))
                .build();
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }

}
