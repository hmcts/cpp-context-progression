package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.NowDocumentRequestQueryView;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowDocumentRequestQueryApiTest {
    private static final String NOW_DOCUMENT_TYPES_API = "progression.query.now-document-requests-by-request-id";
    @Mock
    private JsonEnvelope query;

    @Mock
    private NowDocumentRequestQueryView nowDocumentRequestQueryView;

    @Mock
    private JsonEnvelope response;

    @InjectMocks
    private NowDocumentRequestQueryApi nowDocumentRequestQueryApi;

    @Test
    public void shouldHandleNowDocumentRequestsQuery() {
        when(nowDocumentRequestQueryView.getNowDocumentRequestsByRequestId(query)).thenReturn(response);
        assertThat(nowDocumentRequestQueryApi.getNowDocumentRequests(query), equalTo(response));
    }

    @Test
    public void shouldHandleNowDocumentRequestsByHearingQuery() {
        when(nowDocumentRequestQueryView.getNowDocumentRequestByHearing(query)).thenReturn(response);
        assertThat(nowDocumentRequestQueryApi.getNowDocumentRequestsByHearing(query), equalTo(response));
    }
}
