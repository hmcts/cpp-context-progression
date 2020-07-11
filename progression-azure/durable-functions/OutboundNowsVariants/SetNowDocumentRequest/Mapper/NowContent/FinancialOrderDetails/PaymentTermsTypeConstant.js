class PaymentTermsTypeConstant {

    static get OPTIONAL_TEXT() {
        return " Number of days in default";
    }

    static get PAY_BY_DATE() {
        return "Date to pay in full by";
    }

    static get LUMP_SUM_AMOUNT() {
        return "Lump sum amount";
    }

    static get INSTALMENTS_AMOUNT() {
        return "Instalment amount";
    }

    static get PAYMENT_FREQUENCY() {
        return "Payment frequency";
    }

    static get INSTALMENT_START_DATE() {
        return "Instalment start date";
    }

    static get DEFAULT_PAYMENT_TERMS() {
        return "Instalments only with instalment amount 20, monthly, instalment start date";
    }
}

module.exports = PaymentTermsTypeConstant;