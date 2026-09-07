package uk.gov.moj.cpp.progression.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.OpaNotice;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OpaNoticeServiceTest {
    private static final String METADATA = "metadata";
    private static final String PAYLOAD = "payload";
    private static final String CASE_ID = "caseId";
    private static final String HEARING_ID = "hearingId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String TRIGGER_DATE = "triggerDate";
    private static final String OPA_NOTICE_KEY = "opaNotices";
    private static final String GENERATE_OPA_PUBLIC_LIST_NOTICE = "progression.command.generate-opa-public-list-notice";
    private static final String GENERATE_OPA_PRESS_LIST_NOTICE = "progression.command.generate-opa-press-list-notice";
    private static final String GENERATE_OPA_RESULT_LIST_NOTICE = "progression.command.generate-opa-result-list-notice";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);


    @Mock
    private UtcClock utcClock;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ExecutionService executionService;

    @InjectMocks
    private OpaNoticeService opaNoticeService;

    @Captor
    private ArgumentCaptor<ExecutionInfo> argCapture;

    @Test
    public void shouldGenerateOpaPublicListNotice() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop(GENERATE_OPA_PUBLIC_LIST_NOTICE);
        final Optional<JsonObject> opaNoticeList = getOpaNoticeList();

        when(progressionService.getPublicListNotices(jsonEnvelop)).thenReturn(opaNoticeList);
        when(utcClock.now()).thenReturn(ZonedDateTime.now());

        opaNoticeService.generateOpaPublicListNotice(jsonEnvelop);

        verify(executionService).executeWith(argCapture.capture());
        final JsonObject jobData = argCapture.getValue().getJobData();
        System.out.println(jobData.toString());
        final JsonObject payload = jobData.getJsonObject(PAYLOAD);

        assertThat(jobData.getString(METADATA), is(GENERATE_OPA_PUBLIC_LIST_NOTICE));
        assertThat(payload.getString(CASE_ID), is(notNullValue()));
        assertThat(payload.getString(HEARING_ID), is(notNullValue()));
        assertThat(payload.getString(DEFENDANT_ID), is(notNullValue()));
        assertThat(payload.getString(TRIGGER_DATE), is(LocalDate.now().toString()));
    }

    @Test
    public void shouldGenerateOpaPressListNotice() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop(GENERATE_OPA_PRESS_LIST_NOTICE);
        final Optional<JsonObject> opaNoticeList = getOpaNoticeList();

        when(progressionService.getPressListNotices(jsonEnvelop)).thenReturn(opaNoticeList);
        when(utcClock.now()).thenReturn(ZonedDateTime.now());

        opaNoticeService.generateOpaPressListNotice(jsonEnvelop);

        verify(executionService).executeWith(argCapture.capture());
        final JsonObject jobData = argCapture.getValue().getJobData();
        final JsonObject payload = jobData.getJsonObject(PAYLOAD);

        assertThat(jobData.getString(METADATA), is(GENERATE_OPA_PRESS_LIST_NOTICE));
        assertThat(payload.getString(CASE_ID), is(notNullValue()));
        assertThat(payload.getString(HEARING_ID), is(notNullValue()));
        assertThat(payload.getString(DEFENDANT_ID), is(notNullValue()));
        assertThat(payload.getString(TRIGGER_DATE), is(LocalDate.now().toString()));
    }

    @Test
    public void shouldGenerateOpaResultListNotice() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop(GENERATE_OPA_RESULT_LIST_NOTICE);
        final Optional<JsonObject> opaNoticeList = getOpaNoticeList();

        when(progressionService.getResultListNotices(jsonEnvelop)).thenReturn(opaNoticeList);
        when(utcClock.now()).thenReturn(ZonedDateTime.now());

        opaNoticeService.generateOpaResultListNotice(jsonEnvelop);

        verify(executionService).executeWith(argCapture.capture());
        final JsonObject jobData = argCapture.getValue().getJobData();
        final JsonObject payload = jobData.getJsonObject(PAYLOAD);

        assertThat(jobData.getString(METADATA), is(GENERATE_OPA_RESULT_LIST_NOTICE));
        assertThat(payload.getString(CASE_ID), is(notNullValue()));
        assertThat(payload.getString(HEARING_ID), is(notNullValue()));
        assertThat(payload.getString(DEFENDANT_ID), is(notNullValue()));
        assertThat(payload.getString(TRIGGER_DATE), is(LocalDate.now().toString()));
    }

    private Optional<JsonObject> getOpaNoticeList() {
        final OpaNotice opaNotice = OpaNotice.opaNotice()
                .withCaseId(randomUUID())
                .withHearingId(randomUUID())
                .withDefendantId(randomUUID()).build();

        final JsonArray jsonArray = createArrayBuilder()
                .add(objectToJsonObjectConverter.convert(opaNotice))
                .build();

        return of(createObjectBuilder().add(OPA_NOTICE_KEY, jsonArray)
                .build());
    }

    private JsonEnvelope getJsonEnvelop(final String commandName) {
        return envelopeFrom(
                metadataBuilder()
                        .createdAt(ZonedDateTime.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                createObjectBuilder().add(TRIGGER_DATE, LocalDate.now().toString()).build());
    }
}
