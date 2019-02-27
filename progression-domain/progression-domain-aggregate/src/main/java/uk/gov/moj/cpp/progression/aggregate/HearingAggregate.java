package uk.gov.moj.cpp.progression.aggregate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.SharedHearing;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.hearing.courts.HearingResulted;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

@SuppressWarnings({"squid:S1948", "squid:S1172"})
public class HearingAggregate implements Aggregate {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAggregate.class);
    private static final long serialVersionUID = 7006066325447433090L;
    private final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ProsecutionCaseDefendantListingStatusChanged.class).apply(e -> {
                            //do nothing
                        }
                ),
                when(HearingDefendantRequestCreated.class).apply(e -> {
                    if (!e.getDefendantRequests().isEmpty()) {
                        listDefendantRequests.addAll(e.getDefendantRequests());
                    }
                })
                , otherwiseDoNothing());
    }

    public Stream<Object> createSummonsData(final CourtCentre courtCentre, final ZonedDateTime hearingDateTime, final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds) {
        if (!listDefendantRequests.isEmpty()) {
            return apply(Stream.of(SummonsDataPrepared.summonsDataPrepared()
                    .withSummonsData(
                            SummonsData.summonsData()
                                    .withHearingDateTime(hearingDateTime)
                                    .withCourtCentre(courtCentre)
                                    .withConfirmedProsecutionCaseIds(confirmedProsecutionCaseIds)
                                    .withListDefendantRequests(listDefendantRequests)
                                    .build()
                    )
                    .build()));
        }
        return null;
    }

    public Stream<Object> updateDefedantListingStatus(final Hearing hearing, final HearingListingStatus hearingListingStatus) {
        LOGGER.debug("Defedent listing status updated .");

        return apply(Stream.of(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearing(hearing)
                .withHearingListingStatus(hearingListingStatus)
                .build()));
    }

    public Stream<Object> updateDefedantHearingResult(final UUID hearingId, final List<SharedResultLine> sharedResultLines) {
        LOGGER.debug("Defedent hearing reulst updated .");

        return apply(Stream.of(ProsecutionCaseDefendantHearingResultUpdated.prosecutionCaseDefendantHearingResultUpdated()
                .withHearingId(hearingId)
                .withSharedResultLines(sharedResultLines)
                .build()));
    }

    public Stream<Object> createHearingDefendantRequest(final List<ListDefendantRequest> listDefendantRequests) {
        LOGGER.debug("List Defendant Request is being created.");
        return apply(Stream.of(HearingDefendantRequestCreated.hearingDefendantRequestCreated().withDefendantRequests(listDefendantRequests).build()));
    }

    public Stream<Object> saveHearingResult(SharedHearing hearing) {
        LOGGER.debug("Hearing Resulted.");
        return apply(Stream.of(HearingResulted.hearingResulted().withHearing(hearing).build()));
    }
}
