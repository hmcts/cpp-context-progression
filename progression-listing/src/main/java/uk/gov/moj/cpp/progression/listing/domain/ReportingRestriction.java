package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067" })
public class ReportingRestriction {
  private UUID id;

  private Optional<UUID> judicialResultId;

  private String label;

  private Optional<LocalDate> orderedDate;

  public ReportingRestriction(final UUID id, final Optional<UUID> judicialResultId, final String label, final Optional<LocalDate> orderedDate) {
    this.id = id;
    this.judicialResultId = judicialResultId;
    this.label = label;
    this.orderedDate = orderedDate;
  }

  public UUID getId() {
    return id;
  }

  public Optional<UUID> getJudicialResultId() {
    return judicialResultId;
  }

  public String getLabel() {
    return label;
  }

  public Optional<LocalDate> getOrderedDate() {
    return orderedDate;
  }

  public static Builder reportingRestriction() {
    return new ReportingRestriction.Builder();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ReportingRestriction that = (ReportingRestriction) o;
    return Objects.equals(id, that.id) &&
            Objects.equals(judicialResultId, that.judicialResultId) &&
            Objects.equals(label, that.label) &&
            Objects.equals(orderedDate, that.orderedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, judicialResultId, label, orderedDate);
  }

  @Override
  public String toString() {
    return "ReportingRestriction{" +
            "id=" + id +
            ", judicialResultId=" + judicialResultId +
            ", label='" + label + '\'' +
            ", orderedDate=" + orderedDate +
            '}';
  }

  public static class Builder {
    private UUID id;

    private Optional<UUID> judicialResultId = empty();

    private String label;

    private Optional<LocalDate> orderedDate = empty();

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withJudicialResultId(final Optional<UUID> judicialResultId) {
      this.judicialResultId = judicialResultId;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withOrderedDate(final Optional<LocalDate> orderedDate) {
      this.orderedDate = orderedDate;
      return this;
    }

    public ReportingRestriction build() {
      return new ReportingRestriction(id, judicialResultId, label, orderedDate);
    }
  }
}
