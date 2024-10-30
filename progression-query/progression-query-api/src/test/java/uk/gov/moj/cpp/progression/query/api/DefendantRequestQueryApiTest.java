package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.DefendantPartialMatchQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantRequestQueryApiTest {
    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @InjectMocks
    private DefendantRequestQueryApi defendantRequestQueryApi;

    @Mock
    private DefendantPartialMatchQueryView defendantPartialMatchQueryView;

    @Test
    public void shouldHandleGetDefendantRequest() {
        when(defendantPartialMatchQueryView.getDefendantPartialMatches(query)).thenReturn(response);
        assertThat(defendantRequestQueryApi.getDefendantRequest(query), equalTo(response));
    }
}
