package uk.gov.moj.cpp.progression.processor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class NowsNotificationDocumentState implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID originatingCourtCentreId;
    private List<String> usergroups;
    private UUID nowsTypeId;
    private List<String> caseUrns;
    private String courtClerkName;
    private String defendantName;
    private String jurisdiction;
    private String courtCentreName;
    private String orderName;
    private String priority;
    private UUID materialId;
    private boolean isRemotePrintingRequired;

    public UUID getOriginatingCourtCentreId() {
        return originatingCourtCentreId;
    }

    public NowsNotificationDocumentState setOriginatingCourtCentreId(UUID originatingCourtCentreId) {
        this.originatingCourtCentreId = originatingCourtCentreId;
        return this;
    }

    public List<String> getUsergroups() {
        return usergroups;
    }

    public NowsNotificationDocumentState setUsergroups(List<String> usergroups) {
        this.usergroups = usergroups;
        return this;
    }

    public UUID getNowsTypeId() {
        return nowsTypeId;
    }

    public NowsNotificationDocumentState setNowsTypeId(UUID nowsTypeId) {
        this.nowsTypeId = nowsTypeId;
        return this;
    }

    public List<String> getCaseUrns() {
        return caseUrns;
    }

    public NowsNotificationDocumentState setCaseUrns(List<String> caseUrns) {
        this.caseUrns = caseUrns;
        return this;
    }

    public String getCourtClerkName() {
        return courtClerkName;
    }

    public NowsNotificationDocumentState setCourtClerkName(String courtClerkName) {
        this.courtClerkName = courtClerkName;
        return this;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public NowsNotificationDocumentState setDefendantName(String defendantName) {
        this.defendantName = defendantName;
        return this;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public NowsNotificationDocumentState setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
        return this;
    }

    public NowsNotificationDocumentState setCourtCentreName(String courtCentreName) {
        this.courtCentreName = courtCentreName;
        return this;
    }

    public NowsNotificationDocumentState setIsRemotePrintingRequired(final boolean isRemotePrintingRequired) {
        this.isRemotePrintingRequired = isRemotePrintingRequired;
        return this;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public NowsNotificationDocumentState setOrderName(String orderName) {
        this.orderName = orderName;
        return this;
    }

    public String getOrderName() {
        return orderName;
    }

    public NowsNotificationDocumentState setPriority(String priority) {
        this.priority = priority;
        return this;
    }

    public String getPriority() {
        return priority;
    }

    public NowsNotificationDocumentState setMaterialId(UUID materialId) {
        this.materialId = materialId;
        return this;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public boolean getIsRemotePrintingRequired() {
        return isRemotePrintingRequired;
    }
}
