package uk.gov.moj.cpp.progression.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.NI_NUMBER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.POST_CODE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.NotifiedPlea;
import uk.gov.justice.core.courts.NotifiedPleaValue;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.Source;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"squid:ClassVariableVisibilityCheck", "squid:S1067", "pmd:NullAssignment", "squid:CommentedOutCodeLine", "squid:S1135"})
public class CoreTestTemplates {

    private static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    public enum DefendantType {
        PERSON, ORGANISATION
    }


    public static class CoreTemplateArguments {

        public boolean convicted = false;

        private JurisdictionType jurisdictionType = JurisdictionType.CROWN;

        private HearingLanguage hearingLanguage = HearingLanguage.ENGLISH;

        public DefendantType defendantType = DefendantType.PERSON;

        private boolean minimumAssociatedPerson;
        private boolean minimumDefenceOrganisation;
        private boolean minimumPerson;
        private boolean minimumOrganisation;
        private boolean minimumOffence;
        private boolean reportingRestrictions;
        private List<CourtApplication> courtApplications;

        private Map<UUID,UUID> seedingHearingIds;

        private Map<UUID, Map<UUID, List<UUID>>> structure = toMap(randomUUID(), toMap(randomUUID(), asList(randomUUID())));

        public CoreTemplateArguments setJurisdictionType(JurisdictionType jurisdictionType) {
            this.jurisdictionType = jurisdictionType;
            return this;
        }

        public CoreTemplateArguments setHearingLanguage(HearingLanguage hearingLanguage) {
            this.hearingLanguage = hearingLanguage;
            return this;
        }

        public CoreTemplateArguments setReportingRestrictions(final boolean reportingRestrictions) {
            this.reportingRestrictions = reportingRestrictions;
            return this;
        }

        public CoreTemplateArguments setMinimumAssociatedPerson(boolean minimumAssociatedPerson) {
            this.minimumAssociatedPerson = minimumAssociatedPerson;
            return this;
        }

        public CoreTemplateArguments setMinimumDefenceOrganisation(boolean minimumDefenceOrganisation) {
            this.minimumDefenceOrganisation = minimumDefenceOrganisation;
            return this;
        }

        public CoreTemplateArguments setMinimumPerson(boolean minimumPerson) {
            this.minimumPerson = minimumPerson;
            return this;
        }

        public CoreTemplateArguments setMinimumOrganisation(boolean minimumOrganisation) {
            this.minimumOrganisation = minimumOrganisation;
            return this;
        }

        public CoreTemplateArguments setMinimumOffence(boolean minimumOffence) {
            this.minimumOffence = minimumOffence;
            return this;
        }

        public CoreTemplateArguments setConvicted(boolean convicted) {
            this.convicted = convicted;
            return this;
        }

        @SuppressWarnings("squid:S2384")
        public CoreTemplateArguments setCourtApplication(List<CourtApplication> courtApplications) {
            this.courtApplications = courtApplications;
            return this;
        }

        public CoreTemplateArguments setSeedingHearingIds(Map<UUID,UUID> seedingHearingIds){
            this.seedingHearingIds = seedingHearingIds;
            return this;
        }

        public boolean isMinimumAssociatedPerson() {
            return minimumAssociatedPerson;
        }

        public boolean isMinimumDefenceOrganisation() {
            return minimumDefenceOrganisation;
        }

        public boolean isMinimumPerson() {
            return minimumPerson;
        }

        public boolean isMinimumOrganisation() {
            return minimumOrganisation;
        }

        public boolean isMinimumOffence() {
            return minimumOffence;
        }

        public CoreTemplateArguments setStructure(Map<UUID, Map<UUID, List<UUID>>> structure) {
            this.structure = structure;
            return this;
        }

        public static <T, U> Map<T, U> toMap(T t, U u) {
            final Map<T, U> map = new HashMap<>();
            map.put(t, u);
            return map;
        }

