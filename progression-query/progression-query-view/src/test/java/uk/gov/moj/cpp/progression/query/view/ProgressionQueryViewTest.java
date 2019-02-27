package uk.gov.moj.cpp.progression.query.view;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class ProgressionQueryViewTest extends AbstractProgressionQueryBaseTest {
    
    @Mock
    private JsonEnvelope query;
    
    @Mock
    private JsonEnvelope response;

    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Enveloper enveloper;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @Mock
    private CaseProgressionDetailService casePrgDetailService;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private JsonEnvelope responseJson;

    @InjectMocks
    private ProgressionQueryView queryView;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }

    @Test
    public void shouldGetCaseProgressionDetailsGivenACaseId() throws Exception {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getCaseProgressionDetail(caseId))
                .thenReturn(getCaseProgressionDetailView(caseId, defendantId));

        when(enveloper.withMetadataFrom(query,
                        ProgressionQueryView.CASE_PROGRESSION_DETAILS_RESPONSE))
                                        .thenReturn(function);
        when(function.apply(anyObject())).thenReturn(responseJson);

        final JsonEnvelope response = queryView.getCaseProgressionDetails(query);

        verify(casePrgDetailService, atMost(1)).getCaseProgressionDetail(anyObject());
        verify(query, atMost(2)).payloadAsJsonObject();
        verify(objectMapper, atMost(1)).writeValueAsString(anyObject());
        verify(enveloper, atMost(1)).withMetadataFrom(anyObject(), anyString());
        verify(function, atMost(2)).apply(anyObject());
        assertThat(response, equalTo(responseJson));
    }

    @Test(expected = javax.persistence.NoResultException.class)
    public void shouldReturnNoResultForCaseProgressionDetailsGivenASpecificCaseId() {
        final UUID caseId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getCaseProgressionDetail(caseId))
                        .thenThrow(new NoResultException());
        when(enveloper.withMetadataFrom(query,
                        ProgressionQueryView.CASE_PROGRESSION_DETAILS_RESPONSE))
                                        .thenReturn(function);

        when(function.apply(null)).thenReturn(responseJson);
        assertThat(queryView.getCaseProgressionDetails(query), equalTo(responseJson));
    }

    @Test
    public void shouldGetMultipleCaseProgressionDetailsThatAreReadyForReview() throws Exception {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final Optional<String> status = Optional.of("READY_FOR_REVIEW");
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_STATUS, status.get())
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getCases(Optional.of(anyString()), anyObject())).thenReturn(Arrays.asList(getCaseProgressionDetailView(caseId, defendantId)));

        when(enveloper.withMetadataFrom(anyObject(), anyString())).thenReturn(function);
        when(function.apply(anyObject())).thenReturn(responseJson);

        final JsonEnvelope response = queryView.getCases(query);

        verify(casePrgDetailService, atMost(1)).getCases(anyObject());
        verify(query, atMost(2)).payloadAsJsonObject();
        verify(objectMapper, atMost(1)).writeValueAsString(anyObject());
        verify(enveloper, atMost(1)).withMetadataFrom(anyObject(), anyString());
        verify(function,atMost(2)).apply(anyObject());
        assertThat(response, equalTo(responseJson));
    }

    @Test
    public void shouldGetCaseProgressionDetailsWithCaseId() throws Exception {

        reset(query);
        final Optional<UUID> caseId = Optional.of(UUID.randomUUID());
        final UUID defendantId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(ProgressionQueryView.FIELD_CASE_ID, caseId.get().toString()).build();


        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getCaseProgressionDetail(caseId.get())).thenReturn(getCaseProgressionDetailView(caseId.get(), defendantId));
        when(enveloper.withMetadataFrom(anyObject(), anyString())).thenReturn(function);
        when(function.apply(anyObject())).thenReturn(responseJson);

        final JsonEnvelope response = queryView.getCases(query);

        verify(casePrgDetailService, atMost(1)).getCases(anyObject());
        verify(query, atMost(2)).payloadAsJsonObject();
        verify(objectMapper, atMost(1)).writeValueAsString(anyObject());
        verify(enveloper, atMost(1)).withMetadataFrom(anyObject(), anyString());
        verify(function,atMost(2)).apply(anyObject());
        assertThat(response, equalTo(responseJson));
    }

    @Test
    public void shouldGetMultipleDefendantsGivenACaseId() throws Exception {

        final UUID caseId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);

        when(enveloper.withMetadataFrom(anyObject(), anyString())).thenReturn(function);
        when(function.apply(anyObject())).thenReturn(responseJson);

        final JsonEnvelope response  = queryView.getDefendants(query);

        verify(query, atMost(1)).payloadAsJsonObject();
        verify(objectMapper, atMost(2)).writeValueAsString(anyObject());
        verify(enveloper, atMost(1)).withMetadataFrom(anyObject(), anyString());
        verify(function,atMost(2)).apply(anyObject());

        assertThat(response, equalTo(responseJson));
    }

    @Test(expected = javax.persistence.NoResultException.class)
    public void shouldReturnNoResultForDefendantsGivenASpecificCaseId() {

        final UUID caseId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();
        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getDefendantsByCase(caseId)).thenThrow(new NoResultException());
        when(enveloper.withMetadataFrom(query,
                        ProgressionQueryView.CASE_PROGRESSION_DETAILS_RESPONSE))
                                        .thenReturn(function);
        when(function.apply(null)).thenReturn(responseJson);

        queryView.getDefendants(query);
    }

    @Test
    public void shouldGetADefendantGivenACaseId() throws Exception {
        final UUID caseId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        when(casePrgDetailService.getDefendantsByCase(caseId))
                .thenReturn(getDefendantsView());
        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(enveloper.withMetadataFrom(anyObject(), anyString())).thenReturn(function);
        when(function.apply(anyObject())).thenReturn(responseJson);

        final JsonEnvelope response  = queryView.getDefendants(query);

        verify(query, atMost(1)).payloadAsJsonObject();
        verify(objectMapper, atMost(2)).writeValueAsString(anyObject());
        verify(enveloper, atMost(1)).withMetadataFrom(anyObject(), anyString());
        verify(function,atMost(2)).apply(anyObject());
        verify(casePrgDetailService, atMost(1)).getDefendantsByCase(caseId);

        assertThat(response, equalTo(responseJson));
    }



}


