package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import javax.json.Json;
import javax.json.JsonObject;

public class DefendantHelperTest {

    PersonDefendant personDefendant;
    PersonDefendant updatedPersonDefendant;
    Person personDetails;
    Address contactAddress;
    ContactNumber contactDetails;
    Organisation organisation;

    private static Offence createOffence(final UUID offenceId, final String offenceCode) {
        return Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withStartDate(LocalDate.now()).withArrestDate(LocalDate.now()).withChargeDate(LocalDate.now()).withConvictionDate(LocalDate.now()).withEndDate(LocalDate.now()).withOffenceTitle("title").withOffenceTitleWelsh("welsh title").withWording("wording").withOffenceLegislation("legisltation").withOffenceLegislationWelsh("welsh legisltation").withCount(1).withOrderIndex(500).withOffenceFacts(OffenceFacts.offenceFacts().withAlcoholReadingAmount(new Integer(100)).withAlcoholReadingMethodCode("B").build()).build();
    }

    private static Offence.Builder createOffenceWithDefaults(final UUID offenceId, final String offenceCode) {
        return Offence.offence().
                withId(offenceId)
                .withOffenceCode(offenceCode)
                .withStartDate(LocalDate.now())
                .withArrestDate(LocalDate.now())
                .withChargeDate(LocalDate.now())
                .withConvictionDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withOffenceTitle("title")
                .withOffenceTitleWelsh("welsh title")
                .withWording("wording")
                .withOffenceLegislation("legisltation")
                .withOffenceLegislationWelsh("welsh legisltation")
                .withCount(1);
    }

    @BeforeEach
    public void setUp() throws Exception {
        contactDetails = ContactNumber.contactNumber().withHome("0845123574").withWork("0334578522").withMobile("07896542875").withPrimaryEmail("john.smith@hmcts.net").withSecondaryEmail("john.smith@hmcts.net").withFax("0845123574").build();

        contactAddress = Address.address().withAddress1("22").withAddress2("Acacia Avenue").withAddress3("Acacia Street").withAddress4("Acacia Town").withAddress5("Acacia County").withPostcode("GIR 0AA").build();

        organisation = Organisation.organisation().withName("HMCTS").withIncorporationNumber("INC-45875").withAddress(contactAddress).withContact(contactDetails).build();
        final UUID observedEthnicityId = randomUUID();
        final UUID selfDefEthnicityId = randomUUID();
        personDetails = Person.person().withTitle("DR").withFirstName("John").withMiddleName("S").withLastName("Smith")
                .withDateOfBirth(LocalDate.of(2000, 01, 01)).withNationalityId(randomUUID()).withAdditionalNationalityId(randomUUID())
                .withEthnicity(Ethnicity.ethnicity().withSelfDefinedEthnicityId(selfDefEthnicityId).build())
                .withGender(Gender.MALE)
                .withInterpreterLanguageNeeds("John")
                .withDocumentationLanguageNeeds(HearingLanguage.WELSH).withNationalInsuranceNumber("SK384524").withOccupation("Student").withSpecificRequirements("Screen").withAddress(contactAddress).withContact(contactDetails).build();

        personDefendant = PersonDefendant.personDefendant()
                .withBailStatus(bailStatus().withId(randomUUID()).withDescription("Remanded into Custody").withCode("C").build())
                .withCustodyTimeLimit(LocalDate.of(2018, 12, 01))
                .withPersonDetails(Person.person()
                        .withEthnicity(Ethnicity.ethnicity()
                                .withSelfDefinedEthnicityId(selfDefEthnicityId)
                                .withObservedEthnicityId(observedEthnicityId)
                                .build())
                        .build())
                .withArrestSummonsNumber("arrest123")
                .withPersonDetails(personDetails)
                .withEmployerOrganisation(organisation).build();

        updatedPersonDefendant = PersonDefendant.personDefendant()
                .withBailStatus(bailStatus().withId(randomUUID()).withDescription("Remanded into Custody").withCode("C").build()).withCustodyTimeLimit(LocalDate.of(2018, 12, 01))
                .withPersonDetails(Person.person()
                        .withEthnicity(Ethnicity.ethnicity()
                                .withSelfDefinedEthnicityId(selfDefEthnicityId)
                                .withObservedEthnicityId(observedEthnicityId)
                                .build())
                        .build())

                .withArrestSummonsNumber("arrest123")
                .withPersonDetails(personDetails)
                .withEmployerOrganisation(organisation).build();
    }

