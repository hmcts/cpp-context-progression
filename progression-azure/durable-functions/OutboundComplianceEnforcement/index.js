const StagingEnforcementRequest = require('./StagingEnforcementRequest/Model/StagingEnforcement');
const Plea = require('./StagingEnforcementRequest/Model/Plea');
const ImpositionMapper = require('./StagingEnforcementRequest/Mapper/Imposition/ImpositionMapper');
const ParentGuardianMapper = require('./StagingEnforcementRequest/Mapper/ParentGuardian/ParentGuardianMapper');
const EmployerMapper = require('./StagingEnforcementRequest/Mapper/Employer/EmployerMapper');
const DefendantMapper = require('./StagingEnforcementRequest/Mapper/Defendant/DefendantMapper');
const CollectionOrder = require('./StagingEnforcementRequest/Mapper/CollectionOrder/CollectionOrderMapper');
const PaymentTermsMapper = require('./StagingEnforcementRequest/Mapper/PaymentTerms/PaymentTermsMapper');
const MinorCreditorMapper = require('./StagingEnforcementRequest/Mapper/MinorCreditor/MinorCreditorMapper');

class OutboundComplianceEnforcement {

    constructor(context, hearingJson, complianceEnforcementArray) {
        this.context = context;
        this.complianceEnforcementArray = complianceEnforcementArray;
        this.hearingJson = hearingJson;
    }

    buildEnforcement() {
        const stagingEnforcementRequestArray = [];
        this.context.log("Set enforcement request array size -->" + this.complianceEnforcementArray && this.complianceEnforcementArray.length);
        this.complianceEnforcementArray.forEach(enforcement => {
            //Check whether major or minor creditor
            const enforcementRequest = new StagingEnforcementRequest();
            enforcementRequest.requestId = enforcement.complianceCorrelationId;
            enforcementRequest.prosecutionAuthorityCode = enforcement.prosecutingAuthorityCode;
            enforcementRequest.prosecutionCaseReference = enforcement.prosecutionCaseURNPRN;
            enforcementRequest.originator = "Courts";
            enforcementRequest.imposingCourt = this.getIfFieldExists(this.hearingJson.courtCentre).id;
            enforcementRequest.plea = this.buildPlea(enforcement);
            enforcementRequest.collectionOrder = this.getCollectionOrder(enforcement).buildCollectionOrder();
            enforcementRequest.parentGuardian = this.getParentGuardianMapper(enforcement).buildParentGuardian();
            enforcementRequest.paymentTerms = this.getPaymentTermsMapper(enforcement).buildPayment();
            enforcementRequest.imposition = this.getImpositionMapper(enforcement).buildImposition();
            enforcementRequest.employer = this.getEmployerMapper(enforcement).buildEmployer();
            enforcementRequest.defendant = this.getDefendantMapper(enforcement).buildDefendant();
            enforcementRequest.minorCreditor = this.getMinorCreditorMapper(enforcement).buildMinorCreditor();
            stagingEnforcementRequestArray.push(enforcementRequest);
        });
        return stagingEnforcementRequestArray;
    }

    getMinorCreditorMapper(enforcement) {
        return new MinorCreditorMapper(enforcement);
    }

    getDefendantMapper(enforcement) {
        return new DefendantMapper(enforcement, this.hearingJson);
    }

    getEmployerMapper(enforcement) {
        return new EmployerMapper(enforcement, this.hearingJson);
    }

    getImpositionMapper(enforcement) {
        return new ImpositionMapper(enforcement, this.hearingJson);
    }

    getPaymentTermsMapper(enforcement) {
        return new PaymentTermsMapper(enforcement, this.hearingJson);
    }

    getParentGuardianMapper(enforcement) {
        return new ParentGuardianMapper(enforcement, this.hearingJson);
    }

    getCollectionOrder(enforcement) {
        return new CollectionOrder(enforcement, this.hearingJson);
    }

    buildPlea(enforcement) {
        const plea = new Plea();
        plea.includesOnline = false;
        plea.includesGuilty = enforcement.includesGuiltyPlea;
        return plea;
    }

    getIfFieldExists(obj) {
        if (obj) {
            return obj
        } else {
            return {}
        }
    }

}

module.exports = async function (context) {
    const complianceEnforcement = context.bindings.params.complianceEnforcements;
    const hearingJson = context.bindings.params.hearingResultedJson;
    return new OutboundComplianceEnforcement(context, hearingJson, complianceEnforcement).buildEnforcement();
};