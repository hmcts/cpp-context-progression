package uk.gov.moj.cpp.progression.query.view.converter;

import java.time.LocalDate;

import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.service.ProgressionDataConstant;

public class CaseProgressionDetailToViewConverter {

    public CaseProgressionDetailView convert(final CaseProgressionDetail caseProgressionDetail) {
        CaseProgressionDetailView caseProgressionDetailVo = null;
        caseProgressionDetailVo = new CaseProgressionDetailView();
        caseProgressionDetailVo.setCaseProgressionId(caseProgressionDetail.getId().toString());
        caseProgressionDetailVo.setCaseId(caseProgressionDetail.getCaseId().toString());
        if (caseProgressionDetail.getStatus() != null) {
            caseProgressionDetailVo.setStatus(caseProgressionDetail.getStatus().toString());
        }
        caseProgressionDetailVo.setSentenceReviewDeadlineDate(
                        calcSentenceReviewDeadlineDate(caseProgressionDetail));
        caseProgressionDetailVo.setDirectionIssuedOn(caseProgressionDetail.getDirectionIssuedOn());
        caseProgressionDetailVo.setFromCourtCentre(caseProgressionDetail.getFromCourtCentre());
        caseProgressionDetailVo
                        .setSendingCommittalDate(caseProgressionDetail.getSendingCommittalDate());
        caseProgressionDetailVo
                        .setSentenceHearingDate(caseProgressionDetail.getSentenceHearingDate());

        return caseProgressionDetailVo;
    }

    private LocalDate calcSentenceReviewDeadlineDate(
                    final CaseProgressionDetail caseProgressionDetail) {
        LocalDate basedOnDate = null;
        if (caseProgressionDetail.getSendingCommittalDate() != null) {
            basedOnDate = caseProgressionDetail.getSendingCommittalDate();
        }
        return LocalDateUtils.addWorkingDays(basedOnDate,
                        ProgressionDataConstant.sentenceReviewDeadlineDateDaysFromDateOfSending);
    }
}
