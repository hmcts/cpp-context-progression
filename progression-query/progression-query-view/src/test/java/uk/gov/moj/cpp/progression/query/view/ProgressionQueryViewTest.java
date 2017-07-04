package uk.gov.moj.cpp.progression.query.view;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.DefendantToDefendantViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionQueryViewTest {
    
    @Mock
    private JsonEnvelope query;
    
    @Mock
    private JsonEnvelope response;
    
    @Mock
    Enveloper enveloper;

    @Mock
    private ListToJsonArrayConverter<Object> listToJsonArrayConverter;

    @Mock
    private List<CaseProgressionDetail> listCaseProgressionDetail;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @Mock
    private CaseProgressionDetailView caseProgressionDetailView;

    @Mock
    private DefendantView defendantView;

    @Mock
    CaseProgressionDetailService casePrgDetailService;

    @Mock
    CaseProgressionDetailToViewConverter caseProgressionDetailToViewConverter;

    @Mock
    DefendantToDefendantViewConverter defendantToDefendantViewConverter;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private JsonEnvelope responseJson;

    @InjectMocks
    private ProgressionQueryView queryView;

    @Test
    public void shouldGetCaseProgressionDetailsGivenACaseId() {
        final UUID caseId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getCaseProgressionDetail(caseId))
                        .thenReturn(caseProgressionDetail);
        when(caseProgressionDetailToViewConverter.convert(caseProgressionDetail))
                        .thenReturn(caseProgressionDetailView);
        when(enveloper.withMetadataFrom(query,
                        ProgressionQueryView.CASE_PROGRESSION_DETAILS_RESPONSE))
                                        .thenReturn(function);
        when(function.apply(caseProgressionDetailView)).thenReturn(responseJson);
        assertThat(queryView.getCaseProgressionDetails(query), equalTo(responseJson));
    }

    @Test
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
    public void shouldGetMultipleCaseProgressionDetailsThatAreReadyForReview() {
        Optional<String> status = Optional.ofNullable("READY_FOR_REVIEW");
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_STATUS, status.get()).build();

        final JsonArray jsonArray =
                        Json.createArrayBuilder().add(Json.createObjectBuilder().build()).build();
        final JsonObject jsonObjectcases =
                        Json.createObjectBuilder().add("cases", jsonArray).build();

        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getCases(Optional.ofNullable("READY_FOR_REVIEW")))
                        .thenReturn(Arrays.asList(caseProgressionDetail));
        when(caseProgressionDetailToViewConverter.convert(caseProgressionDetail))
                        .thenReturn(caseProgressionDetailView);
        when(enveloper.withMetadataFrom(query, ProgressionQueryView.CASES_RESPONSE_LIST))
                        .thenReturn(function);

        when(listToJsonArrayConverter.convert(Arrays.asList(caseProgressionDetailView)))
                        .thenReturn(jsonArray);
        when(function.apply(jsonObjectcases)).thenReturn(responseJson);
        assertThat(queryView.getCases(query), equalTo(responseJson));
    }

    @Test
    public void shouldGetMultipleDefendantsGivenACaseId() {
        final UUID caseId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString()).build();

        final JsonArray jsonArray =
                        Json.createArrayBuilder().add(Json.createObjectBuilder().build()).build();
        final JsonObject jsonObjectDefendant =
                        Json.createObjectBuilder().add("defendants", jsonArray).build();

        final Defendant defendant = new Defendant();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getDefendantsByCase(caseId)).thenReturn(Arrays.asList(defendant));
        when(defendantToDefendantViewConverter.convert(defendant)).thenReturn(defendantView);
        when(enveloper.withMetadataFrom(query, ProgressionQueryView.DEFENDANT_RESPONSE_LIST))
                        .thenReturn(function);

        when(listToJsonArrayConverter.convert(Arrays.asList(defendantView))).thenReturn(jsonArray);
        when(function.apply(jsonObjectDefendant)).thenReturn(responseJson);
        assertThat(queryView.getDefendants(query), equalTo(responseJson));
    }

    @Test
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
        assertThat(queryView.getDefendants(query), equalTo(responseJson));
    }


    @Test
    public void shouldGetADefendantGivenACaseId() {
        final UUID defendantId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add(ProgressionQueryView.DEFENDANT_ID, defendantId.toString()).build();
        final Defendant defendant = new Defendant();

        when(query.payloadAsJsonObject()).thenReturn(jsonObject);
        when(casePrgDetailService.getDefendant(Optional.of(defendantId.toString())))
                        .thenReturn(Optional.of(defendant));
        when(defendantToDefendantViewConverter.convert(defendant)).thenReturn(defendantView);
        when(enveloper.withMetadataFrom(query, ProgressionQueryView.DEFENDANT_RESPONSE))
                        .thenReturn(function);

        when(function.apply(defendantView)).thenReturn(responseJson);
        assertThat(queryView.getDefendant(query), equalTo(responseJson));
    }
}
