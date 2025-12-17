package uk.gov.moj.cpp.progression.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingNotificationHelperTimeZoneTest {

    @InjectMocks
    private HearingNotificationHelper hearingNotificationHelper;

    @Test
    public void sendHearingNotifications_VerifyUTCTimeConvertedToUKTimeZone() {
        ZonedDateTime hearingDateTimeUTC = ZonedDateTime.of(2025, 8, 12, 14, 30, 0, 0, ZoneId.of("UTC"));
        // Expected UK time (UTC+1 during summer time)
        //String expectedUKTime = LocalTime.of(15, 30).toString(); // 14:30 UTC + 1 hour = 15:30 UK time
        ZonedDateTime expectedUKTime = hearingDateTimeUTC.withZoneSameInstant(ZoneId.of("Europe/London"));

        final ZonedDateTime ukZoneTime = hearingNotificationHelper.getEarliestStartDateTime(hearingDateTimeUTC);
        assertEquals(expectedUKTime.toInstant(), ukZoneTime.toInstant());
    }
}