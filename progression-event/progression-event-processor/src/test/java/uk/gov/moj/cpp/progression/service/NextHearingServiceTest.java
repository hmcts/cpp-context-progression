package uk.gov.moj.cpp.progression.service;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildDefendant;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJudicialResult;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildOffence;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildReportingRestriction;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.TestHelper;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NextHearingServiceTest {

    private static final UUID HEARING_ID_1 = fromString("dee1acd3-5c18-4417-9397-36c5257ac6b2");
    private static final UUID HEARING_ID_2 = fromString("09d5075a-c780-490d-976c-ef803487db2c");

    private static final UUID PROSECUTION_CASE_ID_1 = fromString("993ae52d-9922-4209-8312-60b42e6d662b");
    private static final UUID PROSECUTION_CASE_ID_2 = fromString("599be8ac-9def-44ff-a5d9-5a33830c1d03");

    private static final UUID DEFENDANT_ID_1 = fromString("11fedc85-ed52-49e4-b441-83ba91b6fd2a");
    private static final UUID DEFENDANT_ID_2 = fromString("d5921a8c-9acd-4f57-b62a-7ac6a4ea0f62");

    private static final UUID OFFENCE_ID_1 = fromString("a98e6235-9d4d-47f0-9c09-81a76dba3caf");
    private static final UUID OFFENCE_ID_2 = fromString("8e7b4098-2d4c-4ecf-b2d0-499bdb5889c2");

    private static final UUID REPORTING_RESTRICTION_ID_1 = fromString("6794cc13-e490-41a0-ba95-bf18590e37e6");

    @InjectMocks
    private NextHearingService service;

    @Spy
    private HearingResultHelper hearingResultHelper;

    @Mock
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForOneOffenceForOneDefendantInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant, DEFENDANT_ID_1, 1);

        final Offence offence = defendant.getOffences().get(0);
        assertOffence(offence, OFFENCE_ID_1);

        final ReportingRestriction reportingRestriction = offence.getReportingRestrictions().get(0);
        assertReportingRestriction(reportingRestriction, REPORTING_RESTRICTION_ID_1);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForTwoOffencesForOneDefendantInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(
                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))),
                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                )
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant, DEFENDANT_ID_1, 2);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForTwoOffencesForTheTwoDefendantsInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(
                                buildDefendant(DEFENDANT_ID_1,
                                        of(
                                                buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                        ).collect(Collectors.toList())),
                                buildDefendant(DEFENDANT_ID_2,
                                        of(
                                                buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                        ).collect(Collectors.toList()))
                        ).collect(Collectors.toList()))));

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 2);

        final Defendant defendant1 = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final Defendant defendant2 = prosecutionCase.getDefendants().get(1);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForTwoOffencesForTheTwoDefendantsInTwoProsecutionCase() {
        final Hearing hearing = buildHearing(
                of(
                        buildProsecutionCase(PROSECUTION_CASE_ID_1,
                                of(
                                        buildDefendant(DEFENDANT_ID_1,
                                                of(
                                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList())),
                        buildProsecutionCase(PROSECUTION_CASE_ID_2,
                                of(
                                        buildDefendant(DEFENDANT_ID_2,
                                                of(
                                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList()))
                ).collect(Collectors.toList())
        );

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 2);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_2, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds.getProsecutionCases().get(1);
        assertProsecutionCase(prosecutionCase2, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldReturnTwoHearingsWhenTwoNextHearingAvailableForTwoOffencesForOneDefendantInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(
                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))),
                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_2))))
                                )
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(2));

        final HearingListingNeeds hearingListingNeeds1 = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds1, HEARING_ID_2, 1);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds1.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_1, 1);

        final HearingListingNeeds hearingListingNeeds2 = nextHearingDetails.getHearingListingNeedsList().get(1);
        assertHearing(hearingListingNeeds2, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds2.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldReturnTwoHearingsWhenTwoNextHearingsAvailableForTwoOffencesForTheTwoDefendantsInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(
                                buildDefendant(DEFENDANT_ID_1,
                                        of(
                                                buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                        ).collect(Collectors.toList())),
                                buildDefendant(DEFENDANT_ID_2,
                                        of(
                                                buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_2))))
                                        ).collect(Collectors.toList()))
                        ).collect(Collectors.toList()))));

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(2));

        final HearingListingNeeds hearingListingNeeds1 = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds1, HEARING_ID_2, 1);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds1.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final HearingListingNeeds hearingListingNeeds2 = nextHearingDetails.getHearingListingNeedsList().get(1);
        assertHearing(hearingListingNeeds2, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds2.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase2, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);

    }

    @Test
    public void shouldReturnTwoHearingsWhenTwoNextHearingsAvailableForTwoOffencesForTheTwoDefendantsInTwoProsecutionCase() {
        final Hearing hearing = buildHearing(
                of(
                        buildProsecutionCase(PROSECUTION_CASE_ID_1,
                                of(
                                        buildDefendant(DEFENDANT_ID_1,
                                                of(
                                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList())),
                        buildProsecutionCase(PROSECUTION_CASE_ID_2,
                                of(
                                        buildDefendant(DEFENDANT_ID_2,
                                                of(
                                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_2))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList()))
                ).collect(Collectors.toList())
        );

        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(2));

        final HearingListingNeeds hearingListingNeeds1 = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds1, HEARING_ID_2, 1);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds1.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_2, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final HearingListingNeeds hearingListingNeeds2 = nextHearingDetails.getHearingListingNeedsList().get(1);
        assertHearing(hearingListingNeeds2, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds2.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase2, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldNotReturnHearingWhenNoNextHearingAvailableForOffences() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,null))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(0));
    }

    @Test
    public void shouldNotReturnHearingWhenOnlyDefendantJudicialResultsAreAvailable() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,null))
                                        .collect(Collectors.toList()), asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(0));
    }

    @Test
    public void shouldReturnConfirmedHearings() {
        final Map<UUID, Set<UUID>> defendantsMap = new HashMap<>();
        defendantsMap.put(DEFENDANT_ID_1, of(OFFENCE_ID_1).collect(Collectors.toSet()));

        final Map<UUID, Map<UUID, Set<UUID>>> prosecutionCasesMap = new HashMap<>();
        prosecutionCasesMap.put(PROSECUTION_CASE_ID_1, defendantsMap);

        final Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap = new HashMap<>();
        hearingsMap.put(HEARING_ID_1, prosecutionCasesMap);

        final Map<UUID, List<ConfirmedProsecutionCase>> confirmedHearings = service.getConfirmedHearings(hearingsMap);

        assertThat(confirmedHearings.size(), is(1));

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = confirmedHearings.get(HEARING_ID_1);

        assertThat(confirmedProsecutionCases, is(notNullValue()));
        assertThat(confirmedProsecutionCases.size(), is(1));

        final ConfirmedProsecutionCase confirmedProsecutionCase = confirmedProsecutionCases.get(0);
        assertThat(confirmedProsecutionCase, is(notNullValue()));
        assertThat(confirmedProsecutionCase.getId(), is(PROSECUTION_CASE_ID_1));
        assertThat(confirmedProsecutionCase.getDefendants().size(), is(1));

        final ConfirmedDefendant confirmedDefendant = confirmedProsecutionCase.getDefendants().get(0);
        assertThat(confirmedDefendant, is(notNullValue()));
        assertThat(confirmedDefendant.getId(), is(DEFENDANT_ID_1));
        assertThat(confirmedDefendant.getOffences().size(), is(1));

        final ConfirmedOffence confirmedOffence = confirmedDefendant.getOffences().get(0);
        assertThat(confirmedOffence, is(notNullValue()));
        assertThat(confirmedOffence.getId(), is(OFFENCE_ID_1));
    }

    @Test
    public void shouldPopulateCommittingCourt() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        final CommittingCourt committingCourt = TestHelper.buildCommittingCourt();
        final Optional<CommittingCourt> committingCourtOptional = Optional.of(committingCourt);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(committingCourtOptional);

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing, true, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant, DEFENDANT_ID_1, 1);

        assertThat(defendant.getOffences().size(), is(1));
        assertThat(defendant.getOffences().get(0).getCommittingCourt(), is(notNullValue()));
        assertThat(defendant.getOffences().get(0).getCommittingCourt().getCourtHouseCode(), is("CCCODE"));
        assertThat(defendant.getOffences().get(0).getCommittingCourt().getCourtHouseName(), is("Committing Court"));
        assertThat(defendant.getOffences().get(0).getCommittingCourt().getCourtHouseType(), is(JurisdictionType.MAGISTRATES));
    }

    private void assertHearing(final HearingListingNeeds hearingListingNeeds, final UUID hearingId, final int size) {
        assertThat(hearingListingNeeds.getId(), is(hearingId));
        assertThat(hearingListingNeeds.getProsecutionCases().size(), is(size));
    }

    private void assertProsecutionCase(final ProsecutionCase prosecutionCase, final UUID prosecutionCaseId, final int size) {
        assertThat(prosecutionCase.getId(), is(prosecutionCaseId));
        assertThat(prosecutionCase.getDefendants().size(), is(size));
        assertThat(prosecutionCase.getCpsOrganisation(), is("A01"));
    }

    private void assertDefendant(final Defendant defendant, final UUID defendantId, final int size) {
        assertThat(defendant.getId(), is(defendantId));
        assertThat(defendant.getOffences().size(), is(size));
    }

    private void assertOffence(final Offence offence, final UUID offenceId) {
        assertThat(offence.getId(), is(offenceId));
    }

    private void assertReportingRestriction(final ReportingRestriction reportingRestriction, final UUID reportingRestrictionId) {
        assertThat(reportingRestriction.getId(), is(reportingRestrictionId));
    }

    @Test
    public void shouldNotReturnDuplicateApplicationWhenBothProsectionCaseAndApplicationPresent() {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));

        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(CourtApplication.courtApplication()
                        .withId(PROSECUTION_CASE_ID_1)
                        .withJudicialResults(judicialResults)
                        .build()));

        final CommittingCourt committingCourt = TestHelper.buildCommittingCourt();
        final Optional<CommittingCourt> committingCourtOptional = Optional.of(committingCourt);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(committingCourtOptional);

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing, true, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));
    }

    @Test
    public void shouldNotReturnDuplicateApplicationWhenOnlyApplicationPresent() {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));

        final Hearing hearing = buildHearingWithCourtApplications(null,
                asList(CourtApplication.courtApplication()
                        .withId(PROSECUTION_CASE_ID_1)
                        .withJudicialResults(judicialResults)
                        .build()));

        final NextHearingDetails nextHearingDetails = service.getNextHearingDetails(hearing, true, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));
    }

    private Hearing buildHearingWithCourtApplications(final List<ProsecutionCase> prosecutionCases,
                                                      final List<CourtApplication> courtApplications) {
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCases)
                .withCourtApplications(courtApplications)
                .build();
    }
}
