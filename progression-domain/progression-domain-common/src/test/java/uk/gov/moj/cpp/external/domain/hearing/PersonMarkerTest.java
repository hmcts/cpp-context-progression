package uk.gov.moj.cpp.external.domain.hearing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PersonMarkerTest {

    private static final String INTERPRETER = PersonMarker.INTERPRETER.getclassification();
    private static final String EXPERT = PersonMarker.EXPERT.getclassification();
    private static final String PROFESSIONAL = PersonMarker.PROFESSIONAL.getclassification();
    private static final String POLICE = "Police";

    @Test
    public void testgetValidPersonClassification() {
        PersonMarker validMarker = PersonMarker.getValidPersonClassification("Police",
                        PersonMarker.PROFESSIONAL);
        assertThat(validMarker, is(PersonMarker.PROFESSIONAL));

        validMarker = PersonMarker.getValidPersonClassification(PROFESSIONAL, PersonMarker.EXPERT);
        assertThat(validMarker, is(PersonMarker.EXPERT));

        validMarker = PersonMarker.getValidPersonClassification(EXPERT, PersonMarker.PROFESSIONAL);
        assertThat(validMarker, is(PersonMarker.EXPERT));

        validMarker = PersonMarker.getValidPersonClassification(INTERPRETER, PersonMarker.EXPERT);
        assertThat(validMarker, is(PersonMarker.EXPERT));

        validMarker = PersonMarker.getValidPersonClassification(EXPERT, PersonMarker.INTERPRETER);
        assertThat(validMarker, is(PersonMarker.EXPERT));

        validMarker = PersonMarker.getValidPersonClassification(INTERPRETER,
                        PersonMarker.PROFESSIONAL);
        assertThat(validMarker, is(PersonMarker.PROFESSIONAL));

        validMarker = PersonMarker.getValidPersonClassification(PROFESSIONAL,
                        PersonMarker.INTERPRETER);
        assertThat(validMarker, is(PersonMarker.PROFESSIONAL));

        validMarker = PersonMarker.getValidPersonClassification(INTERPRETER,
                        PersonMarker.UNSPECIFIED);
        assertThat(validMarker, is(PersonMarker.UNSPECIFIED));


        validMarker = PersonMarker.getValidPersonClassification(POLICE, PersonMarker.INTERPRETER);
        assertThat(validMarker, is(PersonMarker.UNSPECIFIED));

        validMarker = PersonMarker.getValidPersonClassification(POLICE, PersonMarker.UNSPECIFIED);
        assertThat(validMarker, is(PersonMarker.UNSPECIFIED));
    }


}
