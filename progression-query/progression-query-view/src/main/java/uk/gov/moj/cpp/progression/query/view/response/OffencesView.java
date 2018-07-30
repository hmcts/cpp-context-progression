package uk.gov.moj.cpp.progression.query.view.response;

import java.util.Comparator;
import java.util.List;

public class OffencesView {

    private List<OffenceView> offences;

    public OffencesView(List<OffenceView> offences) {
        offences.sort(Comparator.comparing(OffenceView::getOrderIndex));
        this.offences = offences;
    }

    public List<OffenceView> getOffences() {
        return offences;
    }

    public void setOffences(List<OffenceView> offences) {
        this.offences = offences;
    }

}
