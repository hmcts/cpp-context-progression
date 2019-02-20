package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S1948")
public class NowsDocumentOrder implements Serializable {
    private static final long serialVersionUID = 1870765747443534132L;

    private final List<String> caseUrns;

    private final List<Cases> cases;

    private final String courtCentreName;

    private final String courtClerkName;

    private final Defendant defendant;

    private final FinancialOrderDetails financialOrderDetails;

    private final Boolean isAmended;

    private final String ljaCode;

    private final String ljaName;

    private final UUID materialId;

    private final NowResultDefinitionsText nowResultDefinitionsText;

    private final String nowText;

    private final OrderAddressee orderAddressee;

    private final String orderDate;

    private final String orderName;

    private final String priority;

    private final String subTemplateName;

    private final NextHearingCourtDetails nextHearingCourtDetails;

    private final Boolean isCrownCourt;

    @SuppressWarnings({"squid:S00107"})
    public NowsDocumentOrder(final List<String> caseUrns, final List<Cases> cases, final String courtCentreName, final String courtClerkName,
                             final Defendant defendant, final FinancialOrderDetails financialOrderDetails, final Boolean isAmended,
                             final String ljaCode, final String ljaName, final UUID materialId,
                             final NowResultDefinitionsText nowResultDefinitionsText, final String nowText,
                             final OrderAddressee orderAddressee, final String orderDate, final String orderName, final String priority,
                             final String subTemplateName, final NextHearingCourtDetails nextHearingCourtDetails, final Boolean isCrownCourt) {
        this.caseUrns = caseUrns;
        this.cases = cases;
        this.courtCentreName = courtCentreName;
        this.courtClerkName = courtClerkName;
        this.defendant = defendant;
        this.financialOrderDetails = financialOrderDetails;
        this.isAmended = isAmended;
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.materialId = materialId;
        this.nowResultDefinitionsText = nowResultDefinitionsText;
        this.nowText = nowText;
        this.orderAddressee = orderAddressee;
        this.orderDate = orderDate;
        this.orderName = orderName;
        this.priority = priority;
        this.subTemplateName = subTemplateName;
        this.nextHearingCourtDetails = nextHearingCourtDetails;
        this.isCrownCourt = isCrownCourt;
    }

    public List<String> getCaseUrns() {
        return caseUrns;
    }

    public List<Cases> getCases() {
        return cases;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getCourtClerkName() {
        return courtClerkName;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public FinancialOrderDetails getFinancialOrderDetails() {
        return financialOrderDetails;
    }

    public Boolean getIsAmended() {
        return isAmended;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public NowResultDefinitionsText getNowResultDefinitionsText() {
        return nowResultDefinitionsText;
    }

    public String getNowText() {
        return nowText;
    }

    public OrderAddressee getOrderAddressee() {
        return orderAddressee;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getOrderName() {
        return orderName;
    }

    public String getPriority() {
        return priority;
    }

    public String getSubTemplateName() {
        return subTemplateName;
    }

    public Boolean getIsCrownCourt() {
        return isCrownCourt;
    }

    public NextHearingCourtDetails getNextHearingCourtDetails() {
        return this.nextHearingCourtDetails;
    }

    public static Builder nowsDocumentOrder() {
        return new NowsDocumentOrder.Builder();
    }

    public static class Builder {
        private List<String> caseUrns;

        private List<Cases> cases;

        private String courtCentreName;

        private String courtClerkName;

        private Defendant defendant;

        private FinancialOrderDetails financialOrderDetails;

        private Boolean isAmended;

        private String ljaCode;

        private String ljaName;

        private UUID materialId;

        private NowResultDefinitionsText nowResultDefinitionsText;

        private String nowText;

        private OrderAddressee orderAddressee;

        private String orderDate;

        private String orderName;

        private String priority;

        private String subTemplateName;

        private NextHearingCourtDetails nextHearingCourtDetails;

        private Boolean isCrownCourt;

        public Builder withCaseUrns(final List<String> caseUrns) {
            this.caseUrns = caseUrns;
            return this;
        }

        public Builder withCases(final List<Cases> cases) {
            this.cases = cases;
            return this;
        }

        public Builder withCourtCentreName(final String courtCentreName) {
            this.courtCentreName = courtCentreName;
            return this;
        }

        public Builder withCourtClerkName(final String courtClerkName) {
            this.courtClerkName = courtClerkName;
            return this;
        }

        public Builder withDefendant(final Defendant defendant) {
            this.defendant = defendant;
            return this;
        }

        public Builder withFinancialOrderDetails(final FinancialOrderDetails financialOrderDetails) {
            this.financialOrderDetails = financialOrderDetails;
            return this;
        }

        public Builder withIsAmended(final Boolean isAmended) {
            this.isAmended = isAmended;
            return this;
        }

        public Builder withLjaCode(final String ljaCode) {
            this.ljaCode = ljaCode;
            return this;
        }

        public Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public Builder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public Builder withNowResultDefinitionsText(final NowResultDefinitionsText nowResultDefinitionsText) {
            this.nowResultDefinitionsText = nowResultDefinitionsText;
            return this;
        }

        public Builder withNowText(final String nowText) {
            this.nowText = nowText;
            return this;
        }

        public Builder withOrderAddressee(final OrderAddressee orderAddressee) {
            this.orderAddressee = orderAddressee;
            return this;
        }

        public Builder withOrderDate(final String orderDate) {
            this.orderDate = orderDate;
            return this;
        }

        public Builder withOrderName(final String orderName) {
            this.orderName = orderName;
            return this;
        }

        public Builder withPriority(final String priority) {
            this.priority = priority;
            return this;
        }

        public Builder withSubTemplateName(final String subTemplateName) {
            this.subTemplateName = subTemplateName;
            return this;
        }

        public Builder withNextHearingCourtDetails(final NextHearingCourtDetails nextHearingCourtDetails) {
            this.nextHearingCourtDetails = nextHearingCourtDetails;
            return this;
        }

        public Builder withIsCrownCourt(final Boolean isCrownCourt) {
            this.isCrownCourt = isCrownCourt;
            return this;
        }

        public NowsDocumentOrder build() {
            return new NowsDocumentOrder(caseUrns, cases, courtCentreName, courtClerkName, defendant, financialOrderDetails, isAmended, ljaCode, ljaName, materialId,
                    nowResultDefinitionsText, nowText, orderAddressee, orderDate, orderName, priority, subTemplateName, nextHearingCourtDetails, isCrownCourt);
        }
    }
}
