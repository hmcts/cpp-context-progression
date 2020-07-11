const UserGroup = require('./UserGroup');

class NowVariant {
    constructor() {
        this.orderDate = undefined;
        this.amendmentDate = undefined;
        this.complianceCorrelationId = undefined;
        this.now = undefined;
        this.masterDefendantId = undefined;
        this.results = [];
        this.vocabulary = undefined;
        this.matchedSubscription = undefined;
        this.userGroup = new UserGroup();
        this.registerDefendant = undefined;
    }
}

module.exports = NowVariant;