package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class CourtDocumentAggregateTest {

    private static final CourtDocument courtDocument = CourtDocument.courtDocument()
            .withCourtDocumentId(UUID.randomUUID())
            .withDocumentTypeDescription("documentTypeDescription")
            .withDocumentTypeId(UUID.randomUUID())
            .withIsRemoved(false)
            .withMimeType("pdf")
            .withName("name")
            .build();

    private CourtDocumentAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new CourtDocumentAggregate();
    }

    @Test
    public void shouldReturnCourtsDocumentCreated() {

        final List<Object> eventStream = aggregate.createCourtDocument(courtDocument).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentCreated.class)));
    }
    @Test
    public void shouldReturnCourtsDocumentAdded() {

        final List<Object> eventStream = aggregate.addCourtDocument(CourtDocument.courtDocument().build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentAdded.class)));
    }
}
