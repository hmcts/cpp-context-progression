package uk.gov.moj.cpp.progression.event.converter;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.event.converter.CaseSentToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

public class CaseSentToCrownCourtToCaseProgressionDetailConverterTest {

    private static final UUID CASE_PROGRESSION_ID = UUID.randomUUID();

    private static final UUID CASE_ID = UUID.randomUUID();

	private static final LocalDate DATE_OF_SENDING = LocalDate.now();
	
	private static final Long VERSION = 0L;

    private CaseSentToCrownCourtToCaseProgressionDetailConverter converter;
    
    private CaseSentToCrownCourt event;

    @Before
    public void setUp() throws Exception {
        converter = new CaseSentToCrownCourtToCaseProgressionDetailConverter();
        
        event = new CaseSentToCrownCourt(CASE_PROGRESSION_ID, CASE_ID, DATE_OF_SENDING, VERSION);
    }

    @Test
    public void testConvert() throws Exception {
        CaseProgressionDetail cpd = converter.convert(event);

        assertThat(cpd.getId(), equalTo(CASE_PROGRESSION_ID));
        assertThat(cpd.getCaseId(), equalTo(CASE_ID));
        assertThat(cpd.getDateOfSending(), equalTo(DATE_OF_SENDING));
        assertThat(cpd.getVersion(), equalTo(VERSION));
    }

}
