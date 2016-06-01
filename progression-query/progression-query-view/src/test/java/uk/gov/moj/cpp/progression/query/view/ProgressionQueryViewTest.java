package uk.gov.moj.cpp.progression.query.view;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.IndicateStatementsDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.TimelineDateToTimeLineDateViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.IndicateStatementsDetailView;
import uk.gov.moj.cpp.progression.query.view.response.TimeLineDateView;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;
import uk.gov.moj.cpp.progression.query.view.service.IndicateStatementsDetailService;
import uk.gov.moj.cpp.progression.query.view.service.ProgressionHelperService;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionQueryViewTest {
	@Mock
	private JsonEnvelope query;
	@Mock
	private JsonEnvelope response;
	@Mock
	Enveloper enveloper;

	@Mock
	private ProgressionHelperService helperService;

	@Mock
	private List<CaseProgressionDetail> listCaseProgressionDetail;

	@Mock
	private CaseProgressionDetail caseProgressionDetail;

	@Mock
	private CaseProgressionDetailView caseProgressionDetailView;
	
	@Mock 
	TimeLineDate timeLineDate;
	
	@Mock 
	TimeLineDateView timeLineDateView;

	@Mock
	CaseProgressionDetailService casePrgDetailService;

	@Mock
	CaseProgressionDetailToViewConverter caseProgressionDetailToViewConverter;
	
	@Mock
	TimelineDateToTimeLineDateViewConverter timelineDateToTimeLineDateVOConverter;
	
	@Mock
	private IndicateStatement indicateStatement;

	@Mock
	private IndicateStatementsDetailView indicateStatementView;
	
	@Mock
	IndicateStatementsDetailService indicateStmtDetailService;
	
	@Mock
	IndicateStatementsDetailToViewConverter indicateStatementsDetailToViewConverter;
	
	@Mock
	private Function<Object, JsonEnvelope> function;

	@Mock
	private JsonEnvelope responceJson;

	@InjectMocks
	private ProgressionQueryView queryView;

	@Test
	public void shouldHandleProgressionQuery() {
		UUID caseId = UUID.randomUUID();
		String now = LocalDate.now().toString();
		JsonObject jsonObject = Json.createObjectBuilder().add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString())
				.build();

		when(query.payloadAsJsonObject()).thenReturn(jsonObject);
		when(casePrgDetailService.getCaseProgressionDetail(caseId)).thenReturn(Optional.of(caseProgressionDetail));
		when(caseProgressionDetailToViewConverter.convert(caseProgressionDetail)).thenReturn(caseProgressionDetailView);
		when(enveloper.withMetadataFrom(query, ProgressionQueryView.CASE_PROGRESSION_DETAILS_RESPONSE)).thenReturn(function);

		when(function.apply(caseProgressionDetailView)).thenReturn(responceJson);
		assertThat(queryView.getCaseProgressionDetails(query), equalTo(responceJson));
	}

	@Test
	public void shouldHandleIndicateStmtQuery() {
		UUID statementId = UUID.randomUUID();
		String now = LocalDate.now().toString();
		JsonObject jsonObject = Json.createObjectBuilder().add(ProgressionQueryView.FIELD_INDICATE_STATEMENT_ID, statementId.toString())
				.build();

		when(query.payloadAsJsonObject()).thenReturn(jsonObject);
		when(indicateStmtDetailService.getIndicateStatementById(statementId)).thenReturn(Optional.of(indicateStatement));
		when(indicateStatementsDetailToViewConverter.convert(indicateStatement)).thenReturn(indicateStatementView);
		when(enveloper.withMetadataFrom(query, ProgressionQueryView.INDICATE_STATEMENT_RESPONSE)).thenReturn(function);

		when(function.apply(indicateStatementView)).thenReturn(responceJson);
		assertThat(queryView.getIndicatestatementsdetail(query), equalTo(responceJson));
	}
	
	@Test
	public void shouldHandleProgressionSQuery() {
		UUID caseId = UUID.randomUUID();
		String now = LocalDate.now().toString();
		JsonObject jsonObject = Json.createObjectBuilder().add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString())
				.build();
		CaseProgressionDetail caseProgressionDetail=new CaseProgressionDetail();
		TimeLineDate timeLineDate=new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline, LocalDate.now(), LocalDate.now(), 2);
		caseProgressionDetail.setTimeLine(Arrays.asList(timeLineDate));
		
		JsonArray jsonArray=Json.createArrayBuilder().add(Json.createObjectBuilder().build()).build();
		JsonObject jsonObjectTimeline= Json.createObjectBuilder().add("timeline",jsonArray).build();

		when(query.payloadAsJsonObject()).thenReturn(jsonObject);
		when(casePrgDetailService.getCaseProgressionDetail(caseId)).thenReturn(Optional.of(caseProgressionDetail));
		when(timelineDateToTimeLineDateVOConverter.convert(timeLineDate)).thenReturn(timeLineDateView);
		when(enveloper.withMetadataFrom(query, ProgressionQueryView.TIMELINE_RESPONSE)).thenReturn(function);
		when(helperService.arraysToJsonArray(Arrays.asList(timeLineDateView))).thenReturn(jsonArray);
		when(function.apply(jsonObjectTimeline)).thenReturn(responceJson);
		assertThat(queryView.getTimeLineForProgression(query), equalTo(responceJson));
	}

	@Test
	public void shouldHandleIndicateStmtSQuery() {
		UUID caseId = UUID.randomUUID();
		String now = LocalDate.now().toString();
		JsonObject jsonObject = Json.createObjectBuilder().add(ProgressionQueryView.FIELD_CASE_ID, caseId.toString())
				.build();
		
		JsonArray jsonArray=Json.createArrayBuilder().add(Json.createObjectBuilder().build()).build();
		JsonObject jsonObjectindicatestatements= Json.createObjectBuilder().add("indicatestatements",jsonArray).build();

		when(query.payloadAsJsonObject()).thenReturn(jsonObject);
		when(indicateStmtDetailService.getIndicateStatements(caseId)).thenReturn(Arrays.asList(indicateStatement));
		when(indicateStatementsDetailToViewConverter.convert(indicateStatement)).thenReturn(indicateStatementView);
		when(enveloper.withMetadataFrom(query, ProgressionQueryView.INDICATE_STATEMENT_RESPONSE_LIST)).thenReturn(function);
		when(helperService.arraysToJsonArray(Arrays.asList(indicateStatementView))).thenReturn(jsonArray);
		when(function.apply(jsonObjectindicatestatements)).thenReturn(responceJson);
		assertThat(queryView.getIndicatestatementsdetails(query), equalTo(responceJson));
	}
}
