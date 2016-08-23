package uk.gov.moj.cpp.progression.event.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.event.converter.CaseAddedToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

public class CaseAddedToCrownCourtToCaseProgressionDetailConverterTest {

    private static final UUID DEF_ID = UUID.randomUUID();

    private static final UUID CASE_PROGRESSION_ID = DEF_ID;

    private static final UUID CASE_ID = DEF_ID;

    private static final String COURT_CENTRE_ID = "Liverpool";


    private CaseAddedToCrownCourtToCaseProgressionDetailConverter converter;

    private CaseAddedToCrownCourt event;

    @Before
    public void setUp() throws Exception {
        converter = new CaseAddedToCrownCourtToCaseProgressionDetailConverter();

        event = new CaseAddedToCrownCourt(CASE_PROGRESSION_ID, CASE_ID, COURT_CENTRE_ID,
                        Arrays.asList(new Defendant(DEF_ID)));
    }

    @Test
    public void testConvert() throws Exception {
        CaseProgressionDetail cpd = converter.convert(event);

        assertThat(cpd.getId(), equalTo(CASE_PROGRESSION_ID));
        assertThat(cpd.getCaseId(), equalTo(CASE_ID));
        assertThat(cpd.getCourtCentreId(), equalTo(COURT_CENTRE_ID));
        assertThat(cpd.getDefendants().iterator().next().getDefendantId(), equalTo(DEF_ID));
    }

}
