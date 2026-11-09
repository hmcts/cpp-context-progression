package uk.gov.moj.cpp.progression.query.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

class GroupCaseConverterTest {

    private final GroupCaseConverter groupCaseConverter = new GroupCaseConverter();

    @Test
    void shouldFlattenCaseIdDefendantNameDobAndAddress() {
        final UUID caseId = randomUUID();
        final Person personDetails = Person.person()
                .withFirstName("John")
                .withLastName("Smith")
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withAddress(Address.address().withAddress1("1 Example Street").withPostcode("KE8 1PG").build())
                .build();
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(personDetails).build())
                .build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant))
                .build();

        final JsonArray summaries = groupCaseConverter.toCaseSummaries(singletonList(prosecutionCase)).build();
        final JsonObject summary = summaries.getJsonObject(0);

        assertThat(summary.getString("caseId"), is(caseId.toString()));
        assertThat(summary.getString("defendantName"), is("John Smith"));
        assertThat(summary.getString("defendantDoB"), is("1990-01-01"));
        assertThat(summary.getJsonObject("address").getString("address1"), is("1 Example Street"));
        assertThat(summary.getJsonObject("address").getString("postcode"), is("KE8 1PG"));
    }

    @Test
    void shouldUseFirstDefendantOnlyWhenMultipleDefendantsPresent() {
        final Defendant firstDefendant = Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person().withFirstName("John").withLastName("Smith").build())
                        .build())
                .build();
        final Defendant secondDefendant = Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person().withFirstName("Jane").withLastName("Doe").build())
                        .build())
                .build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withDefendants(asList(firstDefendant, secondDefendant))
                .build();

        final JsonArray summaries = groupCaseConverter.toCaseSummaries(singletonList(prosecutionCase)).build();

        assertThat(summaries.getJsonObject(0).getString("defendantName"), is("John Smith"));
    }

    @Test
    void shouldPreferCaseUrnOverProsecutionAuthorityReference() {
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withDefendants(emptyList())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("CASE_URN_VALUE")
                        .withProsecutionAuthorityReference("AUTHORITY_REFERENCE_VALUE")
                        .build())
                .build();

        final JsonArray summaries = groupCaseConverter.toCaseSummaries(singletonList(prosecutionCase)).build();

        assertThat(summaries.getJsonObject(0).getString("caseUrn"), is("CASE_URN_VALUE"));
    }

    @Test
    void shouldFallBackToProsecutionAuthorityReferenceWhenCaseUrnAbsent() {
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withDefendants(emptyList())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("AUTHORITY_REFERENCE_VALUE")
                        .build())
                .build();

        final JsonArray summaries = groupCaseConverter.toCaseSummaries(singletonList(prosecutionCase)).build();

        assertThat(summaries.getJsonObject(0).getString("caseUrn"), is("AUTHORITY_REFERENCE_VALUE"));
    }

    @Test
    void shouldReturnLeadCaseWhenGroupMasterPresent() {
        final UUID leadCaseId = randomUUID();
        final ProsecutionCase leadCase = ProsecutionCase.prosecutionCase()
                .withId(leadCaseId)
                .withIsGroupMaster(true)
                .withDefendants(emptyList())
                .build();
        final ProsecutionCase memberCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(false)
                .withDefendants(emptyList())
                .build();
        final List<ProsecutionCase> prosecutionCases = asList(memberCase, leadCase);

        final Optional<JsonObject> leadCaseJson = groupCaseConverter.toLeadCase(prosecutionCases);

        assertThat(leadCaseJson.isPresent(), is(true));
        assertThat(leadCaseJson.get().getString("caseId"), is(leadCaseId.toString()));
    }

    @Test
    void shouldReturnEmptyLeadCaseWhenNoGroupMasterPresent() {
        final ProsecutionCase memberCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(false)
                .withDefendants(emptyList())
                .build();

        final Optional<JsonObject> leadCaseJson = groupCaseConverter.toLeadCase(singletonList(memberCase));

        assertThat(leadCaseJson.isPresent(), is(false));
    }

    @Test
    void shouldOrderCaseSummariesWithLeadCaseFirstThenByDefendantFirstNameAlphabetically() {
        final ProsecutionCase charlieCase = caseWithDefendantFirstName("Charlie");
        final ProsecutionCase aliceCase = caseWithDefendantFirstName("Alice");
        final ProsecutionCase leadCase = caseWithDefendantFirstNameAndGroupMaster("Zach", true);
        final ProsecutionCase bobCase = caseWithDefendantFirstName("bob");

        final List<ProsecutionCase> prosecutionCases = asList(charlieCase, aliceCase, leadCase, bobCase);

        final JsonArray summaries = groupCaseConverter.toCaseSummaries(prosecutionCases).build();

        assertThat(summaries.size(), is(4));
        assertThat(summaries.getJsonObject(0).getString("defendantName"), is("Zach"));
        assertThat(summaries.getJsonObject(1).getString("defendantName"), is("Alice"));
        assertThat(summaries.getJsonObject(2).getString("defendantName"), is("bob"));
        assertThat(summaries.getJsonObject(3).getString("defendantName"), is("Charlie"));
    }

    private ProsecutionCase caseWithDefendantFirstName(final String firstName) {
        return caseWithDefendantFirstNameAndGroupMaster(firstName, false);
    }

    private ProsecutionCase caseWithDefendantFirstNameAndGroupMaster(final String firstName, final boolean isGroupMaster) {
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person().withFirstName(firstName).build())
                        .build())
                .build();

        return ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(isGroupMaster)
                .withDefendants(singletonList(defendant))
                .build();
    }
}
