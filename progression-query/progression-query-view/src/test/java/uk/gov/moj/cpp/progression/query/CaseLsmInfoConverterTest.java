package uk.gov.moj.cpp.progression.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.*;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.query.utils.CaseLsmInfoConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class CaseLsmInfoConverterTest {
    private static final UUID MASTER_DEFENDANT_ID1 = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID2 = randomUUID();
    private static final UUID DEFENDANT_ID1 = randomUUID();
    private static final UUID DEFENDANT_ID2 = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final String HEARING_DAY = "2023-04-06T09:30:00Z";

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CaseLsmInfoConverter caseLsmInfoConverter;


    @Test
    public void shouldConvertDefendantsToJsonArrayBuilder() {
        final List<Defendant> defendants = Arrays.asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1, true), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID2, null));
        final Hearing hearing = createHearing(defendants);
        final JsonArray jsonArray = caseLsmInfoConverter.convertMatchedCaseDefendants(defendants, hearing, MASTER_DEFENDANT_ID1).build();
        assertThat(jsonArray.size(), is(2));
        validate(jsonArray.getJsonObject(0), defendants.get(0), hearing);
        validate(jsonArray.getJsonObject(1), defendants.get(1), hearing);
    }

    @Test
    public void shouldConvertDefendantsToJsonArrayBuilderWhenProceedingsConcludedIsNull() {
        final List<Defendant> defendants = Arrays.asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1, null), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID2, null));
        final Hearing hearing = createHearing(defendants);
        final JsonArray jsonArray = caseLsmInfoConverter.convertMatchedCaseDefendants(defendants, hearing, MASTER_DEFENDANT_ID1).build();
        assertThat(jsonArray.size(), is(2));
        validate(jsonArray.getJsonObject(0), defendants.get(0), hearing);
        validate(jsonArray.getJsonObject(1), defendants.get(1), hearing);
    }

    @Test
    public void shouldConvertDefendantsToJsonArrayBuilderWhenNoHearingFound() {
        final List<Defendant> defendants = Arrays.asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1, true), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID2, null));
        final JsonArray jsonArray = caseLsmInfoConverter.convertMatchedCaseDefendants(defendants, null, MASTER_DEFENDANT_ID1).build();
        assertThat(jsonArray.size(), is(2));
        validate(jsonArray.getJsonObject(0), defendants.get(0), null);
        validate(jsonArray.getJsonObject(1), defendants.get(1), null);
    }

    @Test
    public void shouldReturnLegalEntityDefendant(){
        final List<Defendant> defendants = Stream.generate(() -> createLegalEntityDefendant())
                .limit(1)
                .collect(Collectors.toList());

        final JsonArray jsonArray = caseLsmInfoConverter.convertRelatedCaseDefendants(defendants, null).build();
        assertThat(jsonArray.size(), is(1));
        validate(jsonArray.getJsonObject(0), defendants.get(0), null);
    }

    private void validate(final JsonObject actual, final Defendant expected, final Hearing hearing){
        assertThat(actual.getString("id"), is(expected.getId().toString()));
        assertThat(actual.getString("masterDefendantId"), is(expected.getMasterDefendantId().toString()));
        final boolean actualHasBeenResulted = actual.getBoolean("hasBeenResulted", false);

        if (expected.getProceedingsConcluded() == null){
            assertThat(actualHasBeenResulted, is(false));
        } else {
            assertThat(actualHasBeenResulted, is(expected.getProceedingsConcluded()));
        }

        if (expected.getPersonDefendant() != null) {
            assertThat(actual.getString("lastName"), is(expected.getPersonDefendant().getPersonDetails().getLastName()));
        } else {
            assertThat(actual.getString("organisationName"), is(expected.getLegalEntityDefendant().getOrganisation().getName()));
        }

        JsonArray offencesArray = actual.getJsonArray("offences");
        assertNotNull(offencesArray);
        assertThat(offencesArray.size(), is(1));
        JsonObject offence = offencesArray.getJsonObject(0);
        assertThat(offence.getString("offenceTitle"), is(expected.getOffences().get(0).getOffenceTitle()));
        JsonObject nextHearing = actual.getJsonObject("nextHearing");
        if (hearing == null) {
            assertNull(nextHearing);
        } else {
            assertNotNull(nextHearing);
            assertThat(nextHearing.getString("hearingId"), is(HEARING_ID.toString()));
            assertThat(nextHearing.getString("type"), is(hearing.getType().getDescription()));
            assertThat(nextHearing.getString("hearingDay"), is(HEARING_DAY));
        }
    }

    private CaseDefendantHearingEntity createCaseDefendantHearingEntity(){
        final List<Defendant> defendants = Arrays.asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1,true), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID1, null));
        final Hearing hearing = createHearing(defendants);
        final JsonObject hearingPayload = objectToJsonObjectConverter.convert(hearing);
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(randomUUID());
        hearingEntity.setPayload(hearingPayload.toString());
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(randomUUID(), randomUUID(), randomUUID()));
        caseDefendantHearingEntity.setHearing(hearingEntity);
        return caseDefendantHearingEntity;
    }

    private Hearing createHearing(final List<Defendant> defendants){

        return Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType()
                        .withDescription("TRIAL")
                        .build())
                .withProsecutionCases(Arrays.asList(
                        ProsecutionCase.prosecutionCase()
                                .withDefendants(defendants)
                                .build()
                ))
                .withHearingDays(
                        Arrays.asList(
                                HearingDay.hearingDay()
                                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DAY))
                                        .build()
                        ))
                .build();
    }

    private Defendant createDefendant(final UUID defendantId, final UUID masterDefendantId, final Boolean proceedingsConcluded) {
         return Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .withId(defendantId)
                .withProceedingsConcluded(proceedingsConcluded)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName("John")
                                .withLastName("Wick")
                                .build())
                        .build())
                .withOffences(Arrays.asList(Offence.offence()
                        .withOffenceTitle("Robbery")
                        .withProceedingsConcluded(proceedingsConcluded)
                        .build()))
                .build();
    }

    private Defendant createLegalEntityDefendant() {
        return Defendant.defendant()
                .withMasterDefendantId(randomUUID())
                .withId(randomUUID())
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("Any Organisation")
                                .build())
                        .build())
                .withOffences(Arrays.asList(Offence.offence()
                        .withOffenceTitle("Robbery")
                        .build()))
                .build();
    }
}
