package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;

import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListHearing.SortDefendantComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("squid:S2384")
public class PublishCourtListByTime {

    private final String hearingTime;
    private final List<PublishCourtListHearing> hearings;

    private PublishCourtListByTime(final String hearingTime, final List<PublishCourtListHearing> hearings) {
        this.hearingTime = hearingTime;
        this.hearings = hearings;
    }

    public String getHearingTime() {
        return hearingTime;
    }

    public List<PublishCourtListHearing> getHearings() {
        return isNull(hearings) ? ImmutableList.of() : copyOf(hearings);
    }

    public PublishCourtListHearing getHearing(final String caseUrn, final String hearingType) {
        return isNull(hearings) ? null : hearings.stream().filter(hearing -> hearing.getCaseReference().equals(caseUrn) && hearing.getHearingType().equals(hearingType)).findFirst().orElse(null);
    }

    public PublishCourtListByTime addHearing(final PublishCourtListHearing hearing) {
        this.hearings.add(hearing);
        this.hearings.sort(new SortHearingComparator());
        return this;
    }

    public static PublishCourtListHearingByTimeBuilder publishCourtListHearingByTimeBuilder() {
        return new PublishCourtListHearingByTimeBuilder();
    }

    public static final class PublishCourtListHearingByTimeBuilder {
        private String hearingTime;
        private List<PublishCourtListHearing> hearings;

        private PublishCourtListHearingByTimeBuilder() {
        }

        public PublishCourtListHearingByTimeBuilder withHearingTime(final String hearingTime) {
            this.hearingTime = hearingTime;
            return this;
        }

        public PublishCourtListHearingByTimeBuilder addHearing(final PublishCourtListHearing hearing) {
            if (isNull(this.hearings)) {
                this.hearings = new ArrayList<>();
            }
            this.hearings.add(hearing);
            this.hearings.sort(new SortHearingComparator());
            return this;
        }

        public PublishCourtListByTime build() {
            return new PublishCourtListByTime(hearingTime, hearings);
        }
    }

    private static class SortHearingComparator implements Comparator<PublishCourtListHearing> {

        @Override
        public int compare(final PublishCourtListHearing o1, final PublishCourtListHearing o2) {
            if (!o1.getDefendants().isEmpty() && !o2.getDefendants().isEmpty()) {
                return new SortDefendantComparator().compare(o1.getDefendants().get(0), o2.getDefendants().get(0));
            }
            return 0;
        }
    }

}
