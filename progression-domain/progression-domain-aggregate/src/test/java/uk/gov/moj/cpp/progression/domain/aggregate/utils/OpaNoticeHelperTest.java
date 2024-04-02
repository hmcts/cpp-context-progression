package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OnlinePleasAllocation;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.moj.cpp.progression.plea.json.schemas.OffencePleaDetails;
import uk.gov.moj.cpp.progression.plea.json.schemas.OpaNoticeDocument;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OpaNoticeHelperTest {

    final static UUID caseId = randomUUID();
    final static UUID defendantId = randomUUID();
    final static UUID offenceId = randomUUID();
    final static ZonedDateTime hearingDate = ZonedDateTime.now().plusDays(4);

    @Test
    public void shouldGenerateOpaPublicListNotice() {
        final Hearing hearing = createHearing(randomUUID(), false);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Prosecutor prosecutor = prosecutionCase.getProsecutor();

        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaPublicListNotice(caseId, defendantId, hearing, prosecutor);

        assertThat(document.getCaseUrn(), is("UB112786 RN"));
        assertThat(document.getFirstName(), is("Adam"));
        assertThat(document.getMiddleName(), is("Middle"));
        assertThat(document.getLastName(), is("Christ"));
        assertThat(document.getOffences().get(0).getTitle(), is("Offence title"));
        assertThat(document.getOffences().get(0).getLegislation(), is("This section"));
        assertThat(document.getFirstHearingDate(), is(hearingDate));
        assertThat(document.getOffences().get(0).getReportRestrictions(), is("The dependant is very high profile"));
        assertThat(document.getProsecutor(), is("TFL Underground"));
    }
    @Test
    public void shouldGenerateOpaPublicListNoticeWelsh() {
        final Hearing hearing = createHearing(randomUUID(), true);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Prosecutor prosecutor = prosecutionCase.getProsecutor();
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaPublicListNotice(caseId, defendantId, hearing, prosecutor);

        assertThat(document.getCaseUrn(), is("UB112786 RN"));
        assertThat(document.getFirstName(), is("Adam"));
        assertThat(document.getMiddleName(), is("Middle"));
        assertThat(document.getLastName(), is("Christ"));
        assertThat(document.getOffences().get(0).getTitle(), is("Offence welsh title"));
        assertThat(document.getOffences().get(0).getLegislation(), is("This welsh section"));
        assertThat(document.getFirstHearingDate(), is(hearingDate));
        assertThat(document.getOffences().get(0).getReportRestrictions(), is("The dependant is very high profile"));
        assertThat(document.getProsecutor(), is("TFL Underground"));
    }
    @Test
    public void shouldGenerateOpaPressListNotice() {
        final Hearing hearing = createHearing(randomUUID(), false);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Prosecutor prosecutor = prosecutionCase.getProsecutor();
        final OnlinePleasAllocation pleasAllocation = creatingOnlinePleasAllocation(offenceId);
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaPressListNotice(caseId, defendantId, hearing, prosecutor, pleasAllocation);

        assertThat(document.getCaseUrn(), is("UB112786 RN"));
        assertThat(document.getFirstName(), is("Adam"));
        assertThat(document.getMiddleName(), is("Middle"));
        assertThat(document.getLastName(), is("Christ"));
        assertThat(document.getOffences().get(0).getTitle(), is("Offence title"));
        assertThat(document.getOffences().get(0).getLegislation(), is("This section"));
        assertThat(document.getOffences().get(0).getWording(), is("This wording"));
        assertThat(document.getOffences().get(0).getIndicatedPlea(), is("GUITLY"));
        assertThat(document.getOffences().get(0).getPleaStartDate(), is(now()));
        assertThat(document.getFirstHearingDate(), is(hearingDate));
        assertThat(document.getOffences().get(0).getReportRestrictions(), is("The dependant is very high profile"));
        assertThat(document.getProsecutor(), is("TFL Underground"));
        assertThat(document.getAge(), is("30"));
        assertThat(document.getOrganisationName(), is("Tesco Ltd"));

        assertThat(document.getDefendantAddress().getAddress1(), is("24 Harvey Road"));
        assertThat(document.getDefendantAddress().getAddress2(), is("Abc Road"));
        assertThat(document.getDefendantAddress().getAddress3(), is("Barking"));
        assertThat(document.getDefendantAddress().getPostcode(), is("IG3 2KG"));
        assertThat(document.getYouth(), is("Youth"));
    }

    @Test
    public void shouldGenerateOpaPressListNoticeWelsh() {
        final Hearing hearing = createHearing(randomUUID(), true);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Prosecutor prosecutor = prosecutionCase.getProsecutor();
        final OnlinePleasAllocation pleasAllocation = creatingOnlinePleasAllocation(offenceId);
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaPressListNotice(caseId, defendantId, hearing, prosecutor, pleasAllocation);

        assertThat(document.getCaseUrn(), is("UB112786 RN"));
        assertThat(document.getFirstName(), is("Adam"));
        assertThat(document.getMiddleName(), is("Middle"));
        assertThat(document.getLastName(), is("Christ"));
        assertThat(document.getOffences().get(0).getTitle(), is("Offence welsh title"));
        assertThat(document.getOffences().get(0).getLegislation(), is("This welsh section"));
        assertThat(document.getOffences().get(0).getWording(), is("This welsh wording"));
        assertThat(document.getOffences().get(0).getIndicatedPlea(), is("GUITLY"));
        assertThat(document.getOffences().get(0).getPleaStartDate(), is(now()));
        assertThat(document.getFirstHearingDate(), is(hearingDate));
        assertThat(document.getOffences().get(0).getReportRestrictions(), is("The dependant is very high profile"));
        assertThat(document.getProsecutor(), is("TFL Underground"));
        assertThat(document.getAge(), is("30"));
        assertThat(document.getOrganisationName(), is("Tesco Ltd"));

        assertThat(document.getDefendantAddress().getAddress1(), is("24 Harvey Road"));
        assertThat(document.getDefendantAddress().getAddress2(), is("Abc Road"));
        assertThat(document.getDefendantAddress().getAddress3(), is("Barking"));
        assertThat(document.getDefendantAddress().getPostcode(), is("IG3 2KG"));
        assertThat(document.getYouth(), is("Youth"));
    }

    @Test
    public void shouldGenerateOpaResultListNotice() {
        final Hearing hearing = createHearing(randomUUID(), false);

        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaResultListNotice(caseId, defendantId, hearing);

        assertThat(document.getFirstName(), is("Adam"));
        assertThat(document.getMiddleName(), is("Middle"));
        assertThat(document.getLastName(), is("Christ"));
        assertThat(document.getCourt(), is("Liverpool Street Court"));
        assertThat(document.getCourtId(), is(notNullValue()));
        assertThat(document.getCourtAddress().getAddress1(), is("24 Harvey Road"));
        assertThat(document.getCourtAddress().getAddress2(), is("Abc Road"));
        assertThat(document.getCourtAddress().getAddress3(), is("Barking"));
        assertThat(document.getCourtAddress().getPostcode(), is("IG3 2KG"));
        assertThat(document.getOffences().get(0).getTitle(), is("Offence title"));
        assertThat(document.getOffences().get(0).getLegislation(), is("This section"));
        assertThat(document.getOffences().get(0).getDecisionDate(), is(now()));
        assertThat(document.getOffences().get(0).getAllocationDecision(), is("Ordered for jail term"));
        assertThat(document.getBailStatus(), is("Unconditional"));
        assertThat(document.getOffences().get(0).getNextHearingLocation(), is("Liverpool Street Court"));
        assertThat(document.getOffences().get(0).getNextHearingDate().toLocalDate(), is(now()));
        assertThat(document.getOffences().get(0).getReportRestrictions(), is("The dependant is very high profile"));
    }

    @Test
    public void shouldGenerateOpaResultListNoticeWelsh() {
        final Hearing hearing = createHearing(randomUUID(), true);
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaResultListNotice(caseId, defendantId, hearing);

        assertThat(document.getFirstName(), is("Adam"));
        assertThat(document.getMiddleName(), is("Middle"));
        assertThat(document.getLastName(), is("Christ"));
        assertThat(document.getCourt(), is("Welsh Liverpool Street Court"));
        assertThat(document.getCourtId(), is(notNullValue()));
        assertThat(document.getCourtAddress().getAddress1(), is("24 Welsh Road"));
        assertThat(document.getCourtAddress().getAddress2(), is("Abc Welsh Road"));
        assertThat(document.getCourtAddress().getAddress3(), is("Barking Welsh"));
        assertThat(document.getCourtAddress().getPostcode(), is("XX3 2KG"));
        assertThat(document.getOffences().get(0).getTitle(), is("Offence welsh title"));
        assertThat(document.getOffences().get(0).getLegislation(), is("This welsh section"));
        assertThat(document.getOffences().get(0).getDecisionDate(), is(now()));
        assertThat(document.getOffences().get(0).getAllocationDecision(), is("Ordered for jail term"));
        assertThat(document.getBailStatus(), is("Unconditional"));
        assertThat(document.getOffences().get(0).getNextHearingLocation(), is("Welsh Liverpool Street Court"));
        assertThat(document.getOffences().get(0).getNextHearingDate().toLocalDate(), is(now()));
        assertThat(document.getOffences().get(0).getReportRestrictions(), is("The dependant is very high profile"));
    }

    private static OnlinePleasAllocation creatingOnlinePleasAllocation(final UUID offenceId) {
        final OffencePleaDetails pleaDetails = OffencePleaDetails.offencePleaDetails()
                .withOffenceId(offenceId)
                .withIndicatedPlea("GUITLY")
                .withPleaDate(now())
                .build();

        return OnlinePleasAllocation.onlinePleasAllocation()
                .withOffences(singletonList(pleaDetails))
                .build();
    }

    private Hearing createHearing(final UUID offenceId2, final boolean welsh) {
        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(hearingDate).build();

        final ReportingRestriction restrictions = ReportingRestriction.reportingRestriction()
                .withLabel("The dependant is very high profile")
                .build();

        final AllocationDecision allocationDecision = AllocationDecision.allocationDecision()
                .withAllocationDecisionDate(now())
                .withMotReasonDescription("Ordered for jail term")
                .build();

        final Address address = Address.address()
                .withAddress1("24 Harvey Road")
                .withAddress2("Abc Road")
                .withAddress3("Barking")
                .withAddress4("")
                .withPostcode("IG3 2KG")
                .build();

        final Address welshAddress = Address.address()
                .withAddress1("24 Welsh Road")
                .withAddress2("Abc Welsh Road")
                .withAddress3("Barking Welsh")
                .withAddress4("")
                .withPostcode("XX3 2KG")
                .build();

        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withName("Liverpool Street Court")
                .withWelshName("Welsh Liverpool Street Court")
                .withId(randomUUID())
                .withAddress(address)
                .withWelshAddress(welshAddress)
                .withWelshCourtCentre(welsh)
                .build();

        final Organisation org =  Organisation.organisation().withName("Tesco Ltd")
                .withAddress(address).build();

        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant().withOrganisation(org).build();

        final NextHearing hearing = NextHearing.nextHearing()
                .withCourtCentre(courtCentre)
                .withListedStartDateTime(ZonedDateTime.now())
                .build();

        final JudicialResult result = JudicialResult.judicialResult().withNextHearing(hearing).build();

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withWording("This wording")
                .withWordingWelsh("This welsh wording")
                .withOffenceTitle("Offence title")
                .withOffenceTitleWelsh("Offence welsh title")
                .withOffenceLegislation("This section")
                .withOffenceLegislationWelsh("This welsh section")
                .withStartDate(now())
                .withReportingRestrictions(singletonList(restrictions))
                .withJudicialResults(singletonList(result))
                .withAllocationDecision(allocationDecision)
                .withConvictingCourt(courtCentre)
                .withId(offenceId)
                .build());

        offences.add(Offence.offence()
                .withId(offenceId2)
                .build());

        final Person person = Person.person()
                .withFirstName("Adam")
                .withMiddleName("Middle")
                .withLastName("Christ")
                .withDateOfBirth(now().minusYears(30))
                .withAddress(address)
                .build();

        final BailStatus bailStatus = BailStatus.bailStatus().withCode("U").build();

        final PersonDefendant personDefendant = PersonDefendant.personDefendant()
                .withPersonDetails(person)
                .withBailStatus(bailStatus)
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withPersonDefendant(personDefendant)
                        .withLegalEntityDefendant(legalEntityDefendant)
                .withId(defendantId)
                .withOffences(offences)
                .withIsYouth(true)
                .build());

        final Prosecutor prosecutor = Prosecutor.prosecutor()
                .withProsecutorName("TFL Underground")
                .build();

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withCaseURN("UB112786 RN")
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withDefendants(defendants)
                .withProsecutor(prosecutor)
                .build();

        return Hearing.hearing()
                .withProsecutionCases(singletonList(prosecutionCase))
                .withCourtCentre(courtCentre)
                .withHearingDays(singletonList(hearingDay))
                .build();
    }
}
