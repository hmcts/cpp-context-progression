class PaymentTermsType {

    static get WEEKLY_INSTALMENTS() {
        return "WEEKLY_INSTALMENTS";
    }

    static get WEEKLY_INSTALMENTS_THEN_LUMP_SUM() {
        return "WEEKLY_INSTALMENTS_THEN_LUMP_SUM"
    }

    static get BY_DATE() {
        return "BY_DATE";
    }

    static get MONTHLY_INSTALMENTS() {
        return "MONTHLY_INSTALMENTS";
    }

    static get FORTNIGHTLY_INSTALMENTS_THEN_LUMP_SUM() {
        return "FORTNIGHTLY_INSTALMENTS_THEN_LUMP_SUM";
    }

    static get PAID() {
        return "PAID";
    }

    static get FORTNIGHTLY_INSTALMENTS() {
        return "FORTNIGHTLY_INSTALMENTS";
    }

    static get MONTHLY_INSTALMENTS_THEN_LUMP_SUM() {
        return "MONTHLY_INSTALMENTS_THEN_LUMP_SUM";
    }
}

module.exports = PaymentTermsType;