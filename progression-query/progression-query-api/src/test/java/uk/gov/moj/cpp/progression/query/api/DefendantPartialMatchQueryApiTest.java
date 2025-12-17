package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.DefendantPartialMatchQueryView;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
public class DefendantPartialMatchQueryApiTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private DefendantPartialMatchQueryView defendantPartialMatchQueryView;

    @InjectMocks
    private DefendantPartialMatchQueryApi defendantPartialMatchQueryApi;

    @Test
    public void shouldHandlePartialMatchQuery() {
        when(defendantPartialMatchQueryView.getDefendantPartialMatches(query)).thenReturn(response);
        assertThat(defendantPartialMatchQueryApi.getPartialMatchDefendant(query), equalTo(response));
    }
}
