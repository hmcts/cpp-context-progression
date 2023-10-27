package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("squid:S2384")
public class PublishCourtListByWeekCommencingDate {

    private final String weekCommencingStartDate;
    private final String weekCommencingEndDate;
    private final List<PublishCourtListForWeekCommencingByCourtroom> hearingByCourtrooms;

    private PublishCourtListByWeekCommencingDate(final String weekCommencingStartDate, final String weekCommencingEndDate, final List<PublishCourtListForWeekCommencingByCourtroom> hearingByCourtrooms) {
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.hearingByCourtrooms = hearingByCourtrooms;
    }

    public String getWeekCommencingStartDate() {
        return weekCommencingStartDate;
    }

    public String getWeekCommencingEndDate() {
        return weekCommencingEndDate;
    }

    public List<PublishCourtListForWeekCommencingByCourtroom> getHearingByCourtrooms() {
        return isNull(hearingByCourtrooms) ? ImmutableList.of() : copyOf(hearingByCourtrooms);
    }

    public PublishCourtListForWeekCommencingByCourtroom getHearingByCourtroom(final String courtroomName) {
        return isNull(hearingByCourtrooms) ? null : hearingByCourtrooms.stream().filter(hearingByCourtroom -> hearingByCourtroom.getCourtroomName().equalsIgnoreCase(courtroomName)).findFirst().orElse(null);
    }

    public PublishCourtListByWeekCommencingDate addHearingByCourtroom(final PublishCourtListForWeekCommencingByCourtroom hearingByCourtroom) {
        this.hearingByCourtrooms.add(hearingByCourtroom);
        this.hearingByCourtrooms.sort(new SortHearingByCourtroomComparator());
        return this;
    }

    public static PublishCourtListHearingByWeekCommencingDateBuilder publishCourtListHearingByWeekCommencingDateBuilder() {
        return new PublishCourtListHearingByWeekCommencingDateBuilder();
    }

    public static final class PublishCourtListHearingByWeekCommencingDateBuilder {
        private String weekCommencingStartDate;
        private String weekCommencingEndDate;
        private List<PublishCourtListForWeekCommencingByCourtroom> hearingByCourtrooms;

        private PublishCourtListHearingByWeekCommencingDateBuilder() {
        }

        public PublishCourtListHearingByWeekCommencingDateBuilder withWeekCommencingStartDate(final String weekCommencingStartDate) {
            this.weekCommencingStartDate = weekCommencingStartDate;
            return this;
        }

        public PublishCourtListHearingByWeekCommencingDateBuilder withWeekCommencingEndDate(final String weekCommencingEndDate) {
            this.weekCommencingEndDate = weekCommencingEndDate;
            return this;
        }

        public PublishCourtListHearingByWeekCommencingDateBuilder addHearingByCourtroom(final PublishCourtListForWeekCommencingByCourtroom hearingByCourtroom) {
            if (isNull(this.hearingByCourtrooms)) {
                this.hearingByCourtrooms = new ArrayList<>();
            }
            this.hearingByCourtrooms.add(hearingByCourtroom);
            this.hearingByCourtrooms.sort(new SortHearingByCourtroomComparator());
            return this;
        }

        public PublishCourtListByWeekCommencingDate build() {
            return new PublishCourtListByWeekCommencingDate(weekCommencingStartDate, weekCommencingEndDate, hearingByCourtrooms);
        }
    }

    private static class SortHearingByCourtroomComparator implements Comparator<PublishCourtListForWeekCommencingByCourtroom> {

        @Override
        public int compare(final PublishCourtListForWeekCommencingByCourtroom o1, final PublishCourtListForWeekCommencingByCourtroom o2) {
            if (nonNull(o1.getCourtroomName()) && nonNull(o2.getCourtroomName())) {
                return o1.getCourtroomName().compareToIgnoreCase(o2.getCourtroomName());
            }
            return 0;
        }
    }
}