    @Test
    public void shouldUpdateOrderIndex() {
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final ArrayList<JsonObject> jsonObjects = new ArrayList<>();
        final JsonObject jsonObjectOffence = Json.createObjectBuilder().add("maxPenalty", "Indicated").add("cjsOffenceCode", "first").build();
        jsonObjects.add(jsonObjectOffence);
        final Optional<List<JsonObject>> refDataOffences = Optional.of(jsonObjects);
        Offence offence = DefendantHelper.updateOrderIndex(offenceOne, 100, refDataOffences);
        assertThat(offence.getOrderIndex(), is(100));
        assertThat(offence.getMaxPenalty(), is("Indicated"));
    }

    @Test
    public void testOffencesUpdatedWithNewOffence() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        commandOffenceList.add(offenceOne);
        commandOffenceList.add(offenceTwo);

        existingOffenceList.add(offenceOne);

        assertTrue(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void testOffencesUpdatedWithDeletedOffence() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        commandOffenceList.add(offenceOne);

        existingOffenceList.add(offenceOne);
        existingOffenceList.add(offenceTwo);

        assertTrue(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void testOffencesUpdatedWithModifiedOffence() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final UUID offenceOneId = randomUUID();
        final Offence offenceOne = createOffence(offenceOneId, "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        final Offence updatedOffence = createOffence(offenceOneId, "updated");

        commandOffenceList.add(updatedOffence);
        commandOffenceList.add(offenceTwo);

        existingOffenceList.add(offenceOne);
        existingOffenceList.add(offenceTwo);

        assertTrue(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void testUnchangedOffences() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        commandOffenceList.add(offenceOne);

        existingOffenceList.add(offenceOne);

        assertFalse(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void shouldGetOffencesForDefencesUpdated() {
        UUID offenceId = randomUUID();
        Offence first = createOffence(offenceId, "first");
        Offence.Builder updatedOffenceBuilder = createOffenceWithDefaults(offenceId, "first");
        Offence.Builder updatedOffence = updatedOffenceBuilder.withLaaApplnReference(LaaReference.laaReference().withApplicationReference("applicationReference").build());
        final List<Offence> offences = singletonList(updatedOffence.build());
        final List<Offence> existingOffences = singletonList(first);
        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(offences, existingOffences, uuid, defendantId);
        assertTrue(offencesForDefendantUpdated.isPresent());
        assertThat(offencesForDefendantUpdated.get().getAddedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getDeletedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getUpdatedOffences().size(), is(1));
    }

    @Test
    public void shouldNotGetOffencesForDefencesUpdatedWhenThereIsNoMatchingOffence() {
        UUID offenceId = randomUUID();
        Offence first = createOffence(randomUUID(), "first");
        Offence.Builder updatedOffenceBuilder = createOffenceWithDefaults(offenceId, "second");
        Offence.Builder updatedOffence = updatedOffenceBuilder.withLaaApplnReference(LaaReference.laaReference().withApplicationReference("applicationReference").build());
        final List<Offence> offences = singletonList(updatedOffence.build());
        final List<Offence> existingOffences = singletonList(first);
        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(offences, existingOffences, uuid, defendantId);
        assertFalse(offencesForDefendantUpdated.isPresent());
    }

    @Test
    public void shouldGetOffencesForDefendantUpdatedIfReportingRestrictionHasRemoved() {
        final UUID offenceId = randomUUID();

        final Offence.Builder offenceBuilder = createOffenceWithDefaults(offenceId, "first");
        final Offence existingOffence = offenceBuilder
                .withReportingRestrictions(singletonList(ReportingRestriction.reportingRestriction()
                .withId(randomUUID())
                .withLabel("Reporting Restriction Label")
                .withJudicialResultId(randomUUID())
                .withOrderedDate(LocalDate.now())
                .build()))
                .build();

        final List<Offence> existingOffences = singletonList(existingOffence);

        final Offence.Builder updatedOffenceBuilder = createOffenceWithDefaults(offenceId, "first");
        final List<Offence> updatedOffences = singletonList(updatedOffenceBuilder.build());

        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(updatedOffences, existingOffences, uuid, defendantId);
        assertThat(offencesForDefendantUpdated.isPresent(), is(true));
        assertThat(offencesForDefendantUpdated.get().getAddedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getDeletedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getUpdatedOffences().size(), is(1));
    }

    @Test
    public void shouldGetOffencesForDefendantUpdatedIfReportingRestrictionLabelChanged() {
        final UUID offenceId = randomUUID();
        final UUID reportingRestrictionId = randomUUID();
        final UUID judicialResultId = randomUUID();
        final LocalDate reportingRestrictionOrderedDate = LocalDate.now();

        final Offence existingOffence = createOffenceWithReportingRestriction(reportingRestrictionId, judicialResultId, reportingRestrictionOrderedDate, offenceId, "Reporting Restriction Label")
                .build();

        final List<Offence> existingOffences = singletonList(existingOffence);

        final Offence.Builder updatedOffenceBuilder = createOffenceWithReportingRestriction(reportingRestrictionId, judicialResultId, reportingRestrictionOrderedDate, offenceId, "Reporting Restriction Label Changed");
        final List<Offence> updatedOffences = singletonList(updatedOffenceBuilder.build());

        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(updatedOffences, existingOffences, uuid, defendantId);
        assertThat(offencesForDefendantUpdated.isPresent(), is(true));
        assertThat(offencesForDefendantUpdated.get().getAddedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getDeletedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getUpdatedOffences().size(), is(1));
    }

    @Test
    public void shouldGetOffencesForDefendantUpdatedIfReportingRestrictionJudicialResultHasBeenAdded() {
        final UUID offenceId = randomUUID();
        final UUID reportingRestrictionId = randomUUID();
        final UUID judicialResultId = randomUUID();
        final LocalDate reportingRestrictionOrderedDate = LocalDate.now();

        final Offence.Builder offenceBuilder = createOffenceWithDefaults(offenceId, "first");
        final Offence existingOffence = offenceBuilder
                .withReportingRestrictions(Stream.of(ReportingRestriction.reportingRestriction()
                        .withId(reportingRestrictionId)
                        .withLabel("Reporting Restriction Label")
                        .withOrderedDate(reportingRestrictionOrderedDate)
                        .build())
                        .collect(Collectors.toList()))
                .build();

        final List<Offence> existingOffences = singletonList(existingOffence);

        final Offence.Builder updatedOffenceBuilder = createOffenceWithReportingRestriction(reportingRestrictionId, judicialResultId, reportingRestrictionOrderedDate, offenceId, "Reporting Restriction Label");
        final List<Offence> updatedOffences = singletonList(updatedOffenceBuilder.build());

        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(updatedOffences, existingOffences, uuid, defendantId);
        assertThat(offencesForDefendantUpdated.isPresent(), is(true));
        assertThat(offencesForDefendantUpdated.get().getAddedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getDeletedOffences(), is(nullValue()));
        assertThat(offencesForDefendantUpdated.get().getUpdatedOffences().size(), is(1));
    }

    @Test
    public void shouldNotGetOffencesForDefendantUpdatedIfReportingRestrictionAsItIs() {
        final UUID offenceId = randomUUID();
        final UUID reportingRestrictionId = randomUUID();
        final UUID judicialResultId = randomUUID();
        final LocalDate reportingRestrictionOrderedDate = LocalDate.now();

        final Offence existingOffence = createOffenceWithReportingRestriction(reportingRestrictionId, judicialResultId, reportingRestrictionOrderedDate, offenceId, "Reporting Restriction Label")
                .build();

        final List<Offence> existingOffences = singletonList(existingOffence);

        final Offence.Builder updatedOffenceBuilder = createOffenceWithReportingRestriction(reportingRestrictionId, judicialResultId, reportingRestrictionOrderedDate, offenceId, "Reporting Restriction Label");
        final List<Offence> updatedOffences = singletonList(updatedOffenceBuilder.build());

        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(updatedOffences, existingOffences, uuid, defendantId);
        assertThat(offencesForDefendantUpdated.isPresent(), is(false));
    }

    @Test
    public void shouldNotGetOffencesForDefendantUpdatedIfReportingRestrictionAsItIsButInDifferentOrder() {
        final UUID offenceId = randomUUID();
        final UUID reportingRestrictionId1 = randomUUID();
        final UUID judicialResultId1 = randomUUID();

        final LocalDate reportingRestrictionOrderedDate = LocalDate.now();
        final String reportingRestrictionLabel = "Reporting Restriction Label";

        final UUID reportingRestrictionId2 = randomUUID();
        final UUID judicialResultId2 = randomUUID();

        final Offence existingOffence = createOffenceWithMultipleReportingRestriction(
                prepareReportingRestriction(reportingRestrictionId1, judicialResultId1, reportingRestrictionOrderedDate, reportingRestrictionLabel),
                prepareReportingRestriction(reportingRestrictionId2, judicialResultId2, reportingRestrictionOrderedDate, reportingRestrictionLabel),
                offenceId)
                .build();

        final List<Offence> existingOffences = singletonList(existingOffence);

        final Offence.Builder updatedOffenceBuilder = createOffenceWithMultipleReportingRestriction(
                prepareReportingRestriction(reportingRestrictionId2, judicialResultId2, reportingRestrictionOrderedDate, reportingRestrictionLabel),
                prepareReportingRestriction(reportingRestrictionId1, judicialResultId1, reportingRestrictionOrderedDate, reportingRestrictionLabel),
                offenceId);
        final List<Offence> updatedOffences = singletonList(updatedOffenceBuilder.build());

        final UUID uuid = randomUUID();
        final UUID defendantId = randomUUID();
        Optional<OffencesForDefendantChanged> offencesForDefendantUpdated = DefendantHelper.getOffencesForDefendantUpdated(updatedOffences, existingOffences, uuid, defendantId);
        assertThat(offencesForDefendantUpdated.isPresent(), is(false));
    }

    @Test
    public void shouldIsConcludedBeTrueFinalResultInAllOffenceAndDefendantAndCaseLevel() {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();

        final List<DefendantJudicialResult> defendantJudicialResults = Arrays.asList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withOffenceId(randomUUID()).build())
                .build());

        final List<JudicialResult> caseDefendantJudicialResults = Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withOffenceId(randomUUID()).build());
        assertTrue(DefendantHelper.isConcluded(offence, defendantJudicialResults, caseDefendantJudicialResults));
    }

    @ParameterizedTest
    @MethodSource("provideDefendantJudicialResultNotIncludedOffence")
    @NullAndEmptySource
    public void shouldIsConcludedBeTrueOffenceHasFinalResultButNotExistsInDefendantJudicialResult(List<DefendantJudicialResult> defendantJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();
        assertTrue(DefendantHelper.isConcluded(offence, defendantJudicialResults, emptyList()));
    }

    private static Stream<Arguments> provideDefendantJudicialResultNotIncludedOffence() {
        return Stream.of(
                Arguments.of(Arrays.asList(DefendantJudicialResult.defendantJudicialResult().build()), Arrays.asList(JudicialResult.judicialResult().build())),
                Arguments.of(Arrays.asList(DefendantJudicialResult.defendantJudicialResult().withJudicialResult(JudicialResult.judicialResult().withCategory(JudicialResultCategory.FINAL).withOffenceId(randomUUID()).build()).build()), Arrays.asList(JudicialResult.judicialResult().build())),
                Arguments.of(Arrays.asList(DefendantJudicialResult.defendantJudicialResult().withJudicialResult(JudicialResult.judicialResult().withCategory(JudicialResultCategory.ANCILLARY).withOffenceId(randomUUID()).build()).build()), Arrays.asList(JudicialResult.judicialResult().build()))
        );
    }

    @ParameterizedTest
    @MethodSource("provideCaseDefendantJudicialResultNotIncludedOffence")
    @NullAndEmptySource
    public void shouldIsConcludedBeTrueOffenceHasFinalResultButNotExistsInCaseDefendantJudicialResult(List<JudicialResult> caseDefendantJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();
        assertTrue(DefendantHelper.isConcluded(offence, emptyList(), caseDefendantJudicialResults));
    }

    private static Stream<Arguments> provideCaseDefendantJudicialResultNotIncludedOffence() {
        return Stream.of(
                Arguments.of(Arrays.asList(JudicialResult.judicialResult().withCategory(JudicialResultCategory.FINAL).withOffenceId(randomUUID()).build())),
                Arguments.of(Arrays.asList(JudicialResult.judicialResult().withCategory(JudicialResultCategory.ANCILLARY).withOffenceId(randomUUID()).build()))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void shouldIsConcludedBeTrueOffenceHasFinalResultAndDefendantJudicialResultHasFinal(final List<JudicialResult> defendantCaseJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = Arrays.asList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withOffenceId(offenceId)
                        .build())
                .build());
        assertTrue(DefendantHelper.isConcluded(offence, defendantJudicialResults, defendantCaseJudicialResults));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void shouldIsConcludedBeTrueOffenceHasFinalResultAndCaseDefendantJudicialResultHasFinal(List<DefendantJudicialResult> defendantJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();
        final List<JudicialResult> caseDefendantJudicialResults = Arrays.asList(JudicialResult.judicialResult()
                .withCategory(JudicialResultCategory.FINAL)
                .withOffenceId(offenceId)
                .build());
        assertTrue(DefendantHelper.isConcluded(offence, defendantJudicialResults, caseDefendantJudicialResults));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void shouldIsConcludedBeFalseOffenceHasFinalResultAndDefendantJudicialResultHasNotFinal(final List<JudicialResult> defendantCaseJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = Arrays.asList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.ANCILLARY)
                        .withOffenceId(offenceId)
                        .build())
                .build());
        assertFalse(DefendantHelper.isConcluded(offence, defendantJudicialResults, defendantCaseJudicialResults));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void shouldIsConcludedBeFalseOffenceHasFinalResultAndCaseDefendantJudicialResultHasNotFinal(final List<DefendantJudicialResult> defendantJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(offenceId)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build()))
                .build();
        final List<JudicialResult> defendantCaseJudicialResults = Arrays.asList(JudicialResult.judicialResult()
                .withCategory(JudicialResultCategory.ANCILLARY)
                .withOffenceId(offenceId)
                .build());
        assertFalse(DefendantHelper.isConcluded(offence, defendantJudicialResults, defendantCaseJudicialResults));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void shouldIsConcludedBeTrueOffenceHasNoResultAndDefendantJudicialResultHasFinal(final List<JudicialResult> defendantCaseJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence().withId(offenceId).build();
        final List<DefendantJudicialResult> defendantJudicialResults = Arrays.asList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withOffenceId(offenceId)
                        .build())
                .build());
        assertTrue(DefendantHelper.isConcluded(offence, defendantJudicialResults, defendantCaseJudicialResults));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void shouldIsConcludedBeTrueOffenceHasNoResultAndCaseDefendantJudicialResultHasFinal(final List<DefendantJudicialResult> defendantJudicialResults) {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence().withId(offenceId).build();
        final List<JudicialResult> defendantCaseJudicialResults = Arrays.asList(JudicialResult.judicialResult()
                .withCategory(JudicialResultCategory.FINAL)
                .withOffenceId(offenceId)
                .build());
        assertTrue(DefendantHelper.isConcluded(offence, defendantJudicialResults, defendantCaseJudicialResults));
    }

