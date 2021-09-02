package uk.gov.moj.cpp.progression.service.payloads;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StatDacAppointmentLetterPayload {

    private final List<String> caseApplicationReferences;

    private final OrderingCourt orderingCourt;

    private final LocalDate orderDate;

    private final String hearingDate;

    private final String hearingTime;

    private final  StatDecAppointmentLetterDefendant orderAddressee;

    private final CourtAddress courtAddress;

    public StatDacAppointmentLetterPayload(final List<String> caseApplicationReferences, final OrderingCourt orderingCourt, final LocalDate orderDate, final String hearingDate, final String hearingTime, final StatDecAppointmentLetterDefendant orderAddressee, final CourtAddress courtAddress) {
        this.caseApplicationReferences = new ArrayList<>(caseApplicationReferences);
        this.orderingCourt = orderingCourt;
        this.orderDate = orderDate;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
        this.orderAddressee = orderAddressee;
        this.courtAddress = courtAddress;
    }

    public List<String> getCaseApplicationReferences() {
        return new ArrayList<>(caseApplicationReferences);
    }

    public OrderingCourt getOrderingCourt() {
        return orderingCourt;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public String getHearingTime() {
        return hearingTime;
    }

    public StatDecAppointmentLetterDefendant getOrderAddressee() {
        return orderAddressee;
    }

    public CourtAddress getCourtAddress() {
        return courtAddress;
    }

    @Override
    public String toString() {
        return "StatDacAppointmentLetterPayload{" +
                "caseApplicationReferences='" + caseApplicationReferences + '\'' +
                ", orderingCourt='" + orderingCourt + '\'' +
                ", orderDate=" + orderDate +
                ", hearingDate='" + hearingDate + '\'' +
                ", hearingTime='" + hearingTime + '\'' +
                ", orderAddressee=" + orderAddressee +
                ", courtAddress=" + courtAddress +
                '}';
    }

    public static StatDacAppointmentLetterPayload.Builder builder() {
        return new StatDacAppointmentLetterPayload.Builder();
    }

    public static class Builder {

        private List<String> caseApplicationReferences;

        private  OrderingCourt orderingCourt;

        private  LocalDate orderDate;

        private String hearingDate;

        private String hearingTime;

        private StatDecAppointmentLetterDefendant orderAddressee;

        private CourtAddress courtAddress;

        public Builder withCaseApplicationReference(final List<String> caseApplicationReferences) {
            this.caseApplicationReferences = new ArrayList<>(caseApplicationReferences);
            return this;
        }

        public Builder withOrderingCourt(final OrderingCourt orderingCourt) {
            this.orderingCourt = orderingCourt;
            return this;
        }

        public Builder withOrderDate(final LocalDate orderDate) {
            this.orderDate = orderDate;
            return this;
        }

        public Builder withHearingDate(final String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withHearingTime(final String hearingTime) {
            this. hearingTime = hearingTime;
            return this;
        }

        public Builder withOrderAddresse(final StatDecAppointmentLetterDefendant orderAddressee) {
            this. orderAddressee = orderAddressee;
            return this;
        }

        public Builder withCourtAddress(final CourtAddress courtAddress) {
            this. courtAddress = courtAddress;
            return this;
        }

        public StatDacAppointmentLetterPayload build() {
            return new StatDacAppointmentLetterPayload(caseApplicationReferences, orderingCourt, orderDate,hearingDate,hearingTime, orderAddressee, courtAddress);
        }
    }


}
