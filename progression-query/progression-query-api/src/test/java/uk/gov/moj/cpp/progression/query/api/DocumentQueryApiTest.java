package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;


@RunWith(MockitoJUnitRunner.class)
public class DocumentQueryApiTest {
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @InjectMocks
    private CourtDocumentQueryApi target = new CourtDocumentQueryApi();

    @Test
    public void shouldHandleCourtDocument() {
        when(requester.request(query)).thenReturn(response);
        assertThat(target.getCourtDocument(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchCourtDocumentsQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(target.searchCourtDocuments(query), equalTo(response));
    }

}
