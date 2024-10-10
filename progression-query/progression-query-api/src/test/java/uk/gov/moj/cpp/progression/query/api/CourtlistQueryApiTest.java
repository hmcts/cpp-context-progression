package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CourtlistQueryView;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
public class CourtlistQueryApiTest {


    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private CourtlistQueryView courtlistQueryView;

    @InjectMocks
    private CourtlistQueryApi courtListQueryApi;


    @Test
    public void shouldHandleApplicationQuery() {
        when(courtlistQueryView.searchCourtlist(query)).thenReturn(response);
        assertThat(courtListQueryApi.searchCourtlist(query), equalTo(response));
    }

    @Test
    public void shouldHandlePrisonCourtListQuery() {
        when(courtlistQueryView.searchPrisonCourtlist(query)).thenReturn(response);
        assertThat(courtListQueryApi.searchPrisonCourtlist(query), equalTo(response));
    }
}
