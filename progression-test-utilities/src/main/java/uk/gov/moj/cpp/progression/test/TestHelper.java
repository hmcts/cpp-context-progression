package uk.gov.moj.cpp.progression.test;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.time.LocalDate;
import java.util.UUID;

public class TestHelper {

   private TestHelper(){
        throw new IllegalStateException("Utility class");
    }

    public static CourtApplication buildCourtapplication(final UUID courtApplicationId, final LocalDate convictionDate){
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType("Appearance to make statutory declaration")
                        .withCode("MC80527")
                        .build())
                .withConvictionDate(convictionDate)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .build();
    }

    public static CourtApplication buildCourtapplicationWithOffenceUnderCase(final UUID courtApplicationId, final UUID offenceId, final LocalDate convictionDate){
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
                .withCourtApplicationCases(singletonList(buildCourtApplicationCase(offenceId, convictionDate)))
                .build();
    }

    public static CourtApplication buildCourtapplicationWithOffenceUnderCourtOrder(final UUID courtApplicationId, final UUID offenceId,  final LocalDate convictionDate){
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

    public static CourtApplicationCase buildCourtApplicationCase(final UUID offenceId, final LocalDate convictionDate){
        return CourtApplicationCase.courtApplicationCase()
                .withIsSJP(false)
                .withCaseStatus("ACTIVE")
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                .withOffences(singletonList(buildOffence(offenceId, convictionDate)))
                .build();
    }

    public static CourtOrder buildCourtOrder(final UUID offenceId, final LocalDate convictionDate){
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
