package uk.gov.moj.cpp.progression.utils;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.progression.helper.TestHelper;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceToCommittingCourtConverterTest {

    private static final UUID CASE_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_1 = randomUUID();

    private static final UUID CASE_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();

    private static final String COURT_LOCATION = "CourtLocation";
    private static final LocalDate WEEK_COMMENCING_DATE_1 = LocalDate.now();

    private static final UUID HEARING_TYPE_1 = randomUUID();

    @InjectMocks
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    @Test
    public void shouldReturnCommittingCourtUsingCourtCentre() {

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withCode("CCCODE")
                .withName("Committing Court")
                .build();

        final Optional<CommittingCourt> committingCourtOptional = offenceToCommittingCourtConverter
                .convert(hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0), courtCentre, Boolean.TRUE);
        assertTrue(committingCourtOptional.isPresent());

        final CommittingCourt committingCourt = committingCourtOptional.get();

        assertThat(committingCourt.getCourtHouseCode(), is("CCCODE"));
        assertThat(committingCourt.getCourtHouseName(), is("Committing Court"));
        assertThat(committingCourt.getCourtHouseType(), is(JurisdictionType.MAGISTRATES));

    }

    @Test
    public void shouldNotReturnCommittingCourt() {
        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withCode("CCCode")
                .withName("Committing Court")
                .build();

        final Optional<CommittingCourt> committingCourtOptional = offenceToCommittingCourtConverter
                .convert(hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0), courtCentre, Boolean.FALSE);
        assertFalse(committingCourtOptional.isPresent());
    }
}
