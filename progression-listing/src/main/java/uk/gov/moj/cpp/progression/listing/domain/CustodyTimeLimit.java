package uk.gov.moj.cpp.progression.listing.domain;

@SuppressWarnings("pmd:BeanMembersShouldSerialize")
public class CustodyTimeLimit {
  private final Integer daysSpent;

  private final String timeLimit;

  public CustodyTimeLimit(final Integer daysSpent, final String timeLimit) {
    this.daysSpent = daysSpent;
    this.timeLimit = timeLimit;
  }

  public static Builder custodyTimeLimit() {
    return new CustodyTimeLimit.Builder();
  }

  public Integer getDaysSpent() {
    return daysSpent;
  }

  public String getTimeLimit() {
    return timeLimit;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CustodyTimeLimit that = (CustodyTimeLimit) obj;

    return java.util.Objects.equals(this.daysSpent, that.daysSpent) &&
            java.util.Objects.equals(this.timeLimit, that.timeLimit);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(daysSpent, timeLimit);
  }

  @Override
  public String toString() {
    return "CustodyTimeLimit{" +
            "daysSpent='" + daysSpent + "'," +
            "timeLimit='" + timeLimit + "'" +
            "}";
  }

  @SuppressWarnings("pmd:BeanMembersShouldSerialize")
  public static class Builder {
    private Integer daysSpent;

    private String timeLimit;

    public Builder withDaysSpent(final Integer daysSpent) {
      this.daysSpent = daysSpent;
      return this;
    }

    public Builder withTimeLimit(final String timeLimit) {
      this.timeLimit = timeLimit;
      return this;
    }

    public CustodyTimeLimit build() {
      return new CustodyTimeLimit(daysSpent, timeLimit);
    }
  }
}
