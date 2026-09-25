package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.core.courts.TypeOfList;
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

    private static final UUID RESULT_DEFINITION_NHCCS = UUID.fromString("fbed768b-ee95-4434-87c8-e81cbc8d24c8");
    private static final UUID HEARING_ID = randomUUID();

    @InjectMocks
    private HearingUnscheduledListingHelper hearingUnscheduledListingHelper;

    @Mock
    private ListingService listingService;

    @Mock
    private JsonEnvelope event;

    @Captor
    private ArgumentCaptor<ListUnscheduledCourtHearing> listUnscheduledCourtHearingCaptor;

    @Test
    public void shouldListUnscheduledHearingWithDateAndTimeToBeFixedWhenTypeOfListNotProvided() {
        hearingUnscheduledListingHelper.processUnscheduledHearings(event, hearing(), null);

        final HearingUnscheduledListingNeeds listingNeeds = captureListingNeeds();
        assertThat(listingNeeds.getId(), is(HEARING_ID));
        assertThat(listingNeeds.getTypeOfList().getId(), is(RESULT_DEFINITION_NHCCS));
        assertThat(listingNeeds.getTypeOfList().getDescription(), is("Date and time to be fixed"));
    }

    @Test
    public void shouldListUnscheduledHearingWithProvidedTypeOfList() {
        final TypeOfList typeOfList = TypeOfList.typeOfList()
                .withId(randomUUID())
                .withDescription("Warned list")
                .build();

        hearingUnscheduledListingHelper.processUnscheduledHearings(event, hearing(), typeOfList);

        final HearingUnscheduledListingNeeds listingNeeds = captureListingNeeds();
        assertThat(listingNeeds.getId(), is(HEARING_ID));
        assertThat(listingNeeds.getTypeOfList(), is(typeOfList));
    }

    private HearingUnscheduledListingNeeds captureListingNeeds() {
        verify(listingService).listUnscheduledHearings(any(), listUnscheduledCourtHearingCaptor.capture());
        return listUnscheduledCourtHearingCaptor.getValue().getHearings().get(0);
    }

    private static Hearing hearing() {
        return Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType().withId(randomUUID()).withDescription("Sentence").build())
                .build();
    }
}
