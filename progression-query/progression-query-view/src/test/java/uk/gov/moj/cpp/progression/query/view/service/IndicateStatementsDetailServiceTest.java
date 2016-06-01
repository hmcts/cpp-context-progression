package uk.gov.moj.cpp.progression.query.view.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.cpp.progression.query.view.converter.IndicateStatementsDetailToViewConverter;
import uk.gov.moj.progression.persistence.repository.IndicateStatementRepository;

@RunWith(MockitoJUnitRunner.class)
public class IndicateStatementsDetailServiceTest {

	private static final UUID ID = UUID.randomUUID();

	private static final UUID CASEID = UUID.randomUUID();

	private static final String EVIDENCENAME = "evidence name";

	@Mock
	private IndicateStatementRepository indicateStatementRepository;

	@Spy
	private IndicateStatementsDetailToViewConverter indicateStatementsDetailToVOConverter = new IndicateStatementsDetailToViewConverter();

	@InjectMocks
	private IndicateStatementsDetailService indicateStatementsDetailService;

	@Test
	public void getIndicateStatementsDetailTestEmpty() {
		final List<IndicateStatement> listIndicateStatementsDetail = new ArrayList<IndicateStatement>();
		when(this.indicateStatementRepository.findByCaseId(CASEID))
				.thenReturn(listIndicateStatementsDetail);
		assertTrue(this.indicateStatementsDetailService.getIndicateStatements(CASEID).isEmpty());
	}

	@Test
	public void getIndicateStatementsDetailTest() throws IOException {
		final List<IndicateStatement> listCaseProgressionDetail = new ArrayList<IndicateStatement>();
		IndicateStatement indicateStatement = new IndicateStatement();
		indicateStatement.setId(ID);
		indicateStatement.setPlanDate(LocalDate.now());
		indicateStatement.setCaseId(CASEID);
		indicateStatement.setIsKeyEvidence(true);
		indicateStatement.setEvidenceName(EVIDENCENAME);

		listCaseProgressionDetail.add(indicateStatement);
		when(this.indicateStatementRepository.findByCaseId(CASEID))
				.thenReturn(listCaseProgressionDetail);
		assertTrue(
				this.indicateStatementsDetailService.getIndicateStatements(CASEID).get(0).getCaseId().equals(CASEID));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatements(CASEID).get(0).getId().equals(ID));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatements(CASEID).get(0).getPlanDate()
				.equals(LocalDate.now()));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatements(CASEID).get(0).getEvidenceName()
				.equals(EVIDENCENAME));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatements(CASEID).get(0).getIsKeyEvidence()
				.equals(true));

	}

	@Test
	public void getIndicateStatementByIdTest() throws IOException {
		final List<IndicateStatement> listCaseProgressionDetail = new ArrayList<IndicateStatement>();
		IndicateStatement indicateStatement = new IndicateStatement();
		indicateStatement.setId(ID);
		indicateStatement.setPlanDate(LocalDate.now());
		indicateStatement.setCaseId(CASEID);
		indicateStatement.setIsKeyEvidence(true);
		indicateStatement.setEvidenceName(EVIDENCENAME);

		listCaseProgressionDetail.add(indicateStatement);
		when(this.indicateStatementRepository.findById(ID)).thenReturn(indicateStatement);
		assertTrue(this.indicateStatementsDetailService.getIndicateStatementById(ID).get().getCaseId().equals(CASEID));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatementById(ID).get().getId().equals(ID));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatementById(ID).get().getPlanDate()
				.equals(LocalDate.now()));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatementById(ID).get().getEvidenceName()
				.equals(EVIDENCENAME));
		assertTrue(this.indicateStatementsDetailService.getIndicateStatementById(ID).get().getIsKeyEvidence()
				.equals(true));

	}

	@Test
	public void getIndicateStatementByIDTestEmpty() {
		when(this.indicateStatementRepository.findById(ID)).thenReturn(null);
		assertFalse(this.indicateStatementsDetailService.getIndicateStatementById(ID).isPresent());
	}
}
