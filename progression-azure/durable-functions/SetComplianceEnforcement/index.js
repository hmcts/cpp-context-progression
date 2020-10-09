const ComplianceEnforcement = require('./ComplianceEnforcement');
const DefendantContextService = require('../NowsHelper/service/DefendantContextBaseService');
const ResultDefinitionConstants = require('../NowsHelper/constants/ResultDefinitionConstants');

const uuidv4 = require('uuid/v4');

const GUILTY = 'GUILTY';

class SetComplianceEnforcement {

    constructor(context, hearingResultedJson, groupResultsByMasterDefendantId) {
        this.context = context;
        this.hearingResultedJson = hearingResultedJson;
        this.groupResultsByMasterDefendantId = groupResultsByMasterDefendantId;
    }

    async buildComplianceEnforcement() {
        let listOfComplianceEnforcementForDefendant = [];
        this.groupResultsByMasterDefendantId.forEach((defendantContextBase) => {
            if (this.isAnyJudicialResultFinancial(defendantContextBase.results)) {
                let complianceEnforcement = new ComplianceEnforcement();
                complianceEnforcement.masterDefendantId = defendantContextBase.masterDefendantId;
                complianceEnforcement.complianceCorrelationId = uuidv4();
                complianceEnforcement.orderedDate = defendantContextBase.orderedDate;
                this.setProsecutionCaseDetails(defendantContextBase, complianceEnforcement);
                this.setProsecutionCasePleaDetails(defendantContextBase, complianceEnforcement);

                // Does Standalone application supports Financial order ????? TO DO
                // this.setApplicationWithPleaDetails(defendantContextBase, complianceEnforcement);
                this.setMinorCreditorResults(defendantContextBase.results, complianceEnforcement);
                this.setEmployerResults(defendantContextBase.results, complianceEnforcement);
                this.setPaymentTermsResults(defendantContextBase.results, complianceEnforcement);
                this.setCollectionOrderResults(defendantContextBase.results, complianceEnforcement);
                this.setReserveTermResults(defendantContextBase.results, complianceEnforcement);
                this.setImpositionResults(defendantContextBase.results, complianceEnforcement);
                listOfComplianceEnforcementForDefendant.push(complianceEnforcement);
            }
        });
        return listOfComplianceEnforcementForDefendant;
    }

    isAnyJudicialResultFinancial(results) {
        let isFinancialResult = false;
        results.forEach(result => {
            if (result.judicialResult.isFinancialResult === true) {
                isFinancialResult = true;
            }
        });
        return isFinancialResult;
    }

    setMinorCreditorResults(results, complianceEnforcement) {
        results.forEach(result => {
            if ((result.judicialResult.judicialResultTypeId
                 === ResultDefinitionConstants.FCOST) || (result.judicialResult.judicialResultTypeId
                                                          === ResultDefinitionConstants.FCOMP) || (result.judicialResult.judicialResultTypeId
                                                                                                   === ResultDefinitionConstants.STRO)) {
                const judicialResultPrompts = result.judicialResult.judicialResultPrompts;
                if (judicialResultPrompts && judicialResultPrompts.length && this.isMinorCreditor(
                    judicialResultPrompts)) {
                    complianceEnforcement.minorCreditorResults =
                        complianceEnforcement.minorCreditorResults || [];
                    const judicialResult = result.judicialResult;
                    judicialResult.minorCreditorId = judicialResult.judicialResultId;
                    complianceEnforcement.minorCreditorResults.push(judicialResult);
                }
            }
        });
    }

    setImpositionResults(results, complianceEnforcement) {
        results.forEach(r => {
            if ((complianceEnforcement.employerResults
                 && complianceEnforcement.employerResults.some(
                    (result) => result.judicialResultId === r.judicialResult.judicialResultId))) {
                return;
            }
            if ((complianceEnforcement.paymentTermsResults
                 && complianceEnforcement.paymentTermsResults.some(
                    (result) => result.judicialResultId === r.judicialResult.judicialResultId))) {
                return;
            }
            if ((complianceEnforcement.reserveTermsResults
                 && complianceEnforcement.reserveTermsResults.some(
                    (result) => result.judicialResultId === r.judicialResult.judicialResultId))) {
                return;

            }
            if ((complianceEnforcement.collectionOrderResults
                 && complianceEnforcement.collectionOrderResults.some(
                    (result) => result.judicialResultId === r.judicialResult.judicialResultId))) {
                return;
            }
            complianceEnforcement.impositionResults = complianceEnforcement.impositionResults || [];
            complianceEnforcement.impositionResults.push(r.judicialResult);
        });
    }

    setApplicationWithPleaDetails(defendantContextBase, complianceEnforcement) {
        if (this.hearingResultedJson.courtApplications) {
            let matchingCase = this.hearingResultedJson.courtApplications.filter(
                (application) => {
                    return application.applicant.defendant.masterDefendantId
                           === defendantContextBase.masterDefendantId
                });
            complianceEnforcement.prosecutionCaseURNPRN =
                matchingCase[0].applicationReference;
            complianceEnforcement.includesGuiltyPlea =
                this.isGuiltyPleaForApplication(this.hearingResultedJson,
                                                defendantContextBase.masterDefendantId);
        }
    }

    setProsecutionCaseDetails(defendantContextBase, complianceEnforcement) {
        if (this.hearingResultedJson.prosecutionCases) {
            let matchingCase = this.hearingResultedJson.prosecutionCases.filter(
                (prosecutionCase) => {
                    return prosecutionCase.defendants.filter(
                        (defendant) => defendant.masterDefendantId
                                       === defendantContextBase.masterDefendantId).length
                           > 0
                });
            complianceEnforcement.prosecutionCaseURNPRN =
                matchingCase[0].prosecutionCaseIdentifier.prosecutionAuthorityReference
                ||
                matchingCase[0].prosecutionCaseIdentifier.caseURN;
            complianceEnforcement.prosecutingAuthorityCode =
                matchingCase[0].prosecutionCaseIdentifier.prosecutionAuthorityCode;
        }
    }

