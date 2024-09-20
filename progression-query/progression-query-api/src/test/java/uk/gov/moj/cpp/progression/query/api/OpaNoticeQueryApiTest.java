package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.OpaNoticeQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OpaNoticeQueryApiTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private OpaNoticeQueryView opaNoticeQueryView;

    @InjectMocks
    private OpaNoticeQueryApi opaNoticeQueryApi;

    @Test
    public void shouldGetPublicListOpaNotices() {
        when(opaNoticeQueryView.getPublicListOpaNoticesView(query)).thenReturn(response);

        final JsonEnvelope opaNotices = opaNoticeQueryApi.getPublicListOpaNotices(query);

        verify(opaNoticeQueryView).getPublicListOpaNoticesView(query);
        assertThat(opaNotices, is(response));
    }

    @Test
    public void shouldGetPressListOpaNotices() {
        when(opaNoticeQueryView.getPressListOpaNoticesView(query)).thenReturn(response);

        final JsonEnvelope opaNotices = opaNoticeQueryApi.getPressListOpaNotices(query);

        verify(opaNoticeQueryView).getPressListOpaNoticesView(query);
        assertThat(opaNotices, is(response));
    }

    @Test
    public void shouldGetResultListOpaNotices() {
        when(opaNoticeQueryView.getResultListOpaNoticesView(query)).thenReturn(response);

        final JsonEnvelope opaNotices = opaNoticeQueryApi.getResultListOpaNotices(query);

        verify(opaNoticeQueryView).getResultListOpaNoticesView(query);
        assertThat(opaNotices, is(response));
    }
}
