package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.domain.event.MaterialStatusUpdateIgnored;

import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("squid:S1948")
public class MaterialAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialAggregate.class);

    private MaterialDetails details;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NowsMaterialRequestRecorded.class).apply(e ->
                        details = e.getContext()
                ),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> create(final MaterialDetails materialDetails) {
        LOGGER.debug("material context started ");
        return apply(Stream.of(NowsMaterialRequestRecorded
                .nowsMaterialRequestRecorded()
                .withContext(materialDetails).build()));
    }

    public Stream<Object> nowsMaterialStatusUpdated(final UUID materialId, final String status) {
        final Object event = details == null ? new MaterialStatusUpdateIgnored(materialId, status) : new NowsMaterialStatusUpdated(this.details, status);
        return Stream.of(event);
    }


}
