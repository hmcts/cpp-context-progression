package uk.gov.moj.cpp.progression.test;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static uk.gov.justice.core.courts.ApplicationStatus.DRAFT;
import static uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S1168")
public class TestHelper {

    private static final String MC_80527 = "MC80527";
    public static final String APPEARANCE_TO_MAKE_STATUTORY_DECLARATION = "Appearance to make statutory declaration";

    private TestHelper(){
        throw new IllegalStateException("Utility class");
    }

    public static CourtApplication buildCourtapplication(final UUID courtApplicationId, final LocalDate convictionDate) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION)
                        .withCode(MC_80527)
                        .build())
                .withConvictionDate(convictionDate)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .build();
    }

    public static CourtApplication buildCourtapplicationWithCustodialEstablisment(final UUID courtApplicationId, final LocalDate convictionDate, final UUID masterDefendantId){
        final CourtApplicationParty applicationParty = CourtApplicationParty.courtApplicationParty()
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .withMasterDefendant(masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("John")
                                        .withLastName("Smith")
                                        .withDateOfBirth(LocalDate.of(1988,6,11))
                                        .build())
                                .withCustodialEstablishment(custodialEstablishment()
                                        .withName("John Smith")
                                        .withCustody("10 years prison")
                                        .build())
                                .build())
                        .build())
                .build();
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION)
                        .withCode(MC_80527)
                        .build())
                .withConvictionDate(convictionDate)
                .withApplicant(applicationParty)
                .withSubject(applicationParty)
                .withCourtApplicationCases(asList(CourtApplicationCase.courtApplicationCase().withCaseStatus("INACTIVE").build()))
                .withApplicationStatus(DRAFT)
                .build();
    }

    public static CourtApplication buildCourtapplicationWithOffenceUnderCase(final UUID courtApplicationId, final UUID offenceId, final LocalDate convictionDate, final boolean hasCourtApplicationCases, final boolean hasCourtOrder) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withLinkType(LinkType.FIRST_HEARING)
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .withCourtApplicationCases(getCourtApplicationCases(offenceId, convictionDate, hasCourtApplicationCases))
                .withCourtOrder(getCourtOrder(offenceId, convictionDate, hasCourtOrder))
                .build();
    }

    public static CourtApplication buildCourtApplicationWithCustody(final UUID courtApplicationId, final UUID masterDefendantId, final LocalDate convictionDate, final String custody){
        final CourtApplicationParty applicationParty = CourtApplicationParty.courtApplicationParty()
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .withMasterDefendant(masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("John")
                                        .withLastName("Smith")
                                        .withDateOfBirth(LocalDate.of(1988,6,11))
                                        .build())
                                .withCustodialEstablishment(custodialEstablishment()
                                        .withName("John Smith")
                                        .withCustody(custody)
                                        .build())
                                .build())
                        .build())
                .build();
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION)
                        .withCode(MC_80527)
                        .build())
                .withConvictionDate(convictionDate)
                .withApplicant(applicationParty)
                .withSubject(applicationParty)
                .withCourtApplicationCases(asList(CourtApplicationCase.courtApplicationCase().withCaseStatus("INACTIVE").build()))
                .withApplicationStatus(DRAFT)
                .build();
    }

    public static CourtApplication buildCourtapplicationWithOffenceUnderCase(final UUID courtApplicationId, final UUID offenceId, final LocalDate convictionDate) {
        return buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, convictionDate, true, false);
    }

    private static CourtOrder getCourtOrder(final UUID offenceId, final LocalDate convictionDate, final boolean hasCourtOrder) {
        if (!hasCourtOrder) {
            return null;
        }
        return buildCourtOrder(offenceId, convictionDate);
    }

    private static List<CourtApplicationCase> getCourtApplicationCases(final UUID offenceId, final LocalDate convictionDate, final boolean hasCourtApplicationCases) {
        if (!hasCourtApplicationCases) {
            return null;
        }
        return singletonList(buildCourtApplicationCase(offenceId, convictionDate));
    }

    public static CourtApplication buildCourtapplicationWithOffenceUnderCourtOrder(final UUID courtApplicationId, final UUID offenceId, final LocalDate convictionDate) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withLinkType(LinkType.STANDALONE)
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .withCourtOrder(buildCourtOrder(offenceId, convictionDate))
                .build();
    }

    public static CourtApplicationCase buildCourtApplicationCase(final UUID offenceId, final LocalDate convictionDate) {
        return CourtApplicationCase.courtApplicationCase()
                .withIsSJP(false)
                .withCaseStatus("ACTIVE")
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                .withOffences(singletonList(buildOffence(offenceId, convictionDate)))
                .build();
    }

    public static CourtOrder buildCourtOrder(final UUID offenceId, final LocalDate convictionDate) {
        return CourtOrder.courtOrder()
                .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                        .withOffence(buildOffence(offenceId, convictionDate))
                        .build()))
                .build();
    }

    public static Offence buildOffence(final UUID offenceId, final LocalDate convictionDate) {
        return Offence.offence()
                .withId(offenceId)
                .withConvictionDate(convictionDate)
                .build();
    }
}
