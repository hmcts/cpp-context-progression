const CollectionOrder = require('./CollectionOrder');
const ParentGuardian = require('./ParentGuardian');
const PaymentTerms = require('./PaymentTerms');
const MinorCreditor = require('./MinorCreditor');
const Employer = require('./Employer');
const Plea = require('./Plea');
const Defendant = require('./Defendant');

class StagingEnforcementRequest {

    constructor() {
        this.requestId;
        this.prosecutionAuthorityCode;
        this.prosecutionCaseReference;
        this.originator;
        this.imposingCourt;
        this.plea;
        this.collectionOrder;
        this.parentGuardian;
        this.imposition;
        this.paymentTerms;
        this.minorCreditor;
        this.employer;
        this.defendant;
    }

}

module.exports = StagingEnforcementRequest;