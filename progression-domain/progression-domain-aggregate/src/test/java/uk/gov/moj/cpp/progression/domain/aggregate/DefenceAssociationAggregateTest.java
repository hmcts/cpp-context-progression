package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociated;

import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class DefenceAssociationAggregateTest {

    private DefenceAssociationAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new DefenceAssociationAggregate();
    }

    @Test
    public void shouldReturnCasesReferredToCourt() {
        final List<Object> eventStream = aggregate.associateOrganization(UUID.randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DefenceOrganisationAssociated.class)));
    }

}
