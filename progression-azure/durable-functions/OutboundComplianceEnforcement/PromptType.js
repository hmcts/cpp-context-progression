class PromptType {
    static get AMOUNT_OF_FINE() {
        return "AOF"
    }

    static get AMOUNT_OF_COSTS() {
        return "AOC"
    }

    static get AMOUNT_OF_SURCHARGE() {
        return "AOS"
    }

    static get AMOUNT_OF_BACK_DUTY() {
        return "AOBD"
    }

    static get CREDITOR_NAME() {
        return "CREDNAME";
    }

    static get COSTS_TO_CROWN_PROSECUTION_SERVICE_AMOUNT_PROMPT_REFERENCE() {
        return "FCPC";
    }

    static get AMOUNT_OF_COMPENSATION() {
        return "AOCOM"
    }

    static get TOTAL_AMOUNT_ENFORCED_PROMPT_REFERENCE() {
        return "TOTENF";
    }

    static get PAYMENT_FREQUENCY() {
        return "PF";
    }

    static get PAY_BY_DATE() {
        return "PBD";
    }

    static get LUMP_SUM_AMOUNT() {
        return "LSA";
    }

    static get DEFAULT_DAYS_IN_JAIL_PROMPT_REFERENCE() {
        return "DID";
    }

    static get INSTALMENT_AMOUNT() {
        return "IAMT";
    }

    static get INSTALMENT_START_DATE(){
     return "instalmentStartDate";
    }

    static get PARENT_GUARDIAN_TOPAY_PROMPT_REFERENCE(){
        return "PARENT_GAURDIAN_TO_PAY";
    }

    static get PAYMENT_CARD_REQUIRED_PROMPT_REFERENCE() {
        return "PAYMENT_CARD_REQUIRED";
    }

    // Fixed List for Payment Frequency
    static get WEEKLY() {
        return "weekly";
    }

    static get FORTNIGHTLY() {
        return "fortnightly";
    }

    static get MONTHLY() {
        return "monthly";
    }

    //Employer related
    static get EMPLOYER_ORGANISATION_NAME_PROMPT_REFERENCE(){
        return "employerName";
    }

    static get EMPLOYER_ORGANISATION_ADDRESS1_PROMPT_REFERENCE(){
        return "employerAddress1";
    }

    static get EMPLOYER_ORGANISATION_ADDRESS2_PROMPT_REFERENCE(){
        return "employerAddress2";
    }

    static get EMPLOYER_ORGANISATION_ADDRESS3_PROMPT_REFERENCE(){
        return "employerAddress3";
    }

    static get EMPLOYER_ORGANISATION_ADDRESS4_PROMPT_REFERENCE(){
        return "employerAddress4";
    }

    static get EMPLOYER_ORGANISATION_ADDRESS5_PROMPT_REFERENCE(){
        return "employerAddress5";
    }

    static get EMPLOYER_ORGANISATION_POST_CODE_PROMPT_REFERENCE(){
        return "employerPostCode";
    }

    static get EMPLOYER_ORGANISATION_REFERENCE_NUMBER_PROMPT_REFERENCE(){
        return "employerReferenceNumber";
    }
}

module.exports = PromptType;