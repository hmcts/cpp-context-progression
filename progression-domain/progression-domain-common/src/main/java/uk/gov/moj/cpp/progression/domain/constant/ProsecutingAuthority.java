package uk.gov.moj.cpp.progression.domain.constant;

public enum ProsecutingAuthority {
    CPS("CPS");
    private String description;

    private ProsecutingAuthority(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ProsecutingAuthority getProsecutingAuthority(final String value) {
        final ProsecutingAuthority[] prosecutingAuthorities = ProsecutingAuthority.values();
        for (final ProsecutingAuthority caseStatus : prosecutingAuthorities) {
            if (caseStatus.getDescription().equals(value)) {
                return caseStatus;
            }
        }
        return null;
    }
}