        public static <T, U> Map<T, U> toMap(List<Pair<T, U>> pairs) {
            return pairs.stream().collect(Collectors.toMap(Pair::getK, Pair::getV));
        }
    }

    public static CoreTemplateArguments defaultArguments() {
        return new CoreTemplateArguments();
    }

    public static ProsecutionCaseIdentifier.Builder prosecutionCaseIdentifier(CoreTemplateArguments args) {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityReference(args.jurisdictionType == JurisdictionType.MAGISTRATES ? STRING.next() : null)
                .withProsecutionAuthorityCode(STRING.next())
                .withCaseURN(args.jurisdictionType == JurisdictionType.CROWN ? STRING.next() : null);
    }

    public static AssociatedPerson.Builder associatedPerson(CoreTemplateArguments args) {
        return AssociatedPerson.associatedPerson()
                .withPerson(person(args).build())
                .withRole(STRING.next());
    }

    public static String generateRandomEmail() {
        return STRING.next().toLowerCase() + "@" + STRING.next().toLowerCase() + "." + STRING.next().toLowerCase();
    }

    public static Address.Builder address() {
        return Address.address()
                .withAddress1(STRING.next())
                .withAddress2((STRING.next()))
                .withAddress3((STRING.next()))
                .withAddress4((STRING.next()))
                .withAddress5((STRING.next()))
                .withPostcode((POST_CODE.next()));
    }

    public static ContactNumber.Builder contactNumber() {
        return ContactNumber.contactNumber()
                .withFax((INTEGER.next().toString()))
                .withHome((INTEGER.next().toString()))
                .withMobile((INTEGER.next().toString()))
                .withPrimaryEmail((generateRandomEmail()))
                .withSecondaryEmail((generateRandomEmail()))
                .withWork((INTEGER.next().toString()));
    }

    public static Person.Builder person(CoreTemplateArguments args) {

        if (args.isMinimumPerson()) {
            return Person.person()
                    .withTitle("DR")
                    .withLastName(STRING.next())
                    .withGender(RandomGenerator.values(uk.gov.justice.core.courts.Gender.values()).next());
        }

        return Person.person()
                .withTitle("DR")
                .withContact((contactNumber().build()))
                .withAdditionalNationalityCode((STRING.next()))
                .withAdditionalNationalityId((randomUUID()))
                .withDateOfBirth((PAST_LOCAL_DATE.next()))
                .withDisabilityStatus((STRING.next()))
                .withDocumentationLanguageNeeds((args.hearingLanguage == HearingLanguage.WELSH ? HearingLanguage.WELSH : HearingLanguage.ENGLISH))
                .withEthnicity(Ethnicity.ethnicity()
                        .withSelfDefinedEthnicityId(randomUUID())
                        .withSelfDefinedEthnicityDescription(STRING.next())
                        .build())
                .withAddress((address().build()))
                .withFirstName((STRING.next()))
                .withMiddleName((STRING.next()))
                .withLastName(STRING.next())
                .withGender(RandomGenerator.values(Gender.values()).next())
                .withOccupation((STRING.next()))
                .withInterpreterLanguageNeeds((STRING.next()))
                .withOccupationCode((STRING.next()))
                .withSpecificRequirements((STRING.next()))
                .withNationalInsuranceNumber((NI_NUMBER.next()))
                .withNationalityId((randomUUID()))
                .withNationalityCode((STRING.next()));
    }

    public static AllocationDecision.Builder allocationDecision(final UUID offenceId) {
        return AllocationDecision.allocationDecision()
                .withOffenceId(offenceId)
                .withMotReasonDescription("Defendant chooses trial by jury")
                .withOriginatingHearingId(randomUUID())
                .withAllocationDecisionDate(PAST_LOCAL_DATE.next())
                .withMotReasonId(fromString("f8eb278a-8bce-373e-b365-b45e939da38a"))
                .withSequenceNumber(40)
                .withMotReasonCode("4");
    }

