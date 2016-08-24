package uk.gov.moj.cpp.progression.domain.aggregate;

import org.junit.Before;

import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;

public class CaseProgressionAggregateBaseTest {

    protected CaseProgressionAggregate caseProgressionAggregate;

    @Before
    public void setUp() {
        caseProgressionAggregate = new CaseProgressionAggregate();
    }
}
