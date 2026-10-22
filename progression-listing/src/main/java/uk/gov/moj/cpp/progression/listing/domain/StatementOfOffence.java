package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.util.Optional;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class StatementOfOffence {
  private final Optional<String>  legislation;

  private final String title;

  private final Optional<String> welshLegislation;

  private final String welshTitle;

  public StatementOfOffence(final Optional<String>  legislation, final String title, final Optional<String> welshLegislation, final String welshTitle) {
    this.legislation = legislation;
    this.title = title;
    this.welshLegislation = welshLegislation;
    this.welshTitle = welshTitle;
  }

  public Optional<String>  getLegislation() {
    return legislation;
  }

  public String getTitle() {
    return title;
  }

  public Optional<String> getWelshLegislation() {
    return welshLegislation;
  }

  public String getWelshTitle() {
    return welshTitle;
  }

  public static Builder statementOfOffence() {
    return new StatementOfOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final StatementOfOffence that = (StatementOfOffence) obj;

    return java.util.Objects.equals(this.legislation, that.legislation) &&
    java.util.Objects.equals(this.title, that.title) &&
    java.util.Objects.equals(this.welshLegislation, that.welshLegislation) &&
    java.util.Objects.equals(this.welshTitle, that.welshTitle);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(legislation, title, welshLegislation, welshTitle);}

  @Override
  public String toString() {
    return "StatementOfOffence{" +
    	"legislation='" + legislation + "'," +
    	"title='" + title + "'," +
    	"welshLegislation='" + welshLegislation + "'," +
    	"welshTitle='" + welshTitle + "'" +
    "}";
  }

  public static class Builder {
    private Optional<String>  legislation = empty();

    private String title;

    private Optional<String> welshLegislation = empty();

    private String welshTitle;

    public Builder withLegislation(final Optional<String>  legislation) {
      this.legislation = legislation;
      return this;
    }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withWelshLegislation(final Optional<String> welshLegislation) {
      this.welshLegislation = welshLegislation;
      return this;
    }

    public Builder withWelshTitle(final String welshTitle) {
      this.welshTitle = welshTitle;
      return this;
    }

    public StatementOfOffence build() {
      return new StatementOfOffence(legislation, title, welshLegislation, welshTitle);
    }
  }
}
