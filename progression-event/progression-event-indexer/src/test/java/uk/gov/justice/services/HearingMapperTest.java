package uk.gov.justice.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingMapperTest {

    private static String COURT_CODE = "COURT CENTRE CODE 1";
    private HearingMapper hearingMapper;
    private List<HearingDay> defaultHearingDays;
    private CourtCentre defaultCourtCentre;
    private List<String> defaultDefendantIds;

    @BeforeEach
    public void before() {
        hearingMapper = new HearingMapper();
        defaultHearingDays = Collections.emptyList();
        defaultCourtCentre = CourtCentre.courtCentre().withCode(COURT_CODE).build();
        defaultDefendantIds = Collections.emptyList();
    }

    @Test
    public void shouldHandleNullIsBoxHearingFlag() {
        final Hearing hearing = Hearing
                .hearing()
                .withCourtCentre(defaultCourtCentre)
                .withIsBoxHearing(null)
                .withHearingDays(defaultHearingDays).build();

        final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = new ProsecutionCaseDefendantListingStatusChanged.Builder()
                .withHearing(hearing)
                .build();

        final uk.gov.justice.services.unifiedsearch.client.domain.Hearing actual = hearingMapper.hearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), defaultDefendantIds);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.isIsBoxHearing(), is(false));
        assertThat(actual.getCourtCentreCode(), is(COURT_CODE));
    }
}