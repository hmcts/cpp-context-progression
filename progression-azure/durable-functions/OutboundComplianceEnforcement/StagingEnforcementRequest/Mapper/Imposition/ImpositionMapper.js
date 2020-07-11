const Imposition = require('../../Model/Imposition');
const Mapper = require('../Mapper');
const MajorCreditor = require('./MajorCreditor');
const ImpositionAmountPromptReferences = require('./ImpositionAmountPromptReferences');
const ImpositionResultDefinitionCodes = require('./ImpositionResultDefinitionCodes');
const PromptType = require('../../../PromptType');
const ImpositionResultCode = require('../Imposition/ImpositionResultCode');

class ImpositionMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
        this.impositionResultCodes = new ImpositionResultDefinitionCodes();
        this.impositionAmountPromptReference = new ImpositionAmountPromptReferences();
        this.majorCreditor = new MajorCreditor();
    }

    buildImposition() {
        const impositionList = [];
        this.complianceEnforcement.impositionResults.forEach(impositionResult => {
            const impositionCode = this.impositionResultCodes.impositionCode(
                impositionResult.judicialResultTypeId);
            const minorCreditorImpositionResult = this.getMinorCreditorImposition(impositionResult);
            if (impositionCode) {
                const imposition = new Imposition();
                /*
                 * Note : prosecutionAuthorityId - Not required by courts as in courts any major creditor may be provided.
                 * This is provided in the design for ATCM.
                 */
                // imposition.prosecutionAuthorityId = null;
                imposition.offenceCode = this.getOffenceCode(impositionResult);
                imposition.impositionResultCode = impositionCode;
                const impositionAmount = this.getImpositionAmount(impositionCode, impositionResult);
                imposition.impositionAmount =
                    Number(parseFloat(impositionAmount.startsWith('£') ? impositionAmount.substring(1)
                                                                : impositionAmount).toFixed(2));
                if(minorCreditorImpositionResult) {
                    imposition.minorCreditorId = minorCreditorImpositionResult.minorCreditorId;
                } else {
                    imposition.majorCreditor = this.getMajorCreditor(impositionCode, impositionResult);
                }

                impositionList.push(imposition);
            }
        });
        return impositionList;
    }

    getImpositionAmount(impositionCode, impositionResult) {
        if (!impositionResult.isDeleted) {
            const promptReference = this.impositionAmountPromptReference.impositionAmountPromptReferences
                .get(impositionCode);
            if (promptReference) {
                const judiciaPrompt = impositionResult.judicialResultPrompts &&
                                       impositionResult.judicialResultPrompts.find(
                                           judicialResultPrompt => judicialResultPrompt.promptReference
                                                                   === promptReference)
                return judiciaPrompt ? judiciaPrompt.value : "0";
            }
        }
        return "0";
    }

    getOffenceCode(impositionResult) {
        const offence = this.getDefendant().offences.find(offence => {
            return offence.judicialResults && offence.judicialResults.some(judicialResult => judicialResult.judicialResultId
                                                             === impositionResult.judicialResultId)
        });
        if(offence){
            return offence.offenceCode;
        }
        //If the judicial results is at the case level, then take the first offence
        return this.getDefendant().offences[0].offenceCode;
    }

    getMajorCreditor(impositionCode, impositionResult) {
        let majorCreditor;
        //        only applies where impositionResultCode ==  impositionResultCode.FCOMP ||
        // impositionResultCode ==  impositionResultCode.FCOST. In these cases when a resultCode is FCOMP,
        // look for an associated prompt "Creditor name" (line 296). When the resultCode is FCOST, look for
        // an associated prompt  "Creditor name" (line 2061).
        if (impositionCode === ImpositionResultCode.FCOMP || impositionCode
            === ImpositionResultCode.FCOST) {
            const judicialPrompt = impositionResult.judicialResultPrompts
                                   && impositionResult.judicialResultPrompts.find(
                    judicialResultPrompt =>
                        judicialResultPrompt.promptReference === PromptType.CREDITOR_NAME);
            majorCreditor = judicialPrompt ? judicialPrompt.value : undefined;
            if (!majorCreditor) {
                majorCreditor = this.hearingJson.prosecutionCases.map(
                    prosecutionCase => prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode)[0];
            }
        }
        return majorCreditor ? this.majorCreditor.majorCreditorMap.get(majorCreditor) : undefined;
    }

    getMinorCreditorImposition(impositionResult) {
        if(this.complianceEnforcement.minorCreditorResults && this.complianceEnforcement.minorCreditorResults.length) {
            const minorCreditorImpositionResult = this.complianceEnforcement.minorCreditorResults.find(
                result => result.judicialResultId === impositionResult.judicialResultId
            );
            if(minorCreditorImpositionResult) {
                return minorCreditorImpositionResult;
            }
        }
    }

}

module.exports = ImpositionMapper;