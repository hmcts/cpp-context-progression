package uk.gov.moj.cpp.progression.domain.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.core.courts.*;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationAggregateTest {

    private static final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().build();
    private ApplicationAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new ApplicationAggregate();
    }

    @Test
    public void shouldReturnCasesReferredToCourt() {
        final List<Object> eventStream = aggregate.referApplicationToCourt(hearingListingNeeds).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(ApplicationReferredToCourt.class)));
    }
    
    @Test
    public void shouldReturnApplicationStatusChanged() {
        final List<Object> eventStream = aggregate.updateApplicationStatus(UUID.randomUUID(), ApplicationStatus.LISTED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtApplicationStatusChanged.class)));
    }

    @Test
    public void shouldReturnCourtApplicationCreated() {
        final List<Object> eventStream = aggregate.createCourtApplication(CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtApplicationCreated.class)));
    }

    @Test
    public void shouldReturnAddCourtApplicationCase() {
        final List<Object> eventStream = aggregate.addApplicationToCase(CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtApplicationAddedToCase.class)));
    }

    @Test
    public void shouldReturnHearingApplicationLinkCreated() {
        final List<Object> eventStream = aggregate.createHearingApplicationLink
                (Hearing.hearing().build(), UUID.randomUUID(), HearingListingStatus.HEARING_INITIALISED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(HearingApplicationLinkCreated.class)));
    }
    
    @Test
    public void shouldReturnApplicationUpdatedAndListedApplicationChanged() {
        UUID applicationId  = UUID.randomUUID();
        List<Object> eventStream = aggregate.updateApplicationStatus(applicationId, ApplicationStatus.LISTED).collect(toList());
        
        eventStream = aggregate.updateCourtApplication(CourtApplication.courtApplication()
                .withId(applicationId)
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(2));
        Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo(ListedCourtApplicationChanged.class)));
        event = eventStream.get(1);
        assertThat(event.getClass(), is(CoreMatchers.equalTo(CourtApplicationUpdated.class)));
    }
}
