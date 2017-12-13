package uk.gov.moj.cpp.progression.query.view.converter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;
import uk.gov.moj.cpp.progression.query.view.service.ProgressionDataConstant;

public class CaseProgressionDetailToViewConverter {

    public CaseProgressionDetailView convert(final CaseProgressionDetail caseProgressionDetail) {
        CaseProgressionDetailView caseProgressionDetailVo = null;
        caseProgressionDetailVo = new CaseProgressionDetailView();
        caseProgressionDetailVo.setCaseProgressionId(caseProgressionDetail.getId().toString());
        caseProgressionDetailVo.setCaseId(caseProgressionDetail.getCaseId().toString());
        caseProgressionDetailVo.setCaseUrn(caseProgressionDetail.getCaseUrn());
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
        caseProgressionDetailVo.setCourtCentreId(caseProgressionDetail.getCourtCentreId());
        if (caseProgressionDetail.getSentenceHearingId() != null) {
            caseProgressionDetailVo.setSentenceHearingId(caseProgressionDetail.getSentenceHearingId().toString());
        }

        DefendantToDefendantViewConverter defendantToDefendantViewConverter=new DefendantToDefendantViewConverter();
        List<DefendantView> defendantViews=caseProgressionDetail.getDefendants().stream().map(d-> defendantToDefendantViewConverter.convert(d)).collect(Collectors.toList());
        caseProgressionDetailVo.setDefendants(defendantViews);
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
