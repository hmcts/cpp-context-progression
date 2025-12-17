package uk.gov.moj.cpp.progression.service;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import uk.gov.justice.core.courts.CourtApplication;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.NextHearing.nextHearing;
import static uk.gov.moj.cpp.progression.service.ListHearingBoxworkService.LHBW_RESULT_DEFINITION;

public class ListHearingBoxworkServiceTest {

    private final ListHearingBoxworkService listHearingBoxworkService = new ListHearingBoxworkService();

    @Test
    public void sendNotificationFalseWhenResultedWithNoLHBW() {

        CourtApplication courtApplication = courtApplication()
                .withJudicialResults(ImmutableList.of(judicialResult()
                        .withJudicialResultTypeId(UUID.randomUUID())
                        .withNextHearing(nextHearing().withCourtCentre(courtCentre().build()).build())
                        .build()))
                .build();

        assertThat(listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(courtApplication.getJudicialResults()), is(false));
    }

    @Test
    public void sendNotificationFalseWhenResultedWithLHBW_andNextHearingHasCourtRoom() {

        CourtApplication courtApplication = courtApplication()
                .withJudicialResults(ImmutableList.of(judicialResult()
                        .withJudicialResultTypeId(LHBW_RESULT_DEFINITION)
                        .withNextHearing(nextHearing().withCourtCentre(courtCentre().withRoomId(UUID.randomUUID()).build()).build())
                        .build()))
                .build();

        assertThat(listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(courtApplication.getJudicialResults()), is(false));
    }

    @Test
    public void sendNotificationTrueWhenResultedWithLHBW_andNextHearingWithFixedDateNoCourtRoom() {

        CourtApplication courtApplication = courtApplication()
                .withJudicialResults(ImmutableList.of(judicialResult()
                        .withJudicialResultTypeId(LHBW_RESULT_DEFINITION)
                        .withNextHearing(nextHearing()
                                .withListedStartDateTime(ZonedDateTime.now())
                                .withCourtCentre(courtCentre().build()).build())
                        .build()))
                .build();

        assertThat(listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(courtApplication.getJudicialResults()), is(true));
    }

    @Test
    public void sendNotificationTrueWhenResultedWithLHBW_andNextHearingWithWeekCommencingDate() {

        CourtApplication courtApplication = courtApplication()
                .withJudicialResults(ImmutableList.of(judicialResult()
                        .withJudicialResultTypeId(LHBW_RESULT_DEFINITION)
                        .withNextHearing(nextHearing()
                                .withWeekCommencingDate(LocalDate.now()).build())
                        .build()))
                .build();

        assertThat(listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(courtApplication.getJudicialResults()), is(true));
    }

}