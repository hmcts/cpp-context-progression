const {default: axiosStatic} = require('axios');

class ProcessOutboundPrisonCourtRegister {

    constructor(context) {
        this.context = context;
        this.outboundPrisonCourtRegisters = this.context.bindings.params.outboundPrisonCourtRegisters;
    }

    async sendToProgression(prisonCourtRegisterRequest) {
        const prisonCourtRegisterEndPoint = process.env.PROGRESSION_CONTEXT_API_BASE_URI + '/progression-command-api/command/api/rest/progression/prison-court-register';
        try {
            this.context.log(`Calling command ${prisonCourtRegisterEndPoint}`);
            return await axiosStatic.post(prisonCourtRegisterEndPoint, prisonCourtRegisterRequest, {
                headers: {
                    'CJSCPPUID': this.context.bindings.params.cjscppuid,
                    'Content-type': 'application/vnd.progression.add-prison-court-register+json'
                },
            });
        } catch (err) {
            this.context.log.error('Unexpected error occurred invoking prison court register'+ err);
            this.context.log('prisonCourtRegisterRequest ==> ' +JSON.stringify(prisonCourtRegisterRequest));
        }
    }

    async processRequests() {
        for(let prisonCourtRegister of this.outboundPrisonCourtRegisters) {
            await this.sendToProgression(prisonCourtRegister);
        }
        return this.outboundPrisonCourtRegisters;
    }
}

module.exports = async function (context) {
    return await new ProcessOutboundPrisonCourtRegister(context).processRequests();
};


