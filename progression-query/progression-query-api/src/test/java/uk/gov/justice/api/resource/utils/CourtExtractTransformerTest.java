package uk.gov.justice.api.resource.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurors;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PleaValue;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ResultPrompt;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class CourtExtractTransformerTest {

    private static final UUID CASE_ID = randomUUID();
    public static final String PROSECUTION_AUTHORITY_CODE = "TFL";
    public static final String GUILTY = "Guilty";
    public static final String PLEA_GUILTY_DESCRIPTION = "plea guilty description";
    private static final String COUNSELS_STATUS = "counsels status";
    public static final String FULL_NAME = "Jack Denial";
    public static final String ORGENISATION_NAME = "Liver pool defence";
    private static UUID DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();

    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-06-04T10:00:00.000Z";
    private static final String HEARING_DATE_3 = "2018-07-01T10:00:00.000Z";

    private static final String DEFENDANT_NAME = "Harry JackKane Junior";
    private static final String COURT_NAME = "liver pool";
    private static final String HEARING_TYPE = "Sentence";
    private static final String ADDRESS_1 = "22";
    private static final String ADDRESS_2 = "Acacia Avenue";
    private static final String ADDRESS_3 = "Acacia Town";
    private static final String POST_CODE = "CR7 0AA";
    private static final String PAR = "6225bd77";
    private static final String LABEL = "Fine";
    private static final String LEVEL = "OFFENCE";
    public static final String FIRST_NAME = "Jack";
    public static final String LAST_NAME = "Denial";
    public static final String FIXED_LIST_CODE = "Years";
    public static final String PROMPT_VALUE = "10";
    private static final String CONVICTION_DATE = "2018-04-04";
    public static final String PLEA_DATE = "2018-01-01";
    private static final String DEFENDANT_AGE = "30";
    public static final String DOB = "2010-01-01";

    @InjectMocks
    CourtExtractTransformer courtExtractTransformer;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Test
    public void testTransformToCourtExtract_shouldUseHearingWithLatestDate_whenExtractTypeIsCrownCourtExtractAndAllHearingsSelected() {

        String extractType = "CrownCourtExtract";
        //given
        GetCaseAtAGlance caseAtAGlance = createCaseAtAGlance();
        List<String> selectedHearingIds = Arrays.asList(HEARING_ID.toString(), HEARING_ID_2.toString());

        //when
        final CourtExtractRequested courtExtractRequested = courtExtractTransformer.getCourtExtractRequested(caseAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID());

        // then
        assertValues(courtExtractRequested, extractType, HEARING_DATE_2, HEARING_DATE_3);

    }

    @Test
    public void testTransformToCourtExtract_shouldUseSelectedHearing_whenExtractTypeIsCrownCourtExtractAndOneHearingsSelected() {
        String extractType = "CrownCourtExtract";
        //given
        GetCaseAtAGlance caseAtAGlance = createCaseAtAGlance();
        List<String> selectedHearingIds = Arrays.asList(HEARING_ID_2.toString());

        //when
        final CourtExtractRequested courtExtractRequested = courtExtractTransformer.getCourtExtractRequested(caseAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID());

        // then
        assertValues(courtExtractRequested, extractType, HEARING_DATE_2, HEARING_DATE_1);

    }

    @Test
    public void testTransformToCourtExtract_shouldUseLatestHearing_whenExtractTypeIsCertificateOfConviction() {

        String extractType = "CertificateOfConviction";

        //given
        GetCaseAtAGlance caseAtAGlance = createCaseAtAGlance();
        List<String> selectedHearingIds = new ArrayList<>();

        //when
        final CourtExtractRequested courtExtractRequested = courtExtractTransformer.getCourtExtractRequested(caseAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID());

        // then
        assertValues(courtExtractRequested, extractType, HEARING_DATE_2, HEARING_DATE_3);

    }

    private void assertValues(final CourtExtractRequested courtExtractRequested, final String extractType, final String hearingDate, final String hearingDate2) {
        assertThat(courtExtractRequested.getCaseReference(), is(PAR));
        assertThat(courtExtractRequested.getExtractType(), is(extractType));

        //Court
        assertThat(courtExtractRequested.getPublishingCourt().getName(), is(COURT_NAME));

        //Defendant
        assertThat(courtExtractRequested.getDefendant().getName(), is(DEFENDANT_NAME));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress3(), is(ADDRESS_3));
        assertThat(courtExtractRequested.getDefendant().getAddress().getPostcode(), is(POST_CODE));
        assertThat(courtExtractRequested.getDefendant().getAge(), is(DEFENDANT_AGE));
        assertThat(courtExtractRequested.getDefendant().getDateOfBirth(), is(DOB));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().get(0).getName(), is(FULL_NAME));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().get(0).getRole(), is(COUNSELS_STATUS));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getName(), is(ORGENISATION_NAME));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getContact().getFax(), is(("fax")));

        //Hearing
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getType(), is(HEARING_TYPE));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getHearingDays().get(0).getDay(), is((ZonedDateTimes.fromString(hearingDate).toString())));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getHearingDays().get(1).getDay(), is((ZonedDateTimes.fromString(hearingDate2).toString())));

        //court decision
        assertThat(courtExtractRequested.getCourtDecisions().get(0).getJudicialDisplayName(), is("Chair: Jack Denial Winger1: Jack Denial Winger2: Jack Denial"));
        assertThat(courtExtractRequested.getCourtDecisions().get(0).getRoleDisplayName(), is("District judge"));
        assertThat(courtExtractRequested.getCourtDecisions().get(0).getJudiciary().get(0).getName(), is(FULL_NAME));

        //results
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getIsAvailableForCourtExtract(), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getLabel(), is(LABEL));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getLevel(), is(LEVEL));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getPrompts().get(0).getLabel(), is(LABEL));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getPrompts().get(0).getFixedListCode(), is((FIXED_LIST_CODE)));
        assertThat(courtExtractRequested.getDefendant().getResults().get(0).getPrompts().get(0).getValue(), is(PROMPT_VALUE));

        //offences
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getConvictionDate(), is((CONVICTION_DATE)));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getIsAvailableForCourtExtract(), is(true));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getLabel(), is(LABEL));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getLevel(), is(LEVEL));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getPrompts().get(0).getLabel(), is(LABEL));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getPrompts().get(0).getFixedListCode(), is((FIXED_LIST_CODE)));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getPrompts().get(0).getValue(), is(PROMPT_VALUE));

        //Offence Plea
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getPleaDate(), is(PLEA_DATE));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getPleaValue(), is(PleaValue.GUILTY));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getIndicatedPlea().getIndicatedPleaValue(), is(IndicatedPleaValue.INDICATED_GUILTY));

        //Offence verdict
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getVerdictType().getCategory(), is((GUILTY)));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getVerdictType().getCategoryType(), is((GUILTY)));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getVerdictType().getDescription(), is((PLEA_GUILTY_DESCRIPTION)));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getJurors().getNumberOfJurors(), is(2));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getJurors().getNumberOfSplitJurors(), is(2));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getJurors().getUnanimous(), is(true));
    }

    private GetCaseAtAGlance createCaseAtAGlance() {
        GetCaseAtAGlance.Builder builder = GetCaseAtAGlance.getCaseAtAGlance().withId(CASE_ID);
        builder.withProsecutionCaseIdentifier(createPCIdentifier());
        builder.withDefendantHearings(createDefendantHearing());
        builder.withHearings(createHearings());
        return builder.build();
    }

    private List<Hearings> createHearings() {
        return Arrays.asList(
                Hearings.hearings()
                        .withId(HEARING_ID)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HEARING_TYPE)
                        .withDefendants(createDefendants(DEFENDANT_ID))
                        .build(),
                Hearings.hearings()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HEARING_TYPE)
                        .withDefendants(createDefendants(DEFENDANT_ID))
                        .build()
        );
    }

    private List<JudicialRole> createJudiciary() {
        return Arrays.asList(
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(true)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("District Judge")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Magistrate")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Circuit Judge")
                                        .build()
                        )
                        .build()
        );
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName(COURT_NAME)
                .build();
    }

    private List<Defendants> createDefendants(final UUID defendantId) {
        return Arrays.asList(
                Defendants.defendants()
                        .withId(defendantId)
                        .withAddress(createAddress())
                        .withDateOfBirth(DOB)
                        .withAge(DEFENDANT_AGE)
                        .withResults(createResults(defendantId))
                        .withOffences(createOffence(defendantId))
                        .withDefenceOrganisation(createDefenceOrganisation())
                        .build()
        );
    }

    private List<Offences> createOffence(final UUID defendantId) {
        return Arrays.asList(
                Offences.offences()
                        .withConvictionDate(CONVICTION_DATE)
                        .withResults(createResults(defendantId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withVerdicts(createVerdicts())
                        .build()
        );
    }

    private IndicatedPlea createIndicatedPlea() {
        return IndicatedPlea.indicatedPlea()
                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                .build();
    }

    private List<SharedResultLine> createResults(final UUID defendantId) {
        return Arrays.asList(
                SharedResultLine.sharedResultLine()
                        .withId(randomUUID())
                        .withProsecutionCaseId(CASE_ID)
                        .withDefendantId(defendantId)
                        .withIsAvailableForCourtExtract(true)
                        .withLabel(LABEL)
                        .withLevel(LEVEL)
                        .withPrompts(createPrompts())
                        .withDelegatedPowers(createDelegatedPower())
                        .build()
        );
    }

    private DelegatedPowers createDelegatedPower() {
        return DelegatedPowers.delegatedPowers()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .build();
    }

    private List<ResultPrompt> createPrompts() {
        return Arrays.asList(
                ResultPrompt.resultPrompt()
                        .withId(randomUUID())
                        .withLabel(LABEL)
                        .withIsAvailableForCourtExtract(true)
                        .withValue(PROMPT_VALUE)
                        .withFixedListCode(FIXED_LIST_CODE)
                        .build()
        );
    }

    private Address createAddress() {
        return Address.address()
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withPostcode(POST_CODE)
                .build();
    }

    private List<HearingDay> createHearingDays() {
        return Arrays.asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3))
                        .build()
        );
    }

    private List<HearingDay> createHearingDays2() {
        return Arrays.asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build()
        );
    }

    private List<DefendantHearings> createDefendantHearing() {
        List<DefendantHearings> defendantHearingsList = new ArrayList<>();
        List<UUID> hearingIds = new ArrayList<>();
        hearingIds.add(HEARING_ID);
        hearingIds.add(HEARING_ID_2);
        DefendantHearings defendantHearings = DefendantHearings.defendantHearings()
                .withDefendantId(DEFENDANT_ID)
                .withHearingIds(hearingIds)
                .withDefendantName(DEFENDANT_NAME)
                .build();
        defendantHearingsList.add(defendantHearings);
        return defendantHearingsList;
    }

    private ProsecutionCaseIdentifier createPCIdentifier() {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityReference(PAR)
                .build();
    }

    private List<Verdict> createVerdicts() {
        return Arrays.asList(Verdict.verdict()
                .withVerdictType(VerdictType.verdictType()
                        .withCategory(GUILTY)
                        .withCategoryType(GUILTY)
                        .withDescription(PLEA_GUILTY_DESCRIPTION)
                        .withSequence(1)
                        .build())
                .withVerdictDate("2018-02-02")
                .withJurors(Jurors.jurors()
                        .withNumberOfJurors(2)
                        .withNumberOfSplitJurors(2)
                        .withUnanimous(true)
                        .build())
                .withOffenceId(UUID.randomUUID())
                .build());
    }

    private List<Plea> createPlea() {
        return Arrays.asList(
                Plea.plea()
                        .withDelegatedPowers(DelegatedPowers.delegatedPowers()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .build())
                        .withPleaDate(PLEA_DATE)
                        .withPleaValue(PleaValue.GUILTY)
                        .build());
    }

    private DefenceOrganisation createDefenceOrganisation() {
        return
                DefenceOrganisation.defenceOrganisation().withDefenceOrganisation(
                        Organisation.organisation()
                                .withName(ORGENISATION_NAME)
                                .withAddress(createAddress())
                                .withContact(createContact())
                                .build()
                )
                        .withDefenceCounsels(createDefenceCounsels())
                        .withDefendantId(DEFENDANT_ID)
                        .build();
    }

    private List<DefenceCounsel> createDefenceCounsels() {
        return Arrays.asList(
                DefenceCounsel.defenceCounsel()
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withStatus(COUNSELS_STATUS)
                        .build()
        );
    }

    private ContactNumber createContact() {
        return ContactNumber.contactNumber()
                .withFax("fax")
                .withHome("home")
                .withMobile("mobile")
                .build();
    }

}
