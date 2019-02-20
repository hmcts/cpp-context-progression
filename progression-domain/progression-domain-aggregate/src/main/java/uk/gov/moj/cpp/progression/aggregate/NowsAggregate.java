package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.events.NowsRequested;

import java.util.stream.Stream;

public class NowsAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(NowsAggregate.class);

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                //no state !
                otherwiseDoNothing()
        );
    }

    public Stream<Object> create(final CreateNowsRequest createNowsRequest) {
        LOGGER.debug("Nows is to be created ");
        return apply(Stream.of(NowsRequested.nowsRequested().withCreateNowsRequest(createNowsRequest).build()));
    }


}