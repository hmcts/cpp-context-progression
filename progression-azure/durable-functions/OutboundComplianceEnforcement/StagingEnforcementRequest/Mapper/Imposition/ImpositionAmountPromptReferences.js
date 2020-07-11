const ImpositionResultCode = require('./ImpositionResultCode');
const PromptType = require('../../../PromptType');

class ImpositionAmountPromptReferences {

    constructor() {
        this.impositionAmountPromptReferences = new Map();
        this.impositionAmountPromptReferences.set(ImpositionResultCode.FO,
                                                  PromptType.AMOUNT_OF_FINE);
        this.impositionAmountPromptReferences.set(ImpositionResultCode.FCPC,
                                                  PromptType.AMOUNT_OF_COSTS);
        this.impositionAmountPromptReferences.set(ImpositionResultCode.FVS,
                                                  PromptType.AMOUNT_OF_SURCHARGE);
        this.impositionAmountPromptReferences.set(ImpositionResultCode.FVEBD,
                                                  PromptType.AMOUNT_OF_BACK_DUTY);
        this.impositionAmountPromptReferences.set(ImpositionResultCode.FCOST,
                                                  PromptType.AMOUNT_OF_COSTS);
        this.impositionAmountPromptReferences.set(ImpositionResultCode.FCOMP,
                                                  PromptType.AMOUNT_OF_COMPENSATION);
    }

}

module.exports = ImpositionAmountPromptReferences;