    setProsecutionCasePleaDetails(defendantContextBase, complianceEnforcement) {
        complianceEnforcement.includesGuiltyPlea =
            this.isGuiltyPleaForProsecutionCase(this.hearingResultedJson,
                                                defendantContextBase.masterDefendantId);
    }

    isGuiltyPleaForProsecutionCase(hearingJson, masterDefendantId) {
        const matchingCaseWithOffencePleadGuilty = hearingJson.prosecutionCases.filter(
            (prosecutionCase) => {
                return prosecutionCase.defendants.filter(
                    (defendant) => {
                        return defendant.masterDefendantId === masterDefendantId
                               && defendant.offences.filter(
                                (offence) => offence.plea && offence.plea.pleaValue
                               === GUILTY).length > 0
                    }).length > 0;
            });
        return matchingCaseWithOffencePleadGuilty.length > 0;
    }

    isGuiltyPleaForApplication(hearingJson, masterDefendantId) {
        const matchingCaseWithOffencePleadGuilty = hearingJson.courtApplications.filter(
            (application) => {
                return application.applicant.defendant.masterDefendantId === masterDefendantId
                       && application.applicant.defendant.offences.filter(
                        (offence) => offence.plea.pleaValue === GUILTY).length > 0
            });
        return matchingCaseWithOffencePleadGuilty.length > 0;
    }

    setEmployerResults(results, complianceEnforcement) {
        results.forEach(result => {
            if (result.judicialResult.judicialResultTypeId
                === ResultDefinitionConstants.ATTACHMENT_OF_EARNINGS) {
                complianceEnforcement.employerResults = complianceEnforcement.employerResults || [];
                complianceEnforcement.employerResults.push(result.judicialResult);
            }
        });
    }

    setPaymentTermsResults(results, complianceEnforcement) {
        results.forEach(result => {
            if (this.isResultDefinitionHasPaymentTerms(result.judicialResult.judicialResultTypeId)) {
                complianceEnforcement.paymentTermsResults =
                    complianceEnforcement.paymentTermsResults || [];
                complianceEnforcement.paymentTermsResults.push(result.judicialResult);
            }
        });
    }

    setReserveTermResults(results, complianceEnforcement) {
        results.forEach(result => {
            if (this.isResultDefinitionHasReserveTerms(
                result.judicialResult.judicialResultTypeId)) {
                complianceEnforcement.reserveTermsResults =
                    complianceEnforcement.reserveTermsResults || [];
                complianceEnforcement.reserveTermsResults.push(result.judicialResult);
            }
        });
    }

    setCollectionOrderResults(results, complianceEnforcement) {
        results.forEach(result => {
            if ([
                ResultDefinitionConstants.COLLECTION_ORDER,
                ResultDefinitionConstants.ATTACHMENT_OF_EARNINGS,
                ResultDefinitionConstants.APPLICATION_FOR_BENEFITS_DEDUCTION,
                ResultDefinitionConstants.PAYMENT_TERMS_ON_RELEASE
            ].includes(result.judicialResult.judicialResultTypeId)) {
                complianceEnforcement.collectionOrderResults =
                    complianceEnforcement.collectionOrderResults || [];
                complianceEnforcement.collectionOrderResults.push(result.judicialResult);
            }
        });
    }

    isResultDefinitionHasPaymentTerms(resultDefinition) {
        return [
            ResultDefinitionConstants.PAY_BY_DATE,
            ResultDefinitionConstants.INSTALLMENTS_ONLY,
            ResultDefinitionConstants.LUMP_SUM_PLUS_INSTALLMENTS,
            ResultDefinitionConstants.PAYMENT_TERMS_ON_RELEASE

        ].includes(resultDefinition)
    }

    isResultDefinitionHasReserveTerms(resultDefinition) {
        return resultDefinition === ResultDefinitionConstants.RESERVE_TERMS_LUMP_SUM
               || resultDefinition
               === ResultDefinitionConstants.RESERVE_TERMS_INSTALLMENTS_ONLY || resultDefinition
               === ResultDefinitionConstants.RESERVE_TERMS_LUMP_SUM_PLUS_INSTALLMENT;
    }

    isMinorCreditor(judicialResultPrompts) {
        const minorCreditor = judicialResultPrompts.find(
            prompt => prompt.promptReference.includes(ResultDefinitionConstants.MINOR_CREDITOR));

        if (minorCreditor) {
            return true;
        }
    }

}

module.exports = async function (context) {
    context.log("inside Set Compliance Enforcement");
    const hearingJson = context.bindings.params.hearingResultedObj;

    // context.log("hearing json inside Set Compliance -->>" + JSON.stringify(hearingJson));

    const defendantContextBaseList = new DefendantContextService(
        hearingJson).getDefendantContextBaseList();

    const groupResultsByMasterDefendantId = defendantContextBaseList.map((defendantContextBase) => {
        return {
            masterDefendantId: defendantContextBase.masterDefendantId,
            results: defendantContextBase.results,
            orderedDate: defendantContextBase.orderedDate
        };
    });
    // context.log("SetCompliance -->> " + JSON.stringify(groupResultsByMasterDefendantId));
    return await new SetComplianceEnforcement(context, hearingJson,
                                              groupResultsByMasterDefendantId).buildComplianceEnforcement();
};


