package uk.gov.moj.cpp.progression.domain.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.*;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationAggregateTest {

    private static final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().build();
    @InjectMocks
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
    public void shouldReturnBoxWorkApplicationReferred() {
        final List<Object> eventStream = aggregate.referBoxWorkApplication(HearingListingNeeds.hearingListingNeeds()
                .withCourtApplications(Arrays.asList(courtApplication()
                        .withId(UUID.randomUUID())
                        .build()))
                .build()).collect(toList());
        assertThat(eventStream.size(), is(2));
        Object objectEvent = eventStream.get(1);
        assertThat(objectEvent.getClass(), is(CoreMatchers.equalTo(BoxworkApplicationReferred.class)));
        objectEvent = eventStream.get(0);
        assertThat(objectEvent.getClass(), is(CoreMatchers.equalTo(CourtApplicationUpdated.class)));

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
        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication()
                .withId(randomUUID())
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtApplicationCreated.class)));
    }

    @Test
    public void shouldReturnAddCourtApplicationCase() {
        final List<Object> eventStream = aggregate.addApplicationToCase(courtApplication()
                .withId(randomUUID())
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

        eventStream = aggregate.updateCourtApplication(courtApplication()
                .withId(applicationId)
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(2));
        Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo(ListedCourtApplicationChanged.class)));
        event = eventStream.get(1);
        assertThat(event.getClass(), is(CoreMatchers.equalTo(CourtApplicationUpdated.class)));
    }

    @Test
    public void shouldReturnApplicationEjected() {
        final List<Object> eventStream = aggregate.ejectApplication(randomUUID(), "Legal").collect(toList());;
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(ApplicationEjected.class)));
    }

    @Test
    public void shouldNotReturnApplicationEjected() {
        Whitebox.setInternalState(this.aggregate, "applicationStatus", ApplicationStatus.EJECTED);
        final List<Object> eventStream = aggregate.ejectApplication(randomUUID(), "Legal").collect(toList());;
        assertThat(eventStream.size(), is(0));
    }
}
