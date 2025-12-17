package uk.gov.moj.cpp.progression.handler.courts.document;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;

import uk.gov.justice.core.courts.CourtDocument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DefaultCourtDocumentFactoryTest {

    @InjectMocks
    private DefaultCourtDocumentFactory defaultCourtDocumentFactory;

    @Test
    public void shouldCreateCourtDocumentWithFinancialMeansFalseIfNotSet() throws Exception {

        final CourtDocument courtDocument = courtDocument().build();
        assertThat(courtDocument.getContainsFinancialMeans(), is(nullValue()));

        final CourtDocument defaultCourtDocument = defaultCourtDocumentFactory.createDefaultCourtDocument(courtDocument);
        assertThat(defaultCourtDocument, is(notNullValue()));
        assertThat(defaultCourtDocument.getContainsFinancialMeans(), is(false));
    }
}
