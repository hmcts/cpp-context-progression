class ReserveTermsType {
    static get LUMP_SUM() {
        return "LUMP_SUM";
    }

    static get LUMP_SUM_PLUS_INSTALMENTS() {
        return "LUMP_SUM_PLUS_INSTALMENTS"
    }

    static get INSTALMENTS_ONLY() {
        return "INSTALMENTS_ONLY"
    }
}

module.exports = ReserveTermsType;