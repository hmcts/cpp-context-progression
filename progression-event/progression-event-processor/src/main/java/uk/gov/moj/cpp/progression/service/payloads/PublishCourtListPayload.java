package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.common.converter.LocalDates.from;

import uk.gov.justice.listing.courts.PublishCourtListType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings({"squid:S00107","squid:S2384"})
public class PublishCourtListPayload {

    private final PublishCourtListType publishCourtListType;
    private final String issueDate;
    private final String listDate;
    private final String ljaCode;
    private final String ljaName;
    private final String ljaNameWelsh;
    private final String courtName;
    private final String courtNameWelsh;
    private final PublishCourtListCourtAddress courtAddress;
    private final boolean isWeekCommencing;
    private final String weekCommencingStartDate;
    private final String weekCommencingEndDate;
    private final PublishCourtListAddressee addressee;
    private final List<PublishCourtListByDate> hearingByDates;
    private final List<PublishCourtListByDate> hearingByDatesNoCourtroom;
    private final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDate;
    private final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDateNoCourtroom;


    private PublishCourtListPayload(final PublishCourtListType publishCourtListType, final String issueDate, final String listDate, final String ljaCode, final String ljaName,
                                    final String ljaNameWelsh, final String courtName, final String courtNameWelsh, final PublishCourtListCourtAddress courtAddress,
                                    final boolean isWeekCommencing, final String weekCommencingStartDate, final String weekCommencingEndDate, final PublishCourtListAddressee addressee,
                                    final List<PublishCourtListByDate> hearingByDates, final List<PublishCourtListByDate> hearingByDatesNoCourtroom,
                                    final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDate,
                                    final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDateNoCourtroom) {
        this.publishCourtListType = publishCourtListType;
        this.issueDate = issueDate;
        this.listDate = listDate;
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.ljaNameWelsh = ljaNameWelsh;
        this.courtName = courtName;
        this.courtNameWelsh = courtNameWelsh;
        this.courtAddress = courtAddress;
        this.isWeekCommencing = isWeekCommencing;
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.addressee = addressee;
        this.hearingByDates = hearingByDates;
        this.hearingByDatesNoCourtroom = hearingByDatesNoCourtroom;
        this.hearingByWeekCommencingDate = hearingByWeekCommencingDate;
        this.hearingByWeekCommencingDateNoCourtroom = hearingByWeekCommencingDateNoCourtroom;
    }

