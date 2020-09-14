class CollectionOrder {

    constructor() {
        this.isApplicationForBenefitsDeduction = false;
        this.isAttachmentOfEarnings = false;
        this.isCollectionOrderMade = false;
        this.isPaymentTermsOnRelease = false;
        this.reserveTerms = undefined;
    }
}

class ReserveTerms {

    constructor() {
        this.reserveTermsType = undefined;
        this.lumpSum = undefined;
        this.installments = undefined;
    }
}

class LumpSum {

    constructor() {
        this.amount = undefined;
        this.withinDays = undefined;
        this.amountImposed = undefined;
    }
}

class Installments {

    constructor() {
        this.amount = undefined;
        this.frequency = undefined;
        this.startDate = undefined;
    }
}

module.exports = {CollectionOrder, ReserveTerms, Installments, LumpSum};