    @Test
    public void shouldIsConcludedBeFalseOffenceHasNoResultAndDefendantJudicialResultAndCaseDefendantJudicialResultIsEmpty() {
        final UUID offenceId = randomUUID();
        final Offence offence = Offence.offence().withId(offenceId).build();

        assertFalse(DefendantHelper.isConcluded(offence, emptyList(), emptyList()));
    }

    private Offence.Builder createOffenceWithMultipleReportingRestriction(final ReportingRestriction reportingRestriction1,
                                                                          final ReportingRestriction reportingRestriction2,
                                                                          final UUID offenceId) {
        return createOffenceWithDefaults(offenceId, "first")
                .withReportingRestrictions(Stream.of(reportingRestriction1, reportingRestriction2)
                        .collect(Collectors.toList()));
    }

    private Offence.Builder createOffenceWithReportingRestriction(final UUID reportingRestrictionId,
                                                                  final UUID judicialResultId,
                                                                  final LocalDate rrOrderedDate,
                                                                  final UUID offenceId,
                                                                  final String label) {
        return createOffenceWithDefaults(offenceId, "first")
                .withReportingRestrictions(Stream.of(prepareReportingRestriction(reportingRestrictionId, judicialResultId, rrOrderedDate, label))
                        .collect(Collectors.toList()));
    }

    private ReportingRestriction prepareReportingRestriction(final UUID reportingRestrictionId,
                                                             final UUID judicialResultId,
                                                             final LocalDate rrOrderedDate,
                                                             final String label) {
        return ReportingRestriction.reportingRestriction()
                .withId(reportingRestrictionId)
                .withLabel(label)
                .withJudicialResultId(judicialResultId)
                .withOrderedDate(rrOrderedDate)
                .build();
    }


}
