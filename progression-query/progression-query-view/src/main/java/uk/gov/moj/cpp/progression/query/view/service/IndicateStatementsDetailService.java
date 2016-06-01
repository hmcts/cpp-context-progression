package uk.gov.moj.cpp.progression.query.view.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.progression.persistence.repository.IndicateStatementRepository;

public class IndicateStatementsDetailService {

	@Inject
	private IndicateStatementRepository indicateStatementDetailRepo;

	@Transactional
	public List<IndicateStatement> getIndicateStatements(UUID caseId) {
		return indicateStatementDetailRepo.findByCaseId(caseId);
	}

	@Transactional
	public Optional<IndicateStatement> getIndicateStatementById(UUID id) {
		IndicateStatement indicateStatement = indicateStatementDetailRepo.findById(id);
		if (indicateStatement == null) {
			return Optional.empty();
		}
		return Optional.of(indicateStatement);
	}

}
