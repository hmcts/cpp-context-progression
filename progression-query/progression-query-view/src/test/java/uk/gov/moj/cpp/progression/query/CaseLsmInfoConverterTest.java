package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.core.courts.HearingDay.hearingDay;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.progression.query.utils.CaseLsmInfoConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseLsmInfoConverterTest {
    private static final UUID MASTER_DEFENDANT_ID1 = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID2 = randomUUID();
    private static final UUID DEFENDANT_ID1 = randomUUID();
    private static final UUID DEFENDANT_ID2 = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime HEARING_DAY = new UtcClock().now().plusDays(1);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CaseLsmInfoConverter caseLsmInfoConverter;


    @Test
    public void shouldConvertDefendantsToJsonArrayBuilder() {
        final List<Defendant> defendants = asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1, true), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID2, null));
        final Hearing hearing = createHearing(defendants);
        final JsonArray jsonArray = caseLsmInfoConverter.convertMatchedCaseDefendants(defendants, hearing, MASTER_DEFENDANT_ID1).build();
        assertThat(jsonArray.size(), is(2));
        validate(jsonArray.getJsonObject(0), defendants.get(0), hearing);
        validate(jsonArray.getJsonObject(1), defendants.get(1), hearing);
    }

    @Test
    public void shouldConvertDefendantsToJsonArrayBuilderWhenProceedingsConcludedIsNull() {
        final List<Defendant> defendants = asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1, null), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID2, null));
        final Hearing hearing = createHearing(defendants);
        final JsonArray jsonArray = caseLsmInfoConverter.convertMatchedCaseDefendants(defendants, hearing, MASTER_DEFENDANT_ID1).build();
        assertThat(jsonArray.size(), is(2));
        validate(jsonArray.getJsonObject(0), defendants.get(0), hearing);
        validate(jsonArray.getJsonObject(1), defendants.get(1), hearing);
    }

    @Test
    public void shouldConvertDefendantsToJsonArrayBuilderWhenNoHearingFound() {
        final List<Defendant> defendants = asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1, true), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID2, null));
        final JsonArray jsonArray = caseLsmInfoConverter.convertMatchedCaseDefendants(defendants, null, MASTER_DEFENDANT_ID1).build();
        assertThat(jsonArray.size(), is(2));
        validate(jsonArray.getJsonObject(0), defendants.get(0), null);
        validate(jsonArray.getJsonObject(1), defendants.get(1), null);
    }

    @Test
    public void shouldReturnLegalEntityDefendant() {
        final List<Defendant> defendants = Stream.generate(() -> createLegalEntityDefendant())
                .limit(1)
                .collect(Collectors.toList());

        final JsonArray jsonArray = caseLsmInfoConverter.convertRelatedCaseDefendants(defendants, null).build();
        assertThat(jsonArray.size(), is(1));
        validate(jsonArray.getJsonObject(0), defendants.get(0), null);
    }

    private void validate(final JsonObject actual, final Defendant expected, final Hearing hearing) {
        assertThat(actual.getString("id"), is(expected.getId().toString()));
        assertThat(actual.getString("masterDefendantId"), is(expected.getMasterDefendantId().toString()));
        final boolean actualHasBeenResulted = actual.getBoolean("hasBeenResulted", false);

        if (expected.getProceedingsConcluded() == null) {
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
            assertThat(nextHearing.getString("hearingDay"), is(HEARING_DAY.format(DateTimeFormatter.ISO_INSTANT)));
        }
    }

    private CaseDefendantHearingEntity createCaseDefendantHearingEntity() {
        final List<Defendant> defendants = asList(createDefendant(DEFENDANT_ID1, MASTER_DEFENDANT_ID1,true), createDefendant(DEFENDANT_ID2, MASTER_DEFENDANT_ID1, null));
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

    private Hearing createHearing(final List<Defendant> defendants) {

        return Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType()
                        .withDescription("TRIAL")
                        .build())
                .withProsecutionCases(singletonList(
                        prosecutionCase()
                                .withDefendants(defendants)
                                .build()
                ))
                .withHearingDays(
                        singletonList(hearingDay()
                                        .withSittingDay(HEARING_DAY)
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
                .withOffences(asList(Offence.offence()
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
                .withOffences(asList(Offence.offence()
                        .withOffenceTitle("Robbery")
                        .build()))
                .build();
    }
}
