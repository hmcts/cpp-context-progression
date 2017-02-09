package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.test.utils.DefendantBuilder;

@RunWith(MockitoJUnitRunner.class)
public class CaseProgressionAggregateTest {

    @InjectMocks
    private CaseProgressionAggregate caseProgressionAggregate;

    @Test
    public void shouldReturnEmptyStringForNonExistingDefendant() {
        // given
        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendant();

        // when
        final Stream<Object> objectStream = caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertThat(objectStream.count(), is(0L));
    }

    @Test
    public void shouldAddAdditionalInformationForDefendant() {
        // given
        final UUID defendantId = randomUUID();
        // and
        createDefendant(defendantId);
        // and
        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendantWith(defendantId);

        // when
        final Stream<Object> objectStream = caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertAdditionalInformationEvent(defendantId, objectStream);
    }

    @Test
    public void shouldNotAddAdditionalInformationForDefendant() {
        // given
        final UUID defendantId = randomUUID();
        // and
        createDefendant(defendantId);
        // and
        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendantWithoutAdditionalInfo(defendantId);
        // when
        final Stream<Object> objectStream = caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertAdditionalInformationEvent(defendantId, objectStream);
    }

    @Test
    public void shouldHandleNoMoreInformationRequired() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);
        
        final NoMoreInformationRequiredEvent noMoreInformationRequiredEvent =
                        new NoMoreInformationRequiredEvent(caseId, defendantId, caseProgressionId);
        
        Object response = caseProgressionAggregate.apply(noMoreInformationRequiredEvent);
        
        assertNoMoreInformationRequiredEvent(defendantId, response);
    }
    
    @Test
    public void shouldHandleNoMoreInformationForDefendant() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);
        final Stream<Object> response = caseProgressionAggregate.noMoreInformationForDefendant(defendantId,caseId, caseProgressionId);
        final Object[] events = response.toArray();
        assertNoMoreInformationRequiredEvent(defendantId, events[events.length-1]);
    }
    
    @Test
    public void shouldHandleNoMoreInformationForDefendantWrongId() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);
        final UUID wrongDefendantId = randomUUID();
        final Stream<Object> response = caseProgressionAggregate.noMoreInformationForDefendant(wrongDefendantId,caseId, caseProgressionId);
        
        assertThat(response.count(), is(0L));
    }
    
    
    private void assertNoMoreInformationRequiredEvent(UUID defendantId, Object response) {
        final NoMoreInformationRequiredEvent o =   (NoMoreInformationRequiredEvent) response;
        assertThat(o.getDefendantId(), is(defendantId));
    }
    
    private void assertAdditionalInformationEvent(UUID defendantId, Stream<Object> objectStream) {
        final DefendantAdditionalInformationAdded o =
                        (DefendantAdditionalInformationAdded) objectStream
                                        .filter(obj -> obj instanceof DefendantAdditionalInformationAdded)
                                        .findFirst().get();
        assertThat(o.getDefendantId(), is(defendantId));
    }

    
    private void createDefendant(UUID defendantId) {
        // and
        UUID caseProgressionId = randomUUID();
        // and
        UUID caseId = randomUUID();
        // and
        String courtCentreId = RandomStringUtils.random(3);
        // and
        Defendant defendant = new Defendant(defendantId);
        // and
        CaseAddedToCrownCourt caseAddedToCrownCourt = new CaseAddedToCrownCourt(caseProgressionId,
                        caseId, courtCentreId, Lists.newArrayList(defendant));
        // and
        caseProgressionAggregate.apply(caseAddedToCrownCourt);
    }
}
