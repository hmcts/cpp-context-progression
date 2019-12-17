package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.CourtsDocumentRemoved;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtDocumentAggregate implements Aggregate {

private static final long serialVersionUID = 101L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentAggregate.class);

    @Override
    public Object apply(final Object event) {
        return match(event).with(
            when(CourtsDocumentCreated.class).apply(e -> {
                        //do nothing
                    }
            ),
            when(CourtsDocumentAdded.class).apply(e -> {
                        //do nothing
                    }
            ), otherwiseDoNothing());
    }
    public Stream<Object> createCourtDocument(final CourtDocument courtDocument) {
        LOGGER.debug("court document is being created .");
        return apply(Stream.of(CourtsDocumentCreated.courtsDocumentCreated().withCourtDocument(courtDocument).build()));
    }

    public Stream<Object> addCourtDocument(final CourtDocument courtDocument) {
        LOGGER.debug("Court document being added");
        return apply(Stream.of(CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build()));
    }

    public Stream<Object> removeCourtDocument(final UUID courtDocumentId, final UUID materialId, final boolean isRemoved) {
        LOGGER.debug("Court document being removed");
        return apply(Stream.of(CourtsDocumentRemoved.courtsDocumentRemoved().withCourtDocumentId(courtDocumentId).withMaterialId(materialId).withIsRemoved(isRemoved).build()));
    }

}
