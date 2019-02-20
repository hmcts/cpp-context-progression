package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;

public class FinancialOrderDetails implements Serializable {
    private static final long serialVersionUID = 1870765747443534132L;

    private final String paymentTerms;

    private final String accountPaymentReference;

    private final String accountingDivisionCode;

    private final String bacsAccountNumber;

    private final String bacsBankName;

    private final String bacsSortCode;

    private final Address enforcementAddress;

    private final String enforcementEmail;

    private final String enforcementPhoneNumber;

    private final String totalAmountImposed;

    private final String totalBalance;

    private final Boolean isCrownCourt;

    @SuppressWarnings({"squid:S00107"})
    public FinancialOrderDetails(final String accountPaymentReference, final String accountingDivisionCode, final String bacsAccountNumber, final String bacsBankName, final String bacsSortCode, final Address enforcementAddress, final String enforcementEmail, final String enforcementPhoneNumber, final String totalAmountImposed,
                                 final String totalBalance, final boolean isCrownCourt, final String paymentTerms) {
        this.accountPaymentReference = accountPaymentReference;
        this.accountingDivisionCode = accountingDivisionCode;
        this.bacsAccountNumber = bacsAccountNumber;
        this.bacsBankName = bacsBankName;
        this.bacsSortCode = bacsSortCode;
        this.enforcementAddress = enforcementAddress;
        this.enforcementEmail = enforcementEmail;
        this.enforcementPhoneNumber = enforcementPhoneNumber;
        this.totalAmountImposed = totalAmountImposed;
        this.totalBalance = totalBalance;
        this.isCrownCourt = isCrownCourt;
        this.paymentTerms = paymentTerms;
    }

    public String getAccountPaymentReference() {
        return accountPaymentReference;
    }

    public String getAccountingDivisionCode() {
        return accountingDivisionCode;
    }

    public String getBacsAccountNumber() {
        return bacsAccountNumber;
    }

    public String getBacsBankName() {
        return bacsBankName;
    }

    public String getBacsSortCode() {
        return bacsSortCode;
    }

    public Address getEnforcementAddress() {
        return enforcementAddress;
    }

    public String getEnforcementEmail() {
        return enforcementEmail;
    }

    public String getEnforcementPhoneNumber() {
        return enforcementPhoneNumber;
    }

    public String getTotalAmountImposed() {
        return totalAmountImposed;
    }

    public String getTotalBalance() {
        return totalBalance;
    }

    public Boolean getIsCrownCourt() {
        return isCrownCourt;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public static Builder financialOrderDetails() {
        return new FinancialOrderDetails.Builder();
    }

    public static class Builder {

        private String paymentTerms;

        private String accountPaymentReference;

        private String accountingDivisionCode;

        private String bacsAccountNumber;

        private String bacsBankName;

        private String bacsSortCode;

        private Address enforcementAddress;

        private String enforcementEmail;

        private String enforcementPhoneNumber;

        private String totalAmountImposed;

        private String totalBalance;

        private Boolean isCrownCourt;

        public Builder withAccountPaymentReference(final String accountPaymentReference) {
            this.accountPaymentReference = accountPaymentReference;
            return this;
        }

        public Builder withAccountingDivisionCode(final String accountingDivisionCode) {
            this.accountingDivisionCode = accountingDivisionCode;
            return this;
        }

        public Builder withBacsAccountNumber(final String bacsAccountNumber) {
            this.bacsAccountNumber = bacsAccountNumber;
            return this;
        }

        public Builder withBacsBankName(final String bacsBankName) {
            this.bacsBankName = bacsBankName;
            return this;
        }

        public Builder withBacsSortCode(final String bacsSortCode) {
            this.bacsSortCode = bacsSortCode;
            return this;
        }

        public Builder withEnforcementAddress(final Address enforcementAddress) {
            this.enforcementAddress = enforcementAddress;
            return this;
        }

        public Builder withEnforcementEmail(final String enforcementEmail) {
            this.enforcementEmail = enforcementEmail;
            return this;
        }

        public Builder withEnforcementPhoneNumber(final String enforcementPhoneNumber) {
            this.enforcementPhoneNumber = enforcementPhoneNumber;
            return this;
        }

        public Builder withTotalAmountImposed(final String totalAmountImposed) {
            this.totalAmountImposed = totalAmountImposed;
            return this;
        }

        public Builder withTotalBalance(final String totalBalance) {
            this.totalBalance = totalBalance;
            return this;
        }

        public Builder withIsCrownCourt(final boolean isCrownCourt) {
            this.isCrownCourt = isCrownCourt;
            return this;
        }

        public Builder withPaymentTerms(final String paymentTerms) {
            this.paymentTerms = paymentTerms;
            return this;
        }

        public FinancialOrderDetails build() {
            return new FinancialOrderDetails(accountPaymentReference, accountingDivisionCode, bacsAccountNumber, bacsBankName, bacsSortCode, enforcementAddress,
                    enforcementEmail, enforcementPhoneNumber, totalAmountImposed, totalBalance, isCrownCourt, paymentTerms);
        }
    }
}
