package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00121", "pmd:BeanMembersShouldSerialize", "squid:S00107", "squid:S00122", "squid:S2384"})
public class Offence {
    private Optional<String> endDate;

    private final UUID id;

    private final String offenceCode;

    private final String offenceWording;

    private final String startDate;

    private final StatementOfOffence statementOfOffence;

    private Optional<CustodyTimeLimit> custodyTimeLimit;

    private Optional<LaaReference> laaApplnReference;

    private Optional<String> laidDate;

    private Optional<Boolean> shadowListed;

    private Optional<CommittingCourt> committingCourt;

    private final Integer count;
    private final Integer orderIndex;

    private List<ReportingRestriction> reportingRestrictions;

    private Optional<SeedingHearing> seedingHearing;

    private String indictmentParticular;

    private CivilOffence civilOffence;

    public Offence(final Optional<String> endDate, final UUID id, final String offenceCode, final String offenceWording,
                   final String startDate, final StatementOfOffence statementOfOffence, final Optional<CustodyTimeLimit> custodyTimeLimit,
                   final Optional<LaaReference> laaApplnReference, final Optional<String> laidDate, final Optional<Boolean> shadowListed,
                   final Optional<CommittingCourt> committingCourt, final List<ReportingRestriction> reportingRestrictions,
                   final Optional<SeedingHearing> seedingHearing, final Integer count, final Integer orderIndex, String indictmentParticular, CivilOffence civilOffence) {
        this.endDate = endDate;
        this.id = id;
        this.offenceCode = offenceCode;
        this.offenceWording = offenceWording;
        this.startDate = startDate;
        this.statementOfOffence = statementOfOffence;
        this.custodyTimeLimit = custodyTimeLimit;
        this.laaApplnReference = laaApplnReference;
        this.laidDate = laidDate;
        this.shadowListed = shadowListed;
        this.committingCourt = committingCourt;
        this.reportingRestrictions = reportingRestrictions;
        this.seedingHearing = seedingHearing;
        this.count = count;
        this.orderIndex = orderIndex;
        this.indictmentParticular = indictmentParticular;
        this.civilOffence = civilOffence;
    }

