package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import java.util.List;

public class HearingAggregateTest {

    @Test
    public void shouldReturnHearingResultedWhenHearingIsResultedAlready() {
        final HearingAggregate aggregate = new HearingAggregate();
        aggregate.apply(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .build());
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .build();
        final List<Object> eventStream = aggregate.updateDefendantListingStatus(hearing,
                HearingListingStatus.HEARING_INITIALISED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ProsecutionCaseDefendantListingStatusChanged.class)));
        final ProsecutionCaseDefendantListingStatusChanged event = (ProsecutionCaseDefendantListingStatusChanged) object;
        assertThat(hearing.getId(), is(equalTo(event.getHearing().getId())));
        assertThat(HearingListingStatus.HEARING_RESULTED, is(equalTo(event.getHearingListingStatus())));
    }

    @Test
    public void shouldReturnSameListingStatusWhenHearingIsNotResultedAlready() {
        final HearingAggregate aggregate = new HearingAggregate();
        aggregate.apply(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .build());
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .build();
        final List<Object> eventStream = aggregate.updateDefendantListingStatus(hearing,
                HearingListingStatus.HEARING_INITIALISED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ProsecutionCaseDefendantListingStatusChanged.class)));
        final ProsecutionCaseDefendantListingStatusChanged event = (ProsecutionCaseDefendantListingStatusChanged) object;
        assertThat(hearing.getId(), is(equalTo(event.getHearing().getId())));
        assertThat(HearingListingStatus.HEARING_INITIALISED, is(equalTo(event.getHearingListingStatus())));
    }

}
