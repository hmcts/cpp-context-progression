package uk.gov.moj.cpp.progression.query;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PressListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PublicListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ResultListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PressListOpaNoticeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PublicListOpaNoticeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ResultListOpaNoticeRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OpaNoticeQueryViewTest {


    private static final String QUERY = "progression.query.opa-notices";
    @Mock
    private JsonEnvelope query;

    @Mock
    private PublicListOpaNoticeRepository publicListOpaNoticeRepository;

    @Mock
    private PressListOpaNoticeRepository pressListOpaNoticeRepository;

    @Mock
    private ResultListOpaNoticeRepository resultListOpaNoticeRepository;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @InjectMocks
    private OpaNoticeQueryView opaNoticeQueryView;

    @Test
    public void shouldGetPublicListOpaNoticeView() throws IOException {
        final List<JsonObject> payload = getJsonObject();
        final List<PublicListOpaNotice> opaNotices = getResultEntities(payload, PublicListOpaNotice.class);

        when(query.metadata()).thenReturn(getMetadata());
        when(publicListOpaNoticeRepository.findAll()).thenReturn(opaNotices);

        final JsonEnvelope response = opaNoticeQueryView.getPublicListOpaNoticesView(query);

        verifyResponsePayloadContents(response, payload);
    }

    @Test
    public void shouldGetPressListOpaNoticeView() throws IOException {
        final List<JsonObject> payload = getJsonObject();
        final List<PressListOpaNotice> opaNotices = getResultEntities(payload, PressListOpaNotice.class);

        when(query.metadata()).thenReturn(getMetadata());
        when(pressListOpaNoticeRepository.findAll()).thenReturn(opaNotices);

        final JsonEnvelope response = opaNoticeQueryView.getPressListOpaNoticesView(query);

        verifyResponsePayloadContents(response, payload);
    }

    @Test
    public void shouldGetResultListOpaNoticeView() throws IOException {
        final List<JsonObject> payload = getJsonObject();
        final List<ResultListOpaNotice> opaNotices = getResultEntities(payload, ResultListOpaNotice.class);

        when(query.metadata()).thenReturn(getMetadata());
        when(resultListOpaNoticeRepository.findAll()).thenReturn(opaNotices);

        final JsonEnvelope response = opaNoticeQueryView.getResultListOpaNoticesView(query);

        verifyResponsePayloadContents(response, payload);
    }

    private List<JsonObject> getJsonObject() throws IOException {
        final List<PublicListOpaNotice> opaNotices = getOpaNotices();
        final String json = objectMapper.writeValueAsString(opaNotices);
        final JsonArray payload = objectMapper.readValue(json, JsonArray.class);

        return Arrays.asList(payload.getJsonObject(0), payload.getJsonObject(1));
    }

    private <T> List<T> getResultEntities(final List<JsonObject> payload, Class<T> clazz) {
        return payload.stream().map(json -> jsonToObjectConverter.convert(json, clazz))
                .collect(toList());
    }

    private void verifyResponsePayloadContents(final JsonEnvelope response, final List<JsonObject> opaNotices) {
        final Stream<JsonEnvelope> envelopeStream = Stream.of(response);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata()
                                .withName(QUERY),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.opaNotices[0].caseId", is(opaNotices.get(0).getString("caseId"))),
                                withJsonPath("$.opaNotices[0].defendantId", is(opaNotices.get(0).getString("defendantId"))),
                                withJsonPath("$.opaNotices[0].hearingId", is(opaNotices.get(0).getString("hearingId"))),
                                withJsonPath("$.opaNotices[1].caseId", is(opaNotices.get(1).getString("caseId"))),
                                withJsonPath("$.opaNotices[1].defendantId", is(opaNotices.get(1).getString("defendantId"))),
                                withJsonPath("$.opaNotices[1].hearingId", is(opaNotices.get(1).getString("hearingId"))))))
        ));
    }

    private List<PublicListOpaNotice> getOpaNotices() {
        final PublicListOpaNotice publicListOpaNotice = new PublicListOpaNotice(randomUUID(), randomUUID(), randomUUID());
        final PublicListOpaNotice publicListOpaNotice1 = new PublicListOpaNotice(randomUUID(), randomUUID(), randomUUID());

        return Arrays.asList(publicListOpaNotice, publicListOpaNotice1);
    }

    private static Metadata getMetadata() {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(QUERY)
                .build();
    }

}