    public static NotifiedPlea.Builder notifiedPlea(UUID offenceId) {
        return NotifiedPlea.notifiedPlea()
                .withOffenceId(offenceId)
                .withNotifiedPleaValue(RandomGenerator.values(NotifiedPleaValue.values()).next())
                .withNotifiedPleaDate(PAST_LOCAL_DATE.next());
    }

    public static IndicatedPlea.Builder indicatedPlea(UUID offenceId) {
        return IndicatedPlea.indicatedPlea()
                .withOffenceId(offenceId)
                .withIndicatedPleaDate(PAST_LOCAL_DATE.next())
                .withIndicatedPleaValue(RandomGenerator.values(IndicatedPleaValue.values()).next())
                .withSource(RandomGenerator.values(Source.values()).next());
    }


    public static OffenceFacts.Builder offenceFacts() {
        return OffenceFacts.offenceFacts()
                .withAlcoholReadingAmount(INTEGER.next())
                .withAlcoholReadingMethodCode((STRING.next()))
                .withVehicleRegistration((STRING.next()));
    }

    public static Offence.Builder offenceWithReportingRestrictions(CoreTemplateArguments args, UUID offenceId, List<ReportingRestriction> reportingRestrictions) {
        final Offence.Builder offencebuilder = offence(args, offenceId);
        offencebuilder.withReportingRestrictions(reportingRestrictions);
        if(nonNull(args.seedingHearingIds) && !args.seedingHearingIds.isEmpty()){
            final UUID seedingHearingId = args.seedingHearingIds.get(offenceId);
            if(nonNull(seedingHearingId)) {
                offencebuilder.withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .build());
            }
        }
        return offencebuilder;
    }

    public static Offence.Builder offence(CoreTemplateArguments args, UUID offenceId) {

        if (args.isMinimumOffence()) {
            return Offence.offence()
                    .withId(offenceId)
                    .withStartDate(PAST_LOCAL_DATE.next())
                    .withOffenceDefinitionId(randomUUID())
                    .withOffenceCode(STRING.next())
                    .withCount(INTEGER.next())
                    .withWording(STRING.next())
                    .withOrderIndex((INTEGER.next()));
        }

        final Offence.Builder result = Offence.offence()
                .withId(offenceId)
                .withStartDate(PAST_LOCAL_DATE.next())
                .withEndDate((PAST_LOCAL_DATE.next()))
                .withArrestDate((PAST_LOCAL_DATE.next()))
                .withChargeDate((PAST_LOCAL_DATE.next()))

                .withIndicatedPlea((indicatedPlea(offenceId).build()))
                .withNotifiedPlea((notifiedPlea(offenceId).build()))
                .withAllocationDecision(allocationDecision(offenceId).build())

                .withOffenceDefinitionId(randomUUID())
                .withOffenceTitle(STRING.next())
                .withOffenceTitleWelsh((STRING.next()))
                .withOffenceCode(STRING.next())
                .withCount(INTEGER.next())
                .withOffenceFacts((offenceFacts().build()))
                .withOffenceLegislation((STRING.next()))
                .withOffenceLegislationWelsh((STRING.next()))
                .withWording(STRING.next())
                .withWordingWelsh((STRING.next()))
                .withModeOfTrial((STRING.next()))
                .withOrderIndex((INTEGER.next()));

        if (args.jurisdictionType == JurisdictionType.MAGISTRATES) {
            final LocalDate convictionDate = PAST_LOCAL_DATE.next();
            result.withConvictionDate((convictionDate));
        }

        if(args.convicted) {
            final LocalDate convictionDate = PAST_LOCAL_DATE.next();
            result.withConvictionDate((convictionDate));
        }
        if( nonNull(args.seedingHearingIds) && !args.seedingHearingIds.isEmpty()){
            final UUID seedingHearingId = args.seedingHearingIds.get(offenceId);
            if(nonNull(seedingHearingId)) {
                result.withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .build());
            }
        }
        return result;
    }

    public static Organisation.Builder organisation(CoreTemplateArguments args) {

        if (args.isMinimumOrganisation()) {
            return Organisation.organisation()
                    .withName(STRING.next());
        }

        return Organisation.organisation()
                .withAddress((address().build()))
                .withContact((contactNumber().build()))
                .withIncorporationNumber((STRING.next()))
                .withName(STRING.next())
                .withRegisteredCharityNumber((STRING.next()));
    }

    public static PersonDefendant.Builder personDefendant(CoreTemplateArguments args) {
        return PersonDefendant.personDefendant()
                .withPersonDetails(person(args).build())
                .withArrestSummonsNumber((STRING.next()))
                .withBailStatus(bailStatus().withId(randomUUID()).withDescription("Remanded into Custody").withCode("C").build())
                .withDriverNumber((STRING.next()))
                .withPerceivedBirthYear((INTEGER.next()))

                .withPersonDetails(Person.person()
                        .withEthnicity(Ethnicity.ethnicity()
                                .withSelfDefinedEthnicityId(randomUUID())
                                .withSelfDefinedEthnicityDescription(STRING.next())
                                .build()).build())


                .withEmployerOrganisation((organisation(args).build()))
                .withEmployerPayrollReference((STRING.next()))

                .withCustodyTimeLimit((PAST_LOCAL_DATE.next()));
    }

    public static LegalEntityDefendant.Builder legalEntityDefendant(CoreTemplateArguments args) {
        return LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(organisation(args).build());
    }

    public static Defendant.Builder defendant(UUID prosecutionCaseId, CoreTemplateArguments args, Pair<UUID, List<UUID>> structure) {

        return Defendant.defendant()
                .withId(structure.getK())
                .withProsecutionCaseId(prosecutionCaseId)
                .withNumberOfPreviousConvictionsCited((INTEGER.next()))
                .withProsecutionAuthorityReference((STRING.next()))
                .withOffences(
                        structure.getV().stream()
                                .map(offenceId -> args.reportingRestrictions ? offenceWithReportingRestrictions(args, offenceId, getDefaultReportingRestrictions()).build() : offence(args, offenceId).build())
                                .collect(toList())
                )
                .withAssociatedPersons(args.isMinimumAssociatedPerson() ? asList(associatedPerson(args).build()) : null)
                .withDefenceOrganisation((args.isMinimumDefenceOrganisation() ? organisation(args).build() : null))
                .withPersonDefendant((args.defendantType == DefendantType.PERSON ? personDefendant(args).build() : null))
                .withLegalEntityDefendant((args.defendantType == DefendantType.ORGANISATION ? legalEntityDefendant(args).build() : null))
                .withAliases(asList(DefendantAlias.defendantAlias().withLastName(STRING.next()).build()))
                .withPncId((STRING.next()));
    }

    private static List<ReportingRestriction> getDefaultReportingRestrictions() {
        return Stream.of(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build(),
                ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(SEXUAL_OFFENCE_RR_DESCRIPTION).withOrderedDate(LocalDate.now()).build()
        ).collect(toList());
    }

    public static ProsecutionCase.Builder prosecutionCase(CoreTemplateArguments args, Pair<UUID, Map<UUID, List<UUID>>> structure) {

        return ProsecutionCase.prosecutionCase()
                .withId(structure.getK())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier(args).build())
                .withCaseStatus((STRING.next()))
                .withOriginatingOrganisation((STRING.next()))
                .withInitiationCode(RandomGenerator.values(InitiationCode.values()).next())
                .withStatementOfFacts((STRING.next()))
                .withStatementOfFactsWelsh((STRING.next()))
                .withDefendants(
                        structure.getV().entrySet().stream()
                                .map(entry -> defendant(structure.getK(), args, Pair.p(entry.getKey(), entry.getValue())).build())
                                .collect(toList())
                );
    }


    public static JudicialRole.Builder judiciaryRole(CoreTemplateArguments args) {
        return JudicialRole.judicialRole()
                .withJudicialId(randomUUID())
                .withFirstName((STRING.next()))
                .withMiddleName((STRING.next()))
                .withLastName((STRING.next()))
                .withIsBenchChairman((BOOLEAN.next()))
                .withIsDeputy((BOOLEAN.next()))
                .withTitle((STRING.next()))
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudicialRoleTypeId(randomUUID())
                                .withJudiciaryType(args.jurisdictionType == JurisdictionType.CROWN ? "Circuit Judge" : "Magistrate")
                                .build()
                );
    }

    public static HearingDay.Builder hearingDay() {
        return HearingDay.hearingDay()
                .withSittingDay(RandomGenerator.PAST_UTC_DATE_TIME.next())
                .withListingSequence((INTEGER.next()))
                .withListedDurationMinutes(INTEGER.next());
    }

    public static ReferralReason.Builder referralReason() {
        return ReferralReason.referralReason()
                .withId(randomUUID())
                .withDefendantId(randomUUID())
                .withDescription(STRING.next());
    }

    public static HearingType.Builder hearingType() {
        return HearingType.hearingType()
                .withId(UUID.randomUUID())
                .withDescription("HearingType");
    }

    public static CourtCentre.Builder courtCentre() {
        return CourtCentre.courtCentre()
                .withId(UUID.randomUUID());
    }


    public static Hearing.Builder hearing(CoreTemplateArguments args) {

        final Hearing.Builder hearingBuilder = Hearing.hearing()
                .withId(randomUUID())
                .withType(hearingType().build())
                .withHearingLanguage((HearingLanguage.WELSH))
                .withJurisdictionType(args.jurisdictionType)
                .withReportingRestrictionReason((STRING.next()))
                .withHearingDays(Stream.of(hearingDay().build()).collect(toList()))
                .withCourtCentre(courtCentre().build())
                .withJudiciary(singletonList(judiciaryRole(args).build()))
                .withDefendantReferralReasons(singletonList(referralReason().build()))
                .withHasSharedResults((false))
                .withProsecutionCases(
                        args.structure.entrySet().stream()
                                .map(entry -> prosecutionCase(args, Pair.p(entry.getKey(), entry.getValue())).build())
                                .collect(toList())
                )
                .withCourtApplications(args.courtApplications);

        if (args.hearingLanguage == HearingLanguage.WELSH) {
            hearingBuilder.withHearingLanguage((HearingLanguage.WELSH));
        } else {
            hearingBuilder.withHearingLanguage((HearingLanguage.ENGLISH));
        }
        return hearingBuilder;
    }

    public static Hearing.Builder hearingForApplication(CoreTemplateArguments args) {

        final Hearing.Builder hearingBuilder = Hearing.hearing()
                .withId(randomUUID())
                .withType(hearingType().build())
                .withHearingLanguage((HearingLanguage.WELSH))
                .withJurisdictionType(args.jurisdictionType)
                .withReportingRestrictionReason((STRING.next()))
                .withHearingDays(Stream.of(hearingDay().build()).collect(toList()))
                .withCourtCentre(courtCentre().build())
                .withJudiciary(singletonList(judiciaryRole(args).build()))
                .withDefendantReferralReasons(singletonList(referralReason().build()))
                .withHasSharedResults((false))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(UUID.randomUUID())
                        .build()));

        if (args.hearingLanguage == HearingLanguage.WELSH) {
            hearingBuilder.withHearingLanguage((HearingLanguage.WELSH));
        } else {
            hearingBuilder.withHearingLanguage((HearingLanguage.ENGLISH));
        }
        return hearingBuilder;
    }

}
