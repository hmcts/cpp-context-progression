const ResultDefinition = require('../../../../NowsHelper/constants/ResultDefinitionConstants');
const {CollectionOrder} = require('../../Model/CollectionOrder');
const Mapper = require('../Mapper');
const ReserveTermsMapper = require('./ReserveTerms/ReserveTermsMapper');

class CollectionOrderMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    buildCollectionOrder() {
        const collectionOrder = new CollectionOrder();
        collectionOrder.isCollectionOrderMade = this.isCollectionOrderMade();
        collectionOrder.isApplicationForBenefitsDeduction =
            this.isApplicationForBenefitsDeduction();
        collectionOrder.isAttachmentOfEarnings = this.isAttachmentOfEarnings();
        const reserveTerms = this.getReserveTermsMapper().getReserveTerms();
        collectionOrder.reserveTerms = reserveTerms ? reserveTerms : undefined;
        return collectionOrder;
    }

    isAttachmentOfEarnings() {
        return this.complianceEnforcement.collectionOrderResults ? this.complianceEnforcement.collectionOrderResults &&
               this.complianceEnforcement.collectionOrderResults.some(collectionOrder => {
                   return collectionOrder.judicialResultTypeId
                          === ResultDefinition.ATTACHMENT_OF_EARNINGS
               }): false;
    }

    isApplicationForBenefitsDeduction() {
        return this.complianceEnforcement.collectionOrderResults ?  this.complianceEnforcement.collectionOrderResults &&
               this.complianceEnforcement.collectionOrderResults.some(collectionOrder => {
                   return collectionOrder.judicialResultTypeId
                          === ResultDefinition.APPLICATION_FOR_BENEFITS_DEDUCTION
               }) : false;
    }

    isCollectionOrderMade() {
        return this.complianceEnforcement.collectionOrderResults &&
               this.complianceEnforcement.collectionOrderResults.some(collectionOrder => {
                   return collectionOrder.judicialResultTypeId === ResultDefinition.COLLECTION_ORDER
               });
    }

    getReserveTermsMapper() {
        return new ReserveTermsMapper(this.complianceEnforcement, this.hearingJson);
    }

}

module.exports = CollectionOrderMapper;