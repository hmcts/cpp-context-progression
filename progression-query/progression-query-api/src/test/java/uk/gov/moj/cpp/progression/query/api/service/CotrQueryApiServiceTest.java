package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.QueryClientTestBase.metadataFor;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.directionmanagement.query.Assignee;
import uk.gov.justice.directionmanagement.query.RefData;
import uk.gov.justice.progression.query.CaseDirections;
import uk.gov.justice.progression.query.TrialReadinessHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class CotrQueryApiServiceTest {

    @InjectMocks
    private CotrQueryApiService cotrQueryApiService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Requester requester;

    @Mock
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @Mock
    private JsonEnvelope requestEnvelope;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void returnsNullWhenThereAreNoHearingDays() {
        final HearingDay earliestHearingDay = cotrQueryApiService.getEarliestHearingDay(new ArrayList<>());
        assertThat(earliestHearingDay, is(nullValue()));
    }

    @Test
    public void returnsEarliestHearingDayWhenThereIsOnlyOneHearingDay() {
        final List<HearingDay> hearingDays = new ArrayList<>();
        HearingDay day1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now())
                .withListingSequence(1)
                .build();
        hearingDays.add(day1);
        final HearingDay earliestHearingDay = cotrQueryApiService.getEarliestHearingDay(hearingDays);

        assertThat(earliestHearingDay.getListingSequence(), is(1));
    }

    @Test
    public void returnsEarliestHearingDayWhenThereAreTwoHearingDays() {
        final List<HearingDay> hearingDays = new ArrayList<>();
        HearingDay day1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now())
                .withListingSequence(1)
                .build();
        hearingDays.add(day1);
        HearingDay day2 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now().plusDays(1))
                .withListingSequence(2)
                .build();
        hearingDays.add(day2);
        final HearingDay earliestHearingDay = cotrQueryApiService.getEarliestHearingDay(hearingDays);

        assertThat(earliestHearingDay.getListingSequence(), is(1));
    }

    @Test
    public void shouldGetTrialReadinessHearing(){
        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                        .withTimeLimit(LocalDate.now())
                        .build())
                .build());

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName("John")
                                .withLastName("Turing")
                                .build())
                        .build())
                .withOffences(offences)
                .build());

        final List<Marker> caseMarkers = new ArrayList<>();
        caseMarkers.add(Marker.marker()
                .withMarkerTypeDescription("typeDesc")
                .build());

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        prosecutionCases.add(ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("caseURN123")
                        .build())
                .withDefendants(defendants)
                .withCaseMarkers(caseMarkers)
                .build());

        final List<HearingDay> hearingDays = new ArrayList<>();
        HearingDay day1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now())
                .withListingSequence(1)
                .build();
        hearingDays.add(day1);

        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(prosecutionCases)
                .withHearingDays(hearingDays)
                .build();
        final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = new ArrayList<>();
        final TrialReadinessHearing trialReadinessHearing = cotrQueryApiService.getTrialReadinessHearing(hearing, caseDirections);
        assertThat(trialReadinessHearing, Matchers.is(notNullValue()));
    }

    @Test
    public void shouldConvertCasesDirections_Cotr(){
        List<CaseDirections> caseDirections = cotrQueryApiService.convertCasesDirections(createCaseDirectionsList("86a1c852-b581-4c38-b4d0-ad8d8130d970"));
        assertThat(caseDirections, Matchers.is(notNullValue()));
        assertThat(caseDirections.get(0).getType(), Matchers.is("COTR"));
    }

    @Test
    public void shouldConvertCasesDirections_Stage2(){
        List<CaseDirections> caseDirections = cotrQueryApiService.convertCasesDirections(createCaseDirectionsList("29be8453-b3c6-4f5c-815f-7f6b41ac1ac1"));
        assertThat(caseDirections, Matchers.is(notNullValue()));
        assertThat(caseDirections.get(0).getType(), Matchers.is("STAGE2"));
    }

    @Test
    public void shouldConvertCasesDirections_Section28(){
        List<CaseDirections> caseDirections = cotrQueryApiService.convertCasesDirections(createCaseDirectionsList("44a07637-d501-4604-9716-1bc664ce2e69"));
        assertThat(caseDirections, Matchers.is(notNullValue()));
        assertThat(caseDirections.get(0).getType(), Matchers.is("SECTION28"));
    }

    private final List<uk.gov.justice.directionmanagement.query.CaseDirections> createCaseDirectionsList(final String directionRefDataId){
        final List<uk.gov.justice.directionmanagement.query.CaseDirections> caseDirectionsList = new ArrayList<>();
        caseDirectionsList.add(uk.gov.justice.directionmanagement.query.CaseDirections.caseDirections()
                .withCaseId(randomUUID())
                .withAssignee(Assignee.assignee()
                        .withAssigneePersonId(randomUUID())
                        .build())
                .withRefData(RefData.refData()
                        .withDirectionRefDataId(UUID.fromString(directionRefDataId))
                        .build())
                .build());

        return caseDirectionsList;
    }

    @Test
    public void shouldCreateCtlForDefendant(){
        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                        .withTimeLimit(LocalDate.now())
                        .build())
                .build());
        final LocalDate localDate = cotrQueryApiService.createCtlForDefendant(Defendant.defendant()
                .withId(randomUUID())
                .withOffences(offences)
                .build());
        assertThat(localDate, Matchers.is(notNullValue()));
    }

    @Test
    public void shouldGetCotrDetailsProsecutionCase(){
        final Metadata metadata = metadataFor("progression.query.cotr.details.prosecutioncase", randomUUID());
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().build());
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        final Optional<JsonObject> jsonObject = cotrQueryApiService.getCotrDetailsProsecutionCase(requester, randomUUID().toString());
        assertThat(jsonObject.get().size(), Matchers.is(0));
    }

    @Test
    public void shouldGetCotrDetails(){
        when(prosecutionCaseQuery.getCotrDetails(any())).thenReturn(requestEnvelope);
        final Optional<JsonObject> jsonObject = cotrQueryApiService.getCotrDetails(prosecutionCaseQuery, randomUUID().toString());
        assertThat(jsonObject.isPresent(), Matchers.is(false));
    }
}
