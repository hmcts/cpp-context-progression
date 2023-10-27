package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("squid:S2384")
public class PublishCourtListForWeekCommencingByCourtroom {

    private final String courtroomName;
    private final List<PublishCourtListHearing> hearings;

    private PublishCourtListForWeekCommencingByCourtroom(final String courtroomName, final List<PublishCourtListHearing> hearings) {
        this.courtroomName = courtroomName;
        this.hearings = hearings;
    }

    public String getCourtroomName() {
        return courtroomName;
    }

    public List<PublishCourtListHearing> getHearings() {
        return isNull(hearings) ? ImmutableList.of() : copyOf(hearings);
    }

    public PublishCourtListHearing getHearing(final String caseUrn, final String hearingType) {
        return isNull(hearings) ? null : hearings.stream().filter(hearing -> hearing.getCaseReference().equals(caseUrn) && hearing.getHearingType().equals(hearingType)).findFirst().orElse(null);
    }

    public PublishCourtListForWeekCommencingByCourtroom addHearing(final PublishCourtListHearing hearing) {
        this.hearings.add(hearing);
        this.hearings.sort(new SortHearingComparator());
        return this;
    }

    public static PublishCourtListForWeekCommencingByCourtroomBuilder publishCourtListForWeekCommencingByCourtroomBuilder() {
        return new PublishCourtListForWeekCommencingByCourtroomBuilder();
    }

    public static final class PublishCourtListForWeekCommencingByCourtroomBuilder {
        private String courtroomName;
        private List<PublishCourtListHearing> hearings;

        private PublishCourtListForWeekCommencingByCourtroomBuilder() {
        }

        public PublishCourtListForWeekCommencingByCourtroomBuilder withCourtroomName(final String courtroomName) {
            this.courtroomName = courtroomName;
            return this;
        }

        public PublishCourtListForWeekCommencingByCourtroomBuilder addHearing(final PublishCourtListHearing hearing) {
            if (isNull(this.hearings)) {
                this.hearings = new ArrayList<>();
            }
            this.hearings.add(hearing);
            this.hearings.sort(new SortHearingComparator());
            return this;
        }

        public PublishCourtListForWeekCommencingByCourtroom build() {
            return new PublishCourtListForWeekCommencingByCourtroom(courtroomName, hearings);
        }
    }

    private static class SortHearingComparator implements Comparator<PublishCourtListHearing> {

        @Override
        public int compare(final PublishCourtListHearing o1, final PublishCourtListHearing o2) {
            if (!o1.getDefendants().isEmpty() && !o2.getDefendants().isEmpty()) {
                return new PublishCourtListHearing.SortDefendantComparator().compare(o1.getDefendants().get(0), o2.getDefendants().get(0));
            }
            return 0;
        }
    }

}
