package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.time.LocalTime.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.TIME_HMMA;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("squid:S2384")
public class PublishCourtListByCourtroom {

    private static final DateTimeFormatter TIME_FORMATTER = ofPattern(TIME_HMMA.getValue());
    private final String courtroomName;
    private final List<PublishCourtListByTime> hearingByTimes;

    private PublishCourtListByCourtroom(final String courtroomName, final List<PublishCourtListByTime> hearingByTimes) {
        this.courtroomName = courtroomName;
        this.hearingByTimes = hearingByTimes;
    }

    public String getCourtroomName() {
        return courtroomName;
    }

    public List<PublishCourtListByTime> getHearingByTimes() {
        return isNull(hearingByTimes) ? ImmutableList.of() : copyOf(hearingByTimes);
    }

    public PublishCourtListByCourtroom addHearingByTime(final PublishCourtListByTime hearingByTime) {
        this.hearingByTimes.add(hearingByTime);
        this.hearingByTimes.sort(new SortByHearingTimeComparator());
        return this;
    }

    public PublishCourtListByTime getHearingByTime(final String hearingTime) {
        return isNull(hearingByTimes) ? null : hearingByTimes.stream().filter(hearingByTime -> hearingByTime.getHearingTime().equals(hearingTime)).findFirst().orElse(null);
    }

    public static PublishCourtListHearingByCourtroomBuilder publishCourtListHearingByCourtroomBuilder() {
        return new PublishCourtListHearingByCourtroomBuilder();
    }

    public static final class PublishCourtListHearingByCourtroomBuilder {
        private String courtroomName;
        private List<PublishCourtListByTime> hearingByTimes;

        private PublishCourtListHearingByCourtroomBuilder() {
        }

        public PublishCourtListHearingByCourtroomBuilder withCourtroomName(final String courtroomName) {
            this.courtroomName = courtroomName;
            return this;
        }

        public PublishCourtListHearingByCourtroomBuilder addHearingByTime(final PublishCourtListByTime hearingByTime) {
            if (isNull(this.hearingByTimes)) {
                this.hearingByTimes = new ArrayList<>();
            }
            this.hearingByTimes.add(hearingByTime);
            this.hearingByTimes.sort(new SortByHearingTimeComparator());
            return this;
        }

        public PublishCourtListByCourtroom build() {
            return new PublishCourtListByCourtroom(courtroomName, hearingByTimes);
        }
    }

    private static class SortByHearingTimeComparator implements Comparator<PublishCourtListByTime> {

        @Override
        public int compare(final PublishCourtListByTime o1, final PublishCourtListByTime o2) {
            return parse(o1.getHearingTime(), TIME_FORMATTER).compareTo(parse(o2.getHearingTime(), TIME_FORMATTER));
        }
    }

}
