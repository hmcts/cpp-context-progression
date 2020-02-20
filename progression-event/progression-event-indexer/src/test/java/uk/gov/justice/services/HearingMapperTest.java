package uk.gov.justice.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class HearingMapperTest {

    private HearingMapper hearingMapper;
    private List<HearingDay> defaultHearingDays;
    private CourtCentre defaultCourtCentre;
    private List<String> defaultDefendantIds;

    @Before
    public void before() {
        hearingMapper = new HearingMapper();
        defaultHearingDays = Collections.emptyList();
        defaultCourtCentre = CourtCentre.courtCentre().build();
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

        final uk.gov.justice.services.unifiedsearch.client.domain.Hearing actual = hearingMapper.hearing(prosecutionCaseDefendantListingStatusChanged, defaultDefendantIds);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.isIsBoxHearing(), is(false));
    }
}