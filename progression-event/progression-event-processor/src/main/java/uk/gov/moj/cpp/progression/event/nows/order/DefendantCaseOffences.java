package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class DefendantCaseOffences implements Serializable {
    private static final long serialVersionUID = 1870765747443534132L;

    private final String convictionDate;

    private final List<Results> results;

    private final String startDate;

    private final String wording;

    public DefendantCaseOffences(final String convictionDate, final List<Results> results, final String startDate, final String wording) {
        this.convictionDate = convictionDate;
        this.results = results;
        this.startDate = startDate;
        this.wording = wording;
    }

    public String getConvictionDate() {
        return convictionDate;
    }

    public List<Results> getResults() {
        return results;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getWording() {
        return wording;
    }

    public static Builder defendantCaseOffences() {
        return new DefendantCaseOffences.Builder();
    }

    public static class Builder {
        private String convictionDate;

        private List<Results> results;

        private String startDate;

        private String wording;

        public Builder withConvictionDate(final String convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public Builder withResults(final List<Results> results) {
            this.results = results;
            return this;
        }

        public Builder withStartDate(final String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withWording(final String wording) {
            this.wording = wording;
            return this;
        }

        public DefendantCaseOffences build() {
            return new DefendantCaseOffences(convictionDate, results, startDate, wording);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantCaseOffences that = (DefendantCaseOffences) o;
        return Objects.equals(convictionDate, that.convictionDate) &&
                Objects.equals(results, that.results) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(wording, that.wording);
    }

    @Override
    public int hashCode() {
        return Objects.hash(convictionDate, results, startDate, wording);
    }
}
