package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("squid:S2384")
public class PublishCourtListByDate {

    private final String hearingDate;
    private final List<PublishCourtListByCourtroom> hearingByCourtrooms;

    private PublishCourtListByDate(final String hearingDate, final List<PublishCourtListByCourtroom> hearingByCourtrooms) {
        this.hearingDate = hearingDate;
        this.hearingByCourtrooms = hearingByCourtrooms;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public List<PublishCourtListByCourtroom> getHearingByCourtrooms() {
        return isNull(hearingByCourtrooms) ? ImmutableList.of() : copyOf(hearingByCourtrooms);
    }

    public PublishCourtListByCourtroom getHearingByCourtroom(final String courtroomName) {
        return isNull(hearingByCourtrooms) ? null : hearingByCourtrooms.stream().filter(hearingByCourtroom -> hearingByCourtroom.getCourtroomName().equalsIgnoreCase(courtroomName)).findFirst().orElse(null);
    }

    public PublishCourtListByDate addHearingByCourtroom(final PublishCourtListByCourtroom hearingByCourtroom) {
        this.hearingByCourtrooms.add(hearingByCourtroom);
        this.hearingByCourtrooms.sort(new SortHearingByCourtroomComparator());
        return this;
    }

    public static PublishCourtListHearingByDateBuilder publishCourtListHearingByDateBuilder() {
        return new PublishCourtListHearingByDateBuilder();
    }

    public static final class PublishCourtListHearingByDateBuilder {
        private String hearingDate;
        private List<PublishCourtListByCourtroom> hearingByCourtrooms;

        private PublishCourtListHearingByDateBuilder() {
        }

        public PublishCourtListHearingByDateBuilder withHearingDate(final String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public PublishCourtListHearingByDateBuilder addHearingByCourtroom(final PublishCourtListByCourtroom hearingByCourtroom) {
            if (isNull(this.hearingByCourtrooms)) {
                this.hearingByCourtrooms = new ArrayList<>();
            }
            this.hearingByCourtrooms.add(hearingByCourtroom);
            this.hearingByCourtrooms.sort(new SortHearingByCourtroomComparator());
            return this;
        }

        public PublishCourtListByDate build() {
            return new PublishCourtListByDate(hearingDate, hearingByCourtrooms);
        }
    }

    private static class SortHearingByCourtroomComparator implements Comparator<PublishCourtListByCourtroom> {

        @Override
        public int compare(final PublishCourtListByCourtroom o1, final PublishCourtListByCourtroom o2) {
            if (nonNull(o1.getCourtroomName()) && nonNull(o2.getCourtroomName())) {
                return o1.getCourtroomName().compareToIgnoreCase(o2.getCourtroomName());
            }
            return 0;
        }
    }
}
