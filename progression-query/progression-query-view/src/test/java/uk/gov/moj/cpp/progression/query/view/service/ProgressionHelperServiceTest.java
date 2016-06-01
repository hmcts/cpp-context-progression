package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionHelperServiceTest {

	@Spy
	StringToJsonObjectConverter stringToJsonObjectConverter;

	@Spy
	ObjectMapper objectMapper;

	@InjectMocks
	private ProgressionHelperService helperService;

	@Test
	public void shouldCovertArraysToJsonArray() {
		CaseProgressionDetailView one = new CaseProgressionDetailView();
		one.setCaseId("Case1");
		CaseProgressionDetailView two = new CaseProgressionDetailView();
		two.setCaseId("Case2");

		List<CaseProgressionDetailView> listOffence = Arrays.asList(one, two);

		assertThat(helperService.arraysToJsonArray(listOffence).size(), equalTo(2));
		assertThat(helperService.arraysToJsonArray(listOffence).getJsonObject(0).getString("caseId"),
				equalTo(one.getCaseId()));
	}
}
