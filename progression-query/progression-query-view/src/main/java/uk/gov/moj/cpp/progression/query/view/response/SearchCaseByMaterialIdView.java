package uk.gov.moj.cpp.progression.query.view.response;

/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class SearchCaseByMaterialIdView {

    private final String caseId;
    private final ProsecutingAuthority prosecutingAuthority;

    public SearchCaseByMaterialIdView(final String caseId, final ProsecutingAuthority prosecutingAuthority) {
        this.caseId = caseId;
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public SearchCaseByMaterialIdView() {
        this(null, null);
    }

    public String getCaseId() {
        return caseId;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }
}
