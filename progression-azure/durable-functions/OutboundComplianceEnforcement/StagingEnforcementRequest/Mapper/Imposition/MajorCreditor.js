//Do we still need to hard codes thes values ??????
class MajorCreditor {
    get majorCreditorMap() {
        return this._majorCreditorMap;
    }

    constructor() {
        this._majorCreditorMap = new Map();
        this._majorCreditorMap.set("Transport for London", "TFL2");
        this._majorCreditorMap.set("Driver and Vehicle Licensing Agency", "DVL2");
        this._majorCreditorMap.set("Television Licensing Organisation", "TVL3");
        this._majorCreditorMap.set("TFL", "TFL2");
        this._majorCreditorMap.set("TVL", "TVL3");
    }
}

module.exports = MajorCreditor;