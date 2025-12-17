package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.SharedCourtDocumentsQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedCourtDocumentQueryApiTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @InjectMocks
    private SharedCourtDocumentQueryApi sharedCourtDocumentQueryApi;

    @Test
    void shouldGetApplicationSharedCourtDocumentsLinks() {
        when(sharedCourtDocumentsQueryView.getApplicationSharedCourtDocumentsLinks(query)).thenReturn(response);

        assertThat(response, equalTo(sharedCourtDocumentQueryApi.getApplicationSharedCourtDocumentsLinks(query)));
    }

}
