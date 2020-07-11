const {default: axiosStatic} = require('axios');

class ProcessOutboundComplianceEnforcement {

    constructor(context) {
        this.context = context;
        this.stagingEnforcementRequests = context.bindings.params.stagingEnforcementRequests;
    }

    async sendToEnforcement(stagingEnforcementRequest) {
        const enforcementEndpoint = process.env.ENFORCEMENT_CONTEXT_API_BASE_URI + '/stagingenforcement-command-api/command/api/rest/stagingenforcement/enforce-financial-imposition';
        this.context.log(`Calling command ${enforcementEndpoint}`);
        try {
            return await axiosStatic.post(enforcementEndpoint, stagingEnforcementRequest, {
                headers: {
                    'CJSCPPUID': this.context.bindings.params.cjscppuid,
                    'Content-type': 'application/vnd.stagingenforcement.enforce-financial-imposition+json'
                },
            });
        } catch (err) {
            this.context.log.error(`Unexpected error occurred invoking enforcement ${err}`);
            this.context.log('enforcements --> ' + JSON.stringify(stagingEnforcementRequest));
        }
    }

    async processRequests() {
        for(let stagingEnforcementRequest of this.stagingEnforcementRequests){
            await this.sendToEnforcement(stagingEnforcementRequest);
        }

        return this.stagingEnforcementRequests;
    }
}

module.exports = async function (context) {
    return await new ProcessOutboundComplianceEnforcement(context).processRequests();
};