    public PublishCourtListType getPublishCourtListType() {
        return publishCourtListType;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public String getListDate() {
        return listDate;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getLjaNameWelsh() {
        return ljaNameWelsh;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getCourtNameWelsh() {
        return courtNameWelsh;
    }

    public PublishCourtListCourtAddress getCourtAddress() {
        return courtAddress;
    }

    public boolean isWeekCommencing() {
        return isWeekCommencing;
    }

    public String getWeekCommencingStartDate() {
        return weekCommencingStartDate;
    }

    public String getWeekCommencingEndDate() {
        return weekCommencingEndDate;
    }

    public PublishCourtListAddressee getAddressee() {
        return addressee;
    }

    public List<PublishCourtListByDate> getHearingByDates() {
        return isNull(hearingByDates) ? ImmutableList.of() : copyOf(hearingByDates);
    }

    public List<PublishCourtListByDate> getHearingByDatesNoCourtroom() {
        return isNull(hearingByDatesNoCourtroom) ? ImmutableList.of() : copyOf(hearingByDatesNoCourtroom);
    }

    public PublishCourtListByWeekCommencingDate getHearingByWeekCommencingDate() {
        return hearingByWeekCommencingDate;
    }

    public PublishCourtListByWeekCommencingDate getHearingByWeekCommencingDateNoCourtroom() {
        return hearingByWeekCommencingDateNoCourtroom;
    }

    public static PublishCourtListPayloadBuilder publishCourtListPayloadBuilder() {
        return new PublishCourtListPayloadBuilder();
    }

    public static final class PublishCourtListPayloadBuilder {
        private PublishCourtListType publishCourtListType;
        private String issueDate;
        private String listDate;
        private String ljaCode;
        private String ljaName;
        private String ljaNameWelsh;
        private String courtName;
        private String courtNameWelsh;
        private PublishCourtListCourtAddress courtAddress;
        private boolean isWeekCommencing;
        private String weekCommencingStartDate;
        private String weekCommencingEndDate;
        private PublishCourtListAddressee addressee;
        private List<PublishCourtListByDate> hearingByDates;
        private List<PublishCourtListByDate> hearingByDatesNoCourtroom;
        private PublishCourtListByWeekCommencingDate hearingByWeekCommencingDate;
        private PublishCourtListByWeekCommencingDate hearingByWeekCommencingDateNoCourtroom;

        private PublishCourtListPayloadBuilder() {
        }

        public PublishCourtListPayloadBuilder withPublishCourtListType(final PublishCourtListType publishCourtListType) {
            this.publishCourtListType = publishCourtListType;
            return this;
        }

        public PublishCourtListPayloadBuilder withIssueDate(final String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public PublishCourtListPayloadBuilder withListDate(final String listDate) {
            this.listDate = listDate;
            return this;
        }

        public PublishCourtListPayloadBuilder withLjaCode(final String ljaCode) {
            this.ljaCode = ljaCode;
            return this;
        }

        public PublishCourtListPayloadBuilder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public PublishCourtListPayloadBuilder withLjaNameWelsh(final String ljaNameWelsh) {
            this.ljaNameWelsh = ljaNameWelsh;
            return this;
        }

        public PublishCourtListPayloadBuilder withCourtName(final String courtName) {
            this.courtName = courtName;
            return this;
        }

        public PublishCourtListPayloadBuilder withCourtNameWelsh(final String courtNameWelsh) {
            this.courtNameWelsh = courtNameWelsh;
            return this;
        }

        public PublishCourtListPayloadBuilder withCourtAddress(final PublishCourtListCourtAddress courtAddress) {
            this.courtAddress = courtAddress;
            return this;
        }

        public PublishCourtListPayloadBuilder withIsWeekCommencing(final boolean isWeekCommencing) {
            this.isWeekCommencing = isWeekCommencing;
            return this;
        }

        public PublishCourtListPayloadBuilder withWeekCommencingStartDate(final String weekCommencingStartDate) {
            this.weekCommencingStartDate = weekCommencingStartDate;
            return this;
        }

        public PublishCourtListPayloadBuilder withWeekCommencingEndDate(final String weekCommencingEndDate) {
            this.weekCommencingEndDate = weekCommencingEndDate;
            return this;
        }

        public PublishCourtListPayloadBuilder withAddressee(final PublishCourtListAddressee addressee) {
            this.addressee = addressee;
            return this;
        }

        public PublishCourtListPayloadBuilder addHearingByDate(final PublishCourtListByDate hearingByDate) {
            if (isNull(this.hearingByDates)) {
                this.hearingByDates = new ArrayList<>();
            }
            this.hearingByDates.add(hearingByDate);
            this.hearingByDates.sort(new SortByHearingDateComparator());
            return this;
        }

        public List<PublishCourtListByDate> getHearingByDates() {
            return isNull(hearingByDates) ? null : copyOf(hearingByDates);
        }

        public PublishCourtListByDate getHearingByDate(final String hearingDate) {
            return isNull(hearingByDates) ? null : hearingByDates.stream().filter(hearingByDate -> hearingByDate.getHearingDate().equals(hearingDate)).findFirst().orElse(null);
        }

        public PublishCourtListPayloadBuilder addHearingByDateNoCourtroom(final PublishCourtListByDate hearingByDateNoCourtroom) {
            if (isNull(this.hearingByDatesNoCourtroom)) {
                this.hearingByDatesNoCourtroom = new ArrayList<>();
            }
            this.hearingByDatesNoCourtroom.add(hearingByDateNoCourtroom);
            this.hearingByDatesNoCourtroom.sort(new SortByHearingDateComparator());
            return this;
        }

        public List<PublishCourtListByDate> getHearingByDatesNoCourtroom() {
            return isNull(hearingByDatesNoCourtroom) ? null : copyOf(hearingByDatesNoCourtroom);
        }

        public PublishCourtListByDate getHearingByDateNoCourtroom(final String hearingDate) {
            return isNull(hearingByDatesNoCourtroom) ? null : hearingByDatesNoCourtroom.stream().filter(hearingByDate -> hearingByDate.getHearingDate().equals(hearingDate)).findFirst().orElse(null);
        }

        public PublishCourtListPayloadBuilder withHearingByWeekCommencingDate(final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDate) {
            this.hearingByWeekCommencingDate = hearingByWeekCommencingDate;
            return this;
        }

        public PublishCourtListPayloadBuilder withHearingByWeekCommencingDateNoCourtroom(final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDateNoCourtroom) {
            this.hearingByWeekCommencingDateNoCourtroom = hearingByWeekCommencingDateNoCourtroom;
            return this;
        }

        public PublishCourtListByWeekCommencingDate getHearingByWeekCommencingDate() {
            return hearingByWeekCommencingDate;
        }

        public PublishCourtListByWeekCommencingDate getHearingByWeekCommencingDateNoCourtroom() {
            return hearingByWeekCommencingDateNoCourtroom;
        }

        public PublishCourtListPayload build() {
            return new PublishCourtListPayload(publishCourtListType, issueDate, listDate, ljaCode, ljaName, ljaNameWelsh, courtName, courtNameWelsh, courtAddress, isWeekCommencing,
                    weekCommencingStartDate, weekCommencingEndDate, addressee, hearingByDates, hearingByDatesNoCourtroom, hearingByWeekCommencingDate, hearingByWeekCommencingDateNoCourtroom);
        }
    }

    private static class SortByHearingDateComparator implements Comparator<PublishCourtListByDate> {

        @Override
        public int compare(final PublishCourtListByDate o1, final PublishCourtListByDate o2) {
            return from(o1.getHearingDate()).compareTo(from(o2.getHearingDate()));
        }
    }
}