    public Optional<String> getEndDate() {
        return endDate.isPresent() ? endDate : empty();
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public Optional<LaaReference> getLaaApplnReference() {
        return laaApplnReference.isPresent() ? laaApplnReference : empty();
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public String getStartDate() {
        return startDate;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }

    public Optional<CustodyTimeLimit> getCustodyTimeLimit() {
        return custodyTimeLimit.isPresent() ? custodyTimeLimit : empty();
    }

    public Optional<String> getLaidDate() {
        return laidDate.isPresent() ? laidDate : empty();
    }

    public Optional<Boolean> getShadowListed() {
        return shadowListed.isPresent() ? shadowListed : empty();
    }

    public Optional<CommittingCourt> getCommittingCourt() {
        return committingCourt.isPresent() ? committingCourt : empty();
    }

    public List<ReportingRestriction> getReportingRestrictions() {
        return reportingRestrictions;
    }

    public Optional<SeedingHearing> getSeedingHearing() {
        return seedingHearing.isPresent() ? seedingHearing : empty();
    }

    public Integer getCount() {
        return count;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public String getIndictmentParticular() {
        return indictmentParticular;
    }

    public CivilOffence getCivilOffence() { return civilOffence; }

    public static Builder offence() {
        return new Offence.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Offence offence = (Offence) o;
        return Objects.equals(endDate, offence.endDate) &&
                Objects.equals(id, offence.id) &&
                Objects.equals(offenceCode, offence.offenceCode) &&
                Objects.equals(offenceWording, offence.offenceWording) &&
                Objects.equals(startDate, offence.startDate) &&
                Objects.equals(statementOfOffence, offence.statementOfOffence) &&
                Objects.equals(custodyTimeLimit, offence.custodyTimeLimit) &&
                Objects.equals(laaApplnReference, offence.laaApplnReference) &&
                Objects.equals(laidDate, offence.laidDate) &&
                Objects.equals(shadowListed, offence.shadowListed) &&
                Objects.equals(committingCourt, offence.committingCourt) &&
                Objects.equals(reportingRestrictions, offence.reportingRestrictions) &&
                Objects.equals(count, offence.count) &&
                Objects.equals(orderIndex, offence.orderIndex) &&
                Objects.equals(seedingHearing, offence.seedingHearing) &&
                Objects.equals(indictmentParticular, offence.indictmentParticular) &&
                Objects.equals(civilOffence, offence.civilOffence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endDate, id, offenceCode, offenceWording, startDate, statementOfOffence, custodyTimeLimit, laaApplnReference, laidDate, shadowListed, committingCourt, reportingRestrictions, seedingHearing, count, orderIndex, indictmentParticular, civilOffence);
    }

    @Override
    public String toString() {
        return "Offence{" +
                "endDate=" + endDate +
                ", id=" + id +
                ", offenceCode='" + offenceCode + '\'' +
                ", offenceWording='" + offenceWording + '\'' +
                ", startDate='" + startDate + '\'' +
                ", statementOfOffence=" + statementOfOffence +
                ", custodyTimeLimit=" + custodyTimeLimit +
                ", laaApplnReference=" + laaApplnReference +
                ", laidDate=" + laidDate +
                ", shadowListed=" + shadowListed +
                ", committingCourt=" + committingCourt +
                ", count=" + count +
                ", orderIndex=" + orderIndex +
                ", reportingRestrictions=" + reportingRestrictions +
                ", seedingHearing=" + seedingHearing +
                ", indictmentParticular=" + indictmentParticular +
                ", civilOffence=" + civilOffence +
                '}';
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private Optional<String> endDate = empty();

        private UUID id;

        private String offenceCode;

        private String offenceWording;

        private String startDate;

        private StatementOfOffence statementOfOffence;

        private Optional<CustodyTimeLimit> custodyTimeLimit = empty();

        private Optional<String> laidDate = empty();

        private Optional<LaaReference> laaApplnReference = empty();

        private Optional<Boolean> shadowListed = empty();

        private Optional<CommittingCourt> committingCourt = empty();

        private List<ReportingRestriction> reportingRestrictions;

        private Optional<SeedingHearing> seedingHearing = empty();

        private Integer count;
        private Integer orderIndex;
        private String indictmentParticular;

        private CivilOffence civilOffence;

        public Builder withEndDate(final Optional<String> endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withOffenceWording(final String offenceWording) {
            this.offenceWording = offenceWording;
            return this;
        }

        public Builder withStartDate(final String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withCustodyTimeLimit(final Optional<CustodyTimeLimit> custodyTimeLimit) {
            this.custodyTimeLimit = custodyTimeLimit;
            return this;
        }

        public Builder withStatementOfOffence(final StatementOfOffence statementOfOffence) {
            this.statementOfOffence = statementOfOffence;
            return this;
        }

        public Builder withLaaApplnReference(final Optional<LaaReference> laaApplnReferences) {
            this.laaApplnReference = laaApplnReferences;
            return this;
        }

        public Builder withLaidDate(final Optional<String> laidDate) {
            this.laidDate = laidDate;
            return this;
        }

        public Builder withShadowListed(final Optional<Boolean> shadowListed) {
            this.shadowListed = shadowListed;
            return this;
        }

        public Builder withCommittingCourt(final Optional<CommittingCourt> committingCourt) {
            this.committingCourt = committingCourt;
            return this;
        }

        public Builder withReportingRestrictions(final List<ReportingRestriction> reportingRestrictions) {
            this.reportingRestrictions = reportingRestrictions;
            return this;
        }

        public Builder withSeedingHearing(final Optional<SeedingHearing> seedingHearing) {
            this.seedingHearing = seedingHearing;
            return this;
        }

        public Builder withCount(final Integer count) {
            this.count = count;
            return this;
        }

        public Builder withOrderIndex(final Integer orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public Builder withIndictmentParticular(final String indictmentParticular) {
            this.indictmentParticular = indictmentParticular;
            return this;
        }

        public Builder withCivilOffence(final CivilOffence civilOffence) {
            this.civilOffence = civilOffence;
            return this;
        }

        public Offence build() {
            return new Offence(endDate, id, offenceCode, offenceWording, startDate, statementOfOffence, custodyTimeLimit, laaApplnReference, laidDate, shadowListed, committingCourt, reportingRestrictions, seedingHearing, count, orderIndex, indictmentParticular, civilOffence);
        }
    }
}
