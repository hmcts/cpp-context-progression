package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingAggregateTest {

    @InjectMocks
    private HearingAggregate hearingAggregate;

    @Test
    public void shouldMarkHearingDuplicate() {
        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID());
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());

        final List<Object> eventStream = hearingAggregate.markAsDuplicate(hearingId, caseIds, defendantIds).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingMarkedAsDuplicate hearingMarkedAsDuplicate = (HearingMarkedAsDuplicate) eventStream.get(0);
        assertThat(hearingMarkedAsDuplicate.getHearingId(), is(hearingId));
        assertThat(hearingMarkedAsDuplicate.getCaseIds(), is(caseIds));
        assertThat(hearingMarkedAsDuplicate.getDefendantIds(), is(defendantIds));
    }

    @Test
    public void shouldNotRaiseNewEventIfAlreadyMarkedAsDuplicate() {
        setField(hearingAggregate, "duplicate", true);

        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID());
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());

        final List<Object> eventStream = hearingAggregate.markAsDuplicate(hearingId, caseIds, defendantIds).collect(toList());

        assertThat(eventStream.size(), is(0));
    }
}
