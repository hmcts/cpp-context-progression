package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtOrderOffence.courtOrderOffence;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.READY_FOR_REVIEW;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.GRANTED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.PENDING;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
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

    private static final String CASE_RESULT_LABEL_1 = "case result label 1";
    private static final String OFFENCE_RESULT_LABEL_1 = "offence result label 1";
    private static final String CASE_RESULT_LABEL_2 = "case result label 2";
    private static final String OFFENCE_RESULT_LABEL_2 = "offence result label 2";
    private static final String DEFENDANT_RESULT_LABEL = "defendant result label";
    private static final String OFFENCE_RESULT_LABEL_3 = "offence result label 3";
    public static final String FOR_NOWS = "for Nows";
    public static final String FOR_NOT_NOWS = "for Not Nows";
    private static final String GRANTED_ONE_ADVOCATE = "Granted (One Advocate)";
    private static final String LAA_Reference = "LAA1234";
    private static final String LAA_APPLICATION_REFERENCE = "ABC123";
    private static final String LAA_APPL_GR_Status_Code = "GR";

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

        final UUID firstHearingId = randomUUID();
        final UUID secondHearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId)
                .withDefendantCaseJudicialResults(newArrayList(getJudicialResult(randomUUID(), CASE_RESULT_LABEL_1)))
                .withProceedingsConcluded(true)
                .withOffences(asList(offence()
                        .withId(offenceId)
                        .withJudicialResults(newArrayList(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1)))
                        .build()))
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withDefendantCaseJudicialResults(newArrayList(getJudicialResult(randomUUID(), CASE_RESULT_LABEL_2)))
                .withProceedingsConcluded(true)
                .withOffences(asList(offence()
                        .withId(offenceId2)
                        .withJudicialResults(newArrayList(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2)))
                        .build()))
                .build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(firstHearingId)
                        .withDefendantJudicialResults(newArrayList(DefendantJudicialResult.defendantJudicialResult().withJudicialResult(getJudicialResult(randomUUID(), DEFENDANT_RESULT_LABEL)).build()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId)
                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                .withDefendants(defendants)
                                .build()))
                        .build())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(firstHearingId)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                        .withDefendants(defendants)
                        .build()))
                .withHasSharedResults(true)
                .build();

        final Hearing hearing2 = Hearing.hearing()
                .withId(secondHearingId)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                        .withDefendants(defendants)
                        .withCpsOrganisationId(randomUUID())
                        .build()))
                .withHasSharedResults(false)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(firstHearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        HearingEntity hearingEntity2 = new HearingEntity();
        hearingEntity2.setHearingId(secondHearingId);
        hearingEntity2.setPayload(objectToJsonObjectConverter.convert(hearing2).toString());
        hearingEntity2.setListingStatus(HearingListingStatus.SENT_FOR_LISTING);

        when(hearingRepository.findBy(firstHearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setHearing(hearingEntity);
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, firstHearingId));

        final CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setHearing(hearingEntity2);
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, secondHearingId));

        final CaseDefendantHearingEntity caseDefendantHearingEntity3 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity3.setHearing(hearingEntity);
        caseDefendantHearingEntity3.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId2, firstHearingId));

        final CaseDefendantHearingEntity caseDefendantHearingEntity4 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity4.setHearing(hearingEntity2);
        caseDefendantHearingEntity4.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId2, secondHearingId));

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

        HearingEntity savedHearingEntity1 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(firstHearingId))
                .findFirst().get();


        final Hearing savedHearing1 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity1.getPayload()), Hearing.class);

        assertThat(savedHearing1, notNullValue());
        assertThat(savedHearing1.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity1.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant firstDefendant = savedHearing1.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(firstDefendant.getProceedingsConcluded(), nullValue());
        assertThat(firstDefendant.getOffences().get(0).getJudicialResults(), hasSize(1));
        assertThat(firstDefendant.getOffences().get(0).getJudicialResults().get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(firstDefendant.getDefendantCaseJudicialResults(), hasSize(1));
        assertThat(firstDefendant.getDefendantCaseJudicialResults().get(0).getLabel(), is(CASE_RESULT_LABEL_1));

        final Defendant secondDefendant = savedHearing1.getProsecutionCases().get(0).getDefendants().get(1);
        assertThat(secondDefendant.getOffences().get(0).getJudicialResults(), hasSize(1));
        assertThat(secondDefendant.getOffences().get(0).getJudicialResults().get(0).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(secondDefendant.getDefendantCaseJudicialResults(), hasSize(1));
        assertThat(secondDefendant.getDefendantCaseJudicialResults().get(0).getLabel(), is(CASE_RESULT_LABEL_2));
        assertThat(secondDefendant.getDefendantCaseJudicialResults(), hasSize(1));

        assertThat(savedHearing1.getDefendantJudicialResults(), hasSize(1));
        assertThat(savedHearing1.getDefendantJudicialResults().get(0).getJudicialResult().getLabel(), is(DEFENDANT_RESULT_LABEL));

        HearingEntity savedHearingEntity2 = savedHearingEntities.stream().filter(savedHearingEntity -> savedHearingEntity.getHearingId().equals(secondHearingId))
                .findFirst().get();

        final Hearing savedHearing2 = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity2.getPayload()), Hearing.class);

        assertThat(savedHearing2, notNullValue());
        assertThat(savedHearing2.getProsecutionCases().get(0).getCaseStatus(), is(CaseStatusEnum.INACTIVE.getDescription()));
        assertThat(savedHearing2.getProsecutionCases().get(0).getDefendants().get(0).getProceedingsConcluded(), is(true));
        assertThat(savedHearing2.getProsecutionCases().get(0).getCpsOrganisationId(), is(hearing2.getProsecutionCases().get(0).getCpsOrganisationId()));
        assertThat(savedHearingEntity2.getListingStatus(), equalTo(HearingListingStatus.SENT_FOR_LISTING));


    }

    @Test
    public void shouldUpdateHearingResultForCourtApplication() {
        final UUID hearingId = randomUUID();
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final JudicialResult judicialResult = JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withPublishedForNows(false)
                .withResultText("Sample")
                .build();
        judicialResults.add(judicialResult);
        judicialResults.add(judicialResult);
        judicialResults.add(judicialResult);
        judicialResults.add(judicialResult);

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .withId(randomUUID())
                .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                .withCourtApplicationCases(
                        singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                        .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withCourtOrderOffences(singletonList(courtOrderOffence().withOffence(offence().build()).build()))
                        .build())
                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty().build()))
                .withJudicialResults(judicialResults)
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
        assertThat(savedHearing1.getCourtApplications().get(0).getJudicialResults().size(), is(1));
    }

    @Test
    public void shouldUpdateHearingResultForCourtApplicationWithForNowsOnlyResults() {
        final UUID hearingId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .withId(randomUUID())
                .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                .withCourtApplicationCases(
                        singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID())
                                .withOffences(singletonList(offence()
                                        .withJudicialResults(asList(JudicialResult.judicialResult()
                                                .withResultText(FOR_NOWS)
                                                .withPublishedForNows(true)
                                                .build(), JudicialResult.judicialResult()
                                                .withResultText(FOR_NOT_NOWS)
                                                .build()))
                                        .build()))
                                .build()))
                .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                        .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withCourtOrderOffences(singletonList(courtOrderOffence().withOffence(offence()
                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                        .withPublishedForNows(true)
                                        .withResultText(FOR_NOWS)
                                        .build(), JudicialResult.judicialResult()
                                        .withResultText(FOR_NOT_NOWS)
                                        .build()))
                                .build())
                                .build()))
                        .build())
                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty().build()))
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withPublishedForNows(true)
                        .withResultText(FOR_NOWS)
                        .build(), JudicialResult.judicialResult()
                        .withResultText(FOR_NOT_NOWS)
                        .build()))
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
        assertThat(savedHearing1.getCourtApplications().get(0).getJudicialResults().size(), is(1));
        assertThat(savedHearing1.getCourtApplications().get(0).getJudicialResults().get(0).getResultText(), is(FOR_NOT_NOWS));
        assertThat(savedHearing1.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getJudicialResults().size(), is(1));
        assertThat(savedHearing1.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getJudicialResults().get(0).getResultText(), is(FOR_NOT_NOWS));
        assertThat(savedHearing1.getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getJudicialResults().size(), is(1));
        assertThat(savedHearing1.getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getJudicialResults().get(0).getResultText(), is(FOR_NOT_NOWS));
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
        assertThat(savedHearing1.getProsecutionCases().get(0).getCpsOrganisation(), is("A01"));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceDateCode(), is(4));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions().get(0).getLabel(), is("ReportingRestrictionLabel"));
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
        assertThat(savedHearing1.getProsecutionCases().get(0).getCpsOrganisation(), is("A01"));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getDefendantCaseJudicialResults().size(), is(1));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getDefendantCaseJudicialResults().get(0).getLabel(),
                is("PublishedForNowsFALSE"));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults().size(), is(1));
        assertThat(savedHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults().get(0).getLabel(),
                is("PublishedForNowsFALSE"));

    }

    @Test
    public void shouldAddJudicialResultsToOffenceWhenOffenceHasNotBeenResultedBefore() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantWithResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResultWithAmendment(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay),
                                        getJudicialResultWithAmendment(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay)
                                )
                        )
                        .build()))
                .build();
        final Defendant defendantWithoutResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantWithResults))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantWithoutResults))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), nullValue());

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, hasSize(2));
        assertThat(judicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(judicialResults.get(0).getOrderedDate(), is(hearingDay));
        assertThat(judicialResults.get(1).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(judicialResults.get(1).getOrderedDate(), is(hearingDay));

    }

    @Test
    public void shouldUpdateOffenceWithLatestJudicialResultsWhenOffenceHaveBeenResharedOnTheSameDay() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResultWithAmendment(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay)
                                )
                        )
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay)
                                )
                        )
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), nullValue());

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, hasSize(1));
        assertThat(judicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(judicialResults.get(0).getOrderedDate(), is(hearingDay));

    }

    @Test
    public void shouldRemoveJudicialResultsFromOffenceWhenOffenceResultsHaveBeenRemovedOnReshareForTheSameDay() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay),
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay)
                                )
                        )
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), nullValue());

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, nullValue());

    }

    @Test
    public void shouldUpdateJudicialResultsOnlyForTheHearingDayAndShouldNotUpdateJudicialResultsForNonSharingDaysOnReshare() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResultWithAmendment(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay)
                                )
                        )
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay),
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_3, hearingDay.plusDays(1))
                                )
                        )
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), nullValue());

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, hasSize(2));
        assertThat(judicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_3));
        assertThat(judicialResults.get(0).getOrderedDate(), is(hearingDay.plusDays(1)));
        assertThat(judicialResults.get(1).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(judicialResults.get(1).getOrderedDate(), is(hearingDay));

    }

    @Test
    public void shouldRemoveJudicialResultsOnlyForTheHearingDayAndShouldNotRemoveJudicialResultsForNonSharingDaysOnReshare() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay),
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay.minusDays(1)),
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_3, hearingDay.plusDays(1))
                                )
                        )
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), nullValue());

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, hasSize(2));
        assertThat(judicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(judicialResults.get(0).getOrderedDate(), is(hearingDay.minusDays(1)));
        assertThat(judicialResults.get(1).getLabel(), is(OFFENCE_RESULT_LABEL_3));
        assertThat(judicialResults.get(1).getOrderedDate(), is(hearingDay.plusDays(1)));

    }

    @Test
    public void shouldReturnDefendantFromPayloadWhenSameDefendantIsNotAvailableInTheViewStoreOnReshare() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay)
                                )
                        )
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(emptyList())
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), is(true));

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, hasSize(1));
        assertThat(judicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));

    }

    @Test
    public void shouldAddDefendantJudicialResultWhenDefendantJudicialResultHasNotBeenSharedBefore() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantWithResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantWithoutResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                                .withMasterDefendantId(defendantId)
                                .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                                .build()))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantWithResults))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantWithoutResults))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(savedHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getMasterDefendantId(), is(defendantId));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));

    }

    @Test
    public void shouldNotUpdateDefendantLegalAidStatusOnAddingAndSharingDefendantJudicialResult() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantWithResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantWithoutResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withLegalAidStatus(GRANTED.getDescription())
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                                .withMasterDefendantId(defendantId)
                                .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                                .build()))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantWithResults))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantWithoutResults))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(savedHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getMasterDefendantId(), is(defendantId));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));

        final Optional<Defendant> defendant = savedHearing.getProsecutionCases().get(0).getDefendants().stream().filter(def -> def.getId().equals(defendantId)).findFirst();
        assertThat(defendant.isPresent(), CoreMatchers.is(true));
        assertThat(defendant.get().getLegalAidStatus(), equalTo(LegalAidStatusEnum.GRANTED.getDescription()));
        assertThat(defendant.get().getOffences().size(), CoreMatchers.is(1));
    }

    @Test
    public void shouldNotUpdateOffencesLegalAidStatusOnAddingAndSharingDefendantJudicialResult() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String statusDescription = GRANTED_ONE_ADVOCATE;
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantWithResults = Defendant.defendant()
                .withId(defendantId)
                .withLegalAidStatus(PENDING.getDescription())
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();

        final Defendant defendantWithoutResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withLegalAidStatus(GRANTED.getDescription())
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withLaaApplnReference(LaaReference
                                .laaReference()
                                .withApplicationReference(LAA_APPLICATION_REFERENCE)
                                .withStatusId(UUID.randomUUID())
                                .withStatusCode(LAA_APPL_GR_Status_Code)
                                .withOffenceLevelStatus(GRANTED.getDescription())
                                .withStatusDescription(statusDescription)
                                .withLaaContractNumber(LAA_Reference)
                                .withStatusDate(now())
                                .build())
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                                .withMasterDefendantId(defendantId)
                                .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                                .build()))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantWithResults))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantWithoutResults))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(savedHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getMasterDefendantId(), is(defendantId));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));

        final Optional<Defendant> defendant = savedHearing.getProsecutionCases().get(0).getDefendants().stream().filter(def -> def.getId().equals(defendantId)).findFirst();
        assertThat(defendant.isPresent(), CoreMatchers.is(true));
        assertThat(defendant.get().getLegalAidStatus(), equalTo(LegalAidStatusEnum.GRANTED.getDescription()));
        assertThat(defendant.get().getOffences().size(), CoreMatchers.is(1));

        LaaReference laaReference = defendant.get().getOffences().get(0).getLaaApplnReference();
        assertThat(laaReference, notNullValue());
        assertThat(laaReference.getStatusDescription(), is(statusDescription));
        assertThat(laaReference.getOffenceLevelStatus(), is(GRANTED.getDescription()));
        assertThat(laaReference.getApplicationReference(), is(LAA_APPLICATION_REFERENCE));
        assertThat(laaReference.getLaaContractNumber(), Is.is(LAA_Reference));
    }

    @Test
    public void shouldUpdateWithLatestDefendantJudicialResultOnReshareForTheSameDay() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                                .withMasterDefendantId(defendantId)
                                .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                                .build()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                        .withMasterDefendantId(defendantId)
                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay))
                        .build()))
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(savedHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getMasterDefendantId(), is(defendantId));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));

    }

    @Test
    public void shouldRemoveDefendantJudicialResultWhenDefendantJudicialResultHasBeenRemovedOnReshareForTheSameDay() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                        .withMasterDefendantId(defendantId)
                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                        .build()))
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(savedHearing.getDefendantJudicialResults(), nullValue());

    }

    @Test
    public void shouldRemoveDefendantJudicialResultWhenDefendantJudicialResultIsforNows() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withDefendantJudicialResults(singletonList(DefendantJudicialResult.defendantJudicialResult()
                                .withJudicialResult(JudicialResult.judicialResult().withPublishedForNows(true).build())
                                .build()))
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getDefendantJudicialResults(), nullValue());

    }

    @Test
    public void shouldUpdateDefendantJudicialResultOnlyForTheHearingDayAndShouldNotUpdateDefendantJudicialResultOnReshareForNonSharingDays() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay)
                                )
                        )
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay),
                                        getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_3, hearingDay.plusDays(1))
                                )
                        )
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                                .withMasterDefendantId(defendantId)
                                .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                                .build()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withDefendantJudicialResults(
                        asList(
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withMasterDefendantId(defendantId)
                                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay))
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withMasterDefendantId(defendantId)
                                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_3, hearingDay.plusDays(1)))
                                        .build()
                        )
                )
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final List<DefendantJudicialResult> judicialResults = savedHearing.getDefendantJudicialResults();
        assertThat(judicialResults, hasSize(2));
        assertThat(judicialResults.get(0).getMasterDefendantId(), is(defendantId));
        assertThat(judicialResults.get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_3));
        assertThat(judicialResults.get(1).getMasterDefendantId(), is(defendantId));
        assertThat(judicialResults.get(1).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));

    }

    @Test
    public void shouldRemoveDefendantJudicialResultOnlyForTheHearingDayAndShouldNotRemoveDefendantJudicialResultForNonSharingDaysOnReshare() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantForEvent = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantForDatabase = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearingDay(hearingDay)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantForEvent))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withDefendantJudicialResults(
                        asList(
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withMasterDefendantId(defendantId)
                                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay.minusDays(1)))
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withMasterDefendantId(defendantId)
                                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay))
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withMasterDefendantId(defendantId)
                                        .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_3, hearingDay.plusDays(1)))
                                        .build()
                        )
                )
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantForDatabase))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final List<DefendantJudicialResult> judicialResults = savedHearing.getDefendantJudicialResults();
        assertThat(judicialResults, hasSize(2));
        assertThat(judicialResults.get(0).getMasterDefendantId(), is(defendantId));
        assertThat(judicialResults.get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(judicialResults.get(1).getMasterDefendantId(), is(defendantId));
        assertThat(judicialResults.get(1).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_3));

    }

    /**
     * This test is for backward compatibility where the old HearingResulted events will not contain hearingDay property.
     * In this case we should retain the previous functionality. That is, the judicial results from view store hearing should reflect what are in event.
     * After multiday (per day results) sharing feature goes live HearingResulted event should contain hearingDay property.
     */
    @Test
    public void shouldAddJudicialResultsFromEventToOffenceWhenHearingDayIsNull() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantWithResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .withJudicialResults(
                                newArrayList(
                                        getJudicialResultWithAmendment(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay),
                                        getJudicialResultWithAmendment(randomUUID(), OFFENCE_RESULT_LABEL_2, hearingDay)
                                )
                        )
                        .build()))
                .build();
        final Defendant defendantWithoutResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantWithResults))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantWithoutResults))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

        final Defendant savedDefendant = savedHearing.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(savedDefendant.getProceedingsConcluded(), nullValue());

        final Offence savedOffence = savedDefendant.getOffences().get(0);
        assertThat(savedOffence.getWording(), is("offence wording"));

        final List<JudicialResult> judicialResults = savedOffence.getJudicialResults();
        assertThat(judicialResults, hasSize(2));
        assertThat(judicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(judicialResults.get(0).getOrderedDate(), is(hearingDay));
        assertThat(judicialResults.get(1).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(judicialResults.get(1).getOrderedDate(), is(hearingDay));

    }

    /**
     * This test is for backward compatibility where the old HearingResulted events will not contain hearingDay property.
     * In this case we should retain the previous functionality. That is, the judicial results from view store hearing should reflect what are in event.
     * After multiday (per day results) sharing feature goes live HearingResulted event should contain hearingDay property.
     */
    @Test
    public void shouldAddDefendantJudicialResultWhenHearingDayIsNull() {

        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2021, 04, 05);
        final Defendant defendantWithResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .withWording("offence wording")
                        .build()))
                .build();
        final Defendant defendantWithoutResults = Defendant.defendant()
                .withId(defendantId)
                .withProceedingsConcluded(true)
                .withOffences(asList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                                .withMasterDefendantId(defendantId)
                                .withJudicialResult(getJudicialResult(randomUUID(), OFFENCE_RESULT_LABEL_1, hearingDay))
                                .build()))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withHasSharedResults(true)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(
                                asList(
                                        ProsecutionCase.prosecutionCase()
                                                .withId(prosecutionCaseId)
                                                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                                                .withDefendants(asList(defendantWithResults))
                                                .build()
                                )
                        )
                        .build()
                )
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withCaseStatus(READY_FOR_REVIEW.getDescription())
                                        .withDefendants(asList(defendantWithoutResults))
                                        .build()
                        )
                )
                .withHasSharedResults(true)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearingId));

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();
        caseDefendantHearingEntities.add(caseDefendantHearingEntity);

        when(caseDefendantHearingRepository.findByCaseId(prosecutionCaseId)).thenReturn(caseDefendantHearingEntities);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));


        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();

        HearingEntity savedHearingEntity = savedHearingEntities.stream()
                .filter(entity -> entity.getHearingId().equals(hearingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Found Invalid Hearing"));

        final Hearing savedHearing = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearing, notNullValue());
        assertThat(savedHearing.getProsecutionCases().get(0).getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(savedHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getMasterDefendantId(), is(defendantId));
        assertThat(savedHearing.getDefendantJudicialResults().get(0).getJudicialResult().getLabel(), is(OFFENCE_RESULT_LABEL_1));

    }

    private List<CourtApplication> getCourtApplications(final UUID courtApplicationId) {
        return singletonList(CourtApplication.courtApplication()
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .withId(courtApplicationId)
                .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                .withCourtApplicationCases(
                        singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                        .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withCourtOrderOffences(singletonList(courtOrderOffence().withOffence(offence().build()).build()))
                        .build())
                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty().build()))
                .withJudicialResults(asList(JudicialResult.judicialResult().withLabel("PublishedForNowsTRUE").withPublishedForNows(Boolean.TRUE).build(),
                        JudicialResult.judicialResult().withLabel("PublishedForNowsFALSE").withPublishedForNows(Boolean.FALSE).build()))
                .build());
    }

    private ProsecutionCase getProsecutionCase(final UUID prosecutionCaseId) {
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withDefendantCaseJudicialResults(asList(JudicialResult.judicialResult().withLabel("PublishedForNowsTRUE").withPublishedForNows(Boolean.TRUE).build(),
                        JudicialResult.judicialResult().withLabel("PublishedForNowsFALSE").withPublishedForNows(Boolean.FALSE).build()))
                .withOffences(singletonList(
                        offence()
                                .withId(randomUUID())
                                .withJudicialResults(
                                        asList(JudicialResult.judicialResult()
                                                        .withLabel("PublishedForNowsTRUE")
                                                        .withIsNewAmendment(Boolean.TRUE)
                                                        .withPublishedForNows(Boolean.TRUE)
                                                        .withOrderedDate(now())
                                                        .build(),
                                                JudicialResult.judicialResult()
                                                        .withIsNewAmendment(Boolean.TRUE)
                                                        .withLabel("PublishedForNowsFALSE")
                                                        .withPublishedForNows(Boolean.FALSE)
                                                        .withOrderedDate(now())
                                                        .build())
                                )
                                .withOffenceDateCode(4)
                                .withReportingRestrictions(singletonList(
                                        ReportingRestriction.reportingRestriction()
                                                .withId(randomUUID())
                                                .withLabel("ReportingRestrictionLabel")
                                                .withJudicialResultId(randomUUID())
                                                .build()
                                ))
                                .build()))
                .build();
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCpsOrganisation("A01")
                .withDefendants(singletonList(defendant))
                .build();
    }

    private JudicialResult getJudicialResult(final UUID judicialResultId, final String resultLabel) {
        return JudicialResult.judicialResult().withJudicialResultId(judicialResultId).withLabel(resultLabel).build();
    }

    private JudicialResult getJudicialResult(final UUID judicialResultId, final String resultLabel, final LocalDate hearingDate) {
        return JudicialResult.judicialResult()
                .withJudicialResultId(judicialResultId)
                .withLabel(resultLabel)
                .withOrderedDate(hearingDate)
                .build();
    }

    private JudicialResult getJudicialResultWithAmendment(final UUID judicialResultId, final String resultLabel, final LocalDate hearingDate) {
        return JudicialResult.judicialResult()
                .withIsNewAmendment(Boolean.TRUE)
                .withJudicialResultId(judicialResultId)
                .withLabel(resultLabel)
                .withOrderedDate(hearingDate)
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
