package uk.gov.moj.cpp.progression.service.payloads;

public class PublishCourtListDefendant {

    private final String defendantName;
    private final String defendantLastName;
    private final String defendantDateOfBirth;

    private PublishCourtListDefendant(final String defendantName, final String defendantLastName, final String defendantDateOfBirth) {
        this.defendantName = defendantName;
        this.defendantLastName = defendantLastName;
        this.defendantDateOfBirth = defendantDateOfBirth;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public String getDefendantDateOfBirth() {
        return defendantDateOfBirth;
    }

    public static PublishCourtListDefendantBuilder publishCourtListDefendantBuilder() {
        return new PublishCourtListDefendantBuilder();
    }

    public static final class PublishCourtListDefendantBuilder {
        private String defendantName;
        private String defendantLastName;
        private String defendantDateOfBirth;

        private PublishCourtListDefendantBuilder() {
        }

        public PublishCourtListDefendantBuilder withDefendantName(String defendantName) {
            this.defendantName = defendantName;
            return this;
        }

        public PublishCourtListDefendantBuilder withDefendantLastName(String defendantLastName) {
            this.defendantLastName = defendantLastName;
            return this;
        }

        public PublishCourtListDefendantBuilder withDefendantDateOfBirth(String defendantDateOfBirth) {
            this.defendantDateOfBirth = defendantDateOfBirth;
            return this;
        }

        public PublishCourtListDefendant build() {
            return new PublishCourtListDefendant(defendantName, defendantLastName, defendantDateOfBirth);
        }
    }
}
