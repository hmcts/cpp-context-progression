package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingUnscheduledListingHelperTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID HEARING_TYPE_ID = randomUUID();

    @Mock
    private ListingService listingService;

    @Mock
    private JsonEnvelope event;

    @Captor
    private ArgumentCaptor<ListUnscheduledCourtHearing> captor;

    @InjectMocks
    private HearingUnscheduledListingHelper helper;

    /**
     * Regression guard: the user-entered duration on the source hearing must be carried onto
     * the HearingUnscheduledListingNeeds posted to the listing context. Previously
     * estimatedMinutes was hardcoded to 0, which caused the listing-side fallback to substitute
     * the hearing-type default and lose the user's value. The Hearing POJO carries the user
     * value as the estimatedDuration String (e.g. "90 MINUTES"); we parse it into Integer
     * minutes for the downstream listing schema.
     */
    @Test
    public void shouldPropagateUserEnteredDurationOnHearingAsMinutes() {
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType()
                        .withId(HEARING_TYPE_ID)
                        .withDescription("Trial")
                        .build())
                .withEstimatedDuration("90 MINUTES")
                .build();

        helper.processUnscheduledHearings(event, hearing);

        verify(listingService).listUnscheduledHearings(eq(event), captor.capture());

        final Integer persistedDuration = captor.getValue().getHearings().get(0).getEstimatedMinutes();
        assertThat(persistedDuration, is(90));
    }

    @Test
    public void shouldConvertHoursToMinutesOnHearing() {
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType()
                        .withId(HEARING_TYPE_ID)
                        .withDescription("Trial")
                        .build())
                .withEstimatedDuration("2 HOURS")
                .build();

        helper.processUnscheduledHearings(event, hearing);

        verify(listingService).listUnscheduledHearings(eq(event), captor.capture());

        assertThat(captor.getValue().getHearings().get(0).getEstimatedMinutes(), is(120));
    }

    @Test
    public void shouldPassNullEstimatedMinutesThroughWhenSourceHearingHasNoDuration() {
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType()
                        .withId(HEARING_TYPE_ID)
                        .withDescription("Trial")
                        .build())
                .build();

        helper.processUnscheduledHearings(event, hearing);

        verify(listingService).listUnscheduledHearings(any(), captor.capture());

        // Null propagates through; the listing-side fallback (HearingDurationDefaults) will
        // substitute DEFAULT_MIN, so the SPRDT-806/807 "never 0 / never null" guarantee still holds.
        assertThat(captor.getValue().getHearings().get(0).getEstimatedMinutes(), is((Integer) null));
    }
}
