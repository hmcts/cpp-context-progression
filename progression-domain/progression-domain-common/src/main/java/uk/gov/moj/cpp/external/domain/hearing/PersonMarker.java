package uk.gov.moj.cpp.external.domain.hearing;

public enum PersonMarker {
    EXPERT("Expert", 3), PROFESSIONAL("Professional", 2), UNSPECIFIED("Unspecified",
                    1), INTERPRETER("Interpreter", 0);

    private final String classification;
    private final int priority;

    PersonMarker(final String classification, final int priority) {
        this.classification = classification;
        this.priority = priority;
    }


    public String getclassification() {
        return classification;
    }


    public static PersonMarker getValidPersonClassification(final String classification,
                    final PersonMarker lastPersonMarker) {
        final PersonMarker personMarkerProvided = getPersonMarkerProvided(classification);
        if (personMarkerProvided.priority > lastPersonMarker.priority) {
            return personMarkerProvided;
        }
        return lastPersonMarker;
    }



    public static PersonMarker getPersonMarkerProvided(final String classification) {

        for (final PersonMarker personMarker : PersonMarker.values()) {
            if (personMarker.classification.equalsIgnoreCase(classification)) {
                return personMarker;
            }
        }
        return PersonMarker.UNSPECIFIED;
    }


}
