package uk.gov.moj.cpp.progression.query.view.response;

import java.util.Comparator;
import java.util.List;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class DefendantsView {
    private List<DefendantView> defendants;

    public DefendantsView(final List<DefendantView> defendants) {
        defendants.sort(Comparator.comparing(DefendantView::getDefendantId));
        this.defendants = defendants;
    }

    public List<DefendantView> getDefendants() {
        return defendants;
    }

    public void setDefendants(final List<DefendantView> defendants) {
        this.defendants = defendants;
    }
}
