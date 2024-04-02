package uk.gov.moj.cpp.progression.task;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.progression.task.Task.GENERATE_OPA_NOTICE;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;

import java.io.IOException;
import java.time.ZonedDateTime;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class GenerateOpaNoticeTaskTest {
    private static final String GENERATE_OPA_PUBLIC_LIST_NOTICE = "progression.generate-opa-public-list-notice";
    private static final String GENERATE_OPA_PRESS_LIST_NOTICE = "progression.generate-opa-press-list-notice";
    private static final String GENERATE_OPA_RESULT_LIST_NOTICE = "progression.generate-opa-result-list-notice";

    @Mock
    private UtcClock utcClock;

    @Mock
    private Logger logger;

    @Mock
    private Sender sender;
    @InjectMocks
    private GenerateOpaNoticeTask generateOpaNoticeTask;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    private ZonedDateTime now = ZonedDateTime.now();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private StringToJsonObjectConverter jsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setup() {
        when(utcClock.now()).thenReturn(now);
    }

    @Test
    public void shouldExecuteGeneratePublicListOpaNotice() throws IOException {
        final JsonObject payload = getPayload(GENERATE_OPA_PUBLIC_LIST_NOTICE);

        final ExecutionInfo executionInfo = new ExecutionInfo(payload, GENERATE_OPA_NOTICE.getTaskName(), utcClock.now(), STARTED);
        final ExecutionInfo response = generateOpaNoticeTask.execute(executionInfo);

        assertThat(response.getExecutionStatus(), is(ExecutionStatus.COMPLETED));

        verify(sender).send(argThat(jsonEnvelope(
                metadata().withName(GENERATE_OPA_PUBLIC_LIST_NOTICE),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is("bad36542-8ac6-48b4-9553-904243ea994b")),
                        withJsonPath("$.defendantId", is("c84abb2b-ae18-47fc-bc69-93ea20e9ccc9")),
                        withJsonPath("$.hearingId", is("e9367615-c663-4011-90fb-9f761302c799")),
                        withJsonPath("$.triggerDate", is("2023-12-12")))
                ))));
    }

    @Test
    public void shouldExecuteGeneratePressListOpaNotice() throws IOException {
        final JsonObject payload = getPayload(GENERATE_OPA_PRESS_LIST_NOTICE);

        final ExecutionInfo executionInfo = new ExecutionInfo(payload, GENERATE_OPA_NOTICE.getTaskName(), utcClock.now(), STARTED);
        final ExecutionInfo response = generateOpaNoticeTask.execute(executionInfo);

        assertThat(response.getExecutionStatus(), is(ExecutionStatus.COMPLETED));

        verify(sender).send(argThat(jsonEnvelope(
                metadata().withName(GENERATE_OPA_PRESS_LIST_NOTICE),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is("bad36542-8ac6-48b4-9553-904243ea994b")),
                        withJsonPath("$.defendantId", is("c84abb2b-ae18-47fc-bc69-93ea20e9ccc9")),
                        withJsonPath("$.hearingId", is("e9367615-c663-4011-90fb-9f761302c799")),
                        withJsonPath("$.triggerDate", is("2023-12-12")))
                ))));
    }

    @Test
    public void shouldExecuteGenerateResultListOpaNotice() throws IOException {
        final JsonObject payload = getPayload(GENERATE_OPA_RESULT_LIST_NOTICE);

        final ExecutionInfo executionInfo = new ExecutionInfo(payload, GENERATE_OPA_NOTICE.getTaskName(), utcClock.now(), STARTED);
        final ExecutionInfo response = generateOpaNoticeTask.execute(executionInfo);

        assertThat(response.getExecutionStatus(), is(ExecutionStatus.COMPLETED));

        verify(sender).send(argThat(jsonEnvelope(
                metadata().withName(GENERATE_OPA_RESULT_LIST_NOTICE),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is("bad36542-8ac6-48b4-9553-904243ea994b")),
                        withJsonPath("$.defendantId", is("c84abb2b-ae18-47fc-bc69-93ea20e9ccc9")),
                        withJsonPath("$.hearingId", is("e9367615-c663-4011-90fb-9f761302c799")),
                        withJsonPath("$.triggerDate", is("2023-12-12")))
                ))));
    }

    private JsonObject getPayload(final String metadata) throws IOException {
        final String json = Resources.toString(getResource("generate-opa-notice.json"), defaultCharset())
                .replaceAll("%METADATA%", metadata);

        return jsonObjectConverter.convert(json);
    }
}
