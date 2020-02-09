package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.RepresentationType;

import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class DefenceAssociationAggregateTest {

    private static final String ORGANISATION_NAME = "CompanyZ";
    private DefenceAssociationAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new DefenceAssociationAggregate();
    }

    @Test
    public void shouldReturnDefenceOrganisationAssociated() {
        final List<Object> eventStream = aggregate.associateOrganisation(randomUUID(),
                randomUUID(),
                ORGANISATION_NAME,
                RepresentationType.REPRESENTATION_ORDER.toString(),
                "1234567890",
                randomUUID().toString()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DefenceOrganisationAssociated.class)));
    }

    @Test
    public void shouldReturnEmptyStreamWhenOrgAlreadyAssociated() {
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        aggregate.associateOrganisation(defendantId,
                organisationId,
                ORGANISATION_NAME,
                RepresentationType.REPRESENTATION_ORDER.toString(),
                "1234567890",
                randomUUID().toString());
        final List<Object> eventStream = aggregate.associateOrganisation(defendantId,
                organisationId,
                ORGANISATION_NAME,
                RepresentationType.REPRESENTATION_ORDER.toString(),
                "1234567890",
                randomUUID().toString()).collect(toList());

        assertThat(eventStream.size(), is(0));

    }

    @Test
    public void shouldReturnDefenceOrganisationDisassociated() {
        final List<Object> eventStream = aggregate.disassociateOrganisation(randomUUID(),
                randomUUID(), randomUUID(), randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DefenceOrganisationDisassociated.class)));
    }

}
