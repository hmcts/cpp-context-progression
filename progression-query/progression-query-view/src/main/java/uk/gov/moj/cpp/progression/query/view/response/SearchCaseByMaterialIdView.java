package uk.gov.moj.cpp.progression.query.view.response;


public class SearchCaseByMaterialIdView {

    private String caseId;
    private ProsecutingAuthority prosecutingAuthority;

    public SearchCaseByMaterialIdView(String caseId, ProsecutingAuthority prosecutingAuthority) {
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
