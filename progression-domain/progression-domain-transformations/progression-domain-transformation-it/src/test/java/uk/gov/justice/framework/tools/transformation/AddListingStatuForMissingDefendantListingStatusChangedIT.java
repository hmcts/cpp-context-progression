package uk.gov.justice.framework.tools.transformation;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.framework.tools.transformation.FileUtil.getPayload;
import static uk.gov.justice.framework.tools.transformation.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.justice.framework.tools.transformation.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.PublishedEvent;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStream;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AddListingStatuForMissingDefendantListingStatusChangedIT {

    private static final long STREAM_COUNT_REPORTING_INTERVAL = 10L;
    private static final String MEMORY_OPTIONS_PARAMETER = "2048M";
    private static final Boolean ENABLE_REMOTE_DEBUGGING_FOR_WILDFLY = true;
    private static final int WILDFLY_TIMEOUT_IN_SECONDS = 60;

    private SwarmStarterUtil swarmStarterUtil;
    private DatabaseUtils databaseUtils;
    private TestPublishedEventJdbcRepository testPublishedEventJdbcRepository;
    private TestHelper testHelper;
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    public void setUp() throws Exception {
        testHelper = new TestHelper();
        stringToJsonObjectConverter = new StringToJsonObjectConverter();
        swarmStarterUtil = new SwarmStarterUtil();
        databaseUtils = new DatabaseUtils();
        databaseUtils.dropAndUpdateLiquibase();
        testPublishedEventJdbcRepository = new TestPublishedEventJdbcRepository(databaseUtils.getDataSource());
    }

    @Test
    public void shouldTransformEventInEventStoreForListingChanged() throws Exception {
        cleanEventStoreTables();
        cleanViewStoreTables();
        final ZonedDateTime createdAt = new UtcClock().now().minusMonths(1);
        final String transformedEventName = "progression.event.prosecutionCase-defendant-listing-status-changed";

        final UUID streamId1 = fromString("81dff79a-da70-4919-8eb1-aeeb1306f7df");
        final UUID streamId2 = fromString("496dabe7-b8ed-4c97-a2be-ff94efb30d27");
        final UUID streamIdNotToBeTransformed = fromString("761cfc5c-0084-41c8-8bd3-71c3efe82115");
        final UUID streamIdForAnotherEvent = UUID.randomUUID();
        final String event1Payload = getPayload("transformation/progression.event.prosecutionCase-defendant-listing-status-changed-event1.json");
        final String event2Payload = getPayload("transformation/progression.event.prosecutionCase-defendant-listing-status-changed-event2.json");
        final String event3Payload = getPayload("transformation/progression.event.prosecutionCase-defendant-listing-status-changed-event3.json");

        databaseUtils.insertEventLogData(transformedEventName, streamId1, 1L, createdAt, of(1L), true, event1Payload);
        databaseUtils.insertEventLogData(transformedEventName, streamId2, 1L, createdAt, of(1L), true, event2Payload);
        databaseUtils.insertEventLogData(transformedEventName, streamIdNotToBeTransformed, 1L, createdAt, of(1L), true, event3Payload);
        databaseUtils.insertEventLogData("sample.v2.events.name", streamIdForAnotherEvent, 1L, createdAt, of(1L), true, "");

        final String transformationJar = "progression-domain*.jar";
        swarmStarterUtil.runCommandW(ENABLE_REMOTE_DEBUGGING_FOR_WILDFLY, WILDFLY_TIMEOUT_IN_SECONDS, STREAM_COUNT_REPORTING_INTERVAL, MEMORY_OPTIONS_PARAMETER, transformationJar);

        assertThat(10, is(10));

        assertThat(testHelper.eventStoreTransformedEventPresentAndSequenceCorrect(transformedEventName, streamId1, 1l), is(true));
        assertThat(testHelper.eventStoreTransformedEventPresentAndSequenceCorrect(transformedEventName, streamId2, 1l), is(true));

        assertThat(testHelper.originalEventStreamIsActive(streamId1), is(true));
        assertThat(testHelper.originalEventStreamIsActive(streamId2), is(true));
        assertThat(testHelper.originalEventStreamIsActive(streamIdNotToBeTransformed), is(true));
        assertThat(testHelper.originalEventStreamIsActive(streamIdForAnotherEvent), is(true));

        assertThat(testHelper.clonedStreamAvailableAndActive(), is(false));
        assertThat(testHelper.totalEventCount(), is(8L));

        final long publishedEventsCount1 = testPublishedEventJdbcRepository.publishedEventsCount(streamId1);
        assertThat(publishedEventsCount1, is(1L));

        final long publishedEventsCount2 = testPublishedEventJdbcRepository.publishedEventsCount(streamId2);
        assertThat(publishedEventsCount2, is(1L));

        final long publishedEventsCount3 = testPublishedEventJdbcRepository.publishedEventsCount(streamIdNotToBeTransformed);
        assertThat(publishedEventsCount3, is(1L));

        final long publishedEventsCount4 = testPublishedEventJdbcRepository.publishedEventsCount(streamIdForAnotherEvent);
        assertThat(publishedEventsCount4, is(1L));

        final List<PublishedEvent> publishedEvents = testPublishedEventJdbcRepository.findAllOrderByPositionAsc();
        assertThat(publishedEvents.size(), is(4));

        final PublishedEvent publishedEvent_1 = publishedEvents.get(0);
        assertThat(publishedEvent_1.getPositionInStream(), is(1L));
        assertThat(publishedEvent_1.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_1.getName(), is(transformedEventName));
        assertThat(publishedEvent_1.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_1.getEventNumber(), is(of(1L)));
        assertThat(publishedEvent_1.getPreviousEventNumber(), is(0L));
        assertThat(publishedEvent_1.getPreviousEventNumber(), is(0L));

        final PublishedEvent publishedEvent_2 = publishedEvents.get(1);
        assertThat(publishedEvent_2.getPositionInStream(), is(1L));
        assertThat(publishedEvent_2.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_2.getName(), is("sample.v2.events.name"));
        assertThat(publishedEvent_2.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_2.getEventNumber(), is(of(2L)));
        assertThat(publishedEvent_2.getPreviousEventNumber(), is(1L));

        final PublishedEvent publishedEvent_3 = publishedEvents.get(2);
        assertThat(publishedEvent_3.getPositionInStream(), is(1L));
        assertThat(publishedEvent_3.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_3.getName(), is(transformedEventName));
        assertThat(publishedEvent_3.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_3.getEventNumber(), is(of(5L)));
        assertThat(publishedEvent_3.getPreviousEventNumber(), is(2L));

        final PublishedEvent publishedEvent_4 = publishedEvents.get(3);
        assertThat(publishedEvent_4.getPositionInStream(), is(1L));
        assertThat(publishedEvent_4.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_4.getName(), is(transformedEventName));
        assertThat(publishedEvent_4.getCreatedAt(), is(createdAt));
        assertThat(publishedEvent_4.getEventNumber(), is(of(6L)));
        assertThat(publishedEvent_4.getPreviousEventNumber(), is(5L));

        final long clonedEventsCount = testHelper.totalEventCount("system.events.cloned");
        assertThat(clonedEventsCount, is(2L));

        final List<Event> clonedEvents = testHelper.retrieveEventsFor("system.events.cloned");
        final String transformedEventString1 = clonedEvents.get(0).getPayload();
        final JsonObject cloned1 = stringToJsonObjectConverter.convert(transformedEventString1);
        assertThat(cloned1.getString("originatingStream"), is(streamId1.toString()));

        final String transformedEventString2 = clonedEvents.get(1).getPayload();
        final JsonObject cloned2 = stringToJsonObjectConverter.convert(transformedEventString2);
        assertThat(cloned2.getString("originatingStream"), is(streamId2.toString()));

        assertThat(testPublishedEventJdbcRepository.prePublishQueueCount(), is(0L));
    }
}
