const ImpositionResultCode = require('./ImpositionResultCode');

class ImpositionResultDefinitionCodes {

    constructor() {
        this.resultDefinitionImpositionCodes = new Map();
        this.resultDefinitionImpositionCodes.set("969f150c-cd05-46b0-9dd9-30891efcc766", ImpositionResultCode.FO);
        this.resultDefinitionImpositionCodes.set("f5d492b8-a09b-4f70-9ceb-aa06c306a7dc", ImpositionResultCode.FCPC);
        this.resultDefinitionImpositionCodes.set("e866cd11-6073-4fdf-a229-51c9d694e1d0", ImpositionResultCode.FVS);
        this.resultDefinitionImpositionCodes.set("5edd3a3a-8dc7-43e4-96c4-10fed16278ac", ImpositionResultCode.FVEBD);
        this.resultDefinitionImpositionCodes.set("76d43772-0660-4a33-b5c6-8f8ccaf6b4e3", ImpositionResultCode.FCOST);
        this.resultDefinitionImpositionCodes.set("ae89b99c-e0e3-47b5-b218-24d4fca3ca53", ImpositionResultCode.FCOMP);
        this.resultDefinitionImpositionCodes.set("7b2ac1e1-a802-41e3-a8e7-20661330a9e3", ImpositionResultCode.STRO);
    }

    impositionCode(resultDefinitionId) {
        return this.resultDefinitionImpositionCodes.get(resultDefinitionId);
    }
}

module.exports = ImpositionResultDefinitionCodes;