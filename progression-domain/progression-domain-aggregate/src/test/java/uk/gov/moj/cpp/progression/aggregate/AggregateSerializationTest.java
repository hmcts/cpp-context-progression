package uk.gov.moj.cpp.progression.aggregate;

import uk.gov.moj.cpp.platform.test.serializable.AggregateSerializableChecker;

import org.junit.jupiter.api.Test;

public class AggregateSerializationTest {

    private AggregateSerializableChecker aggregateSerializableChecker = new AggregateSerializableChecker();

    @Test
    public void shouldCheckAggregatesAreSerializable() {
        aggregateSerializableChecker.checkAggregatesIn("uk.gov.moj.cpp.progression.aggregate");
    }
}