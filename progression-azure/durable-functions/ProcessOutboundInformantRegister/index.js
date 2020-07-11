const {default: axiosStatic} = require('axios');

class ProcessOutboundInformantRegister {

    constructor(context) {
        this.context = context;
    }

    async sendToInformantRegister(outboundInformantRegister) {

        const informantRegisterEndpoint = process.env.PROGRESSION_CONTEXT_API_BASE_URI + '/progression-command-api/command/api/rest/progression/informant-register';

        this.context.log(`Calling command ${informantRegisterEndpoint}`);

        try {
            return await axiosStatic.post(informantRegisterEndpoint, outboundInformantRegister, {
                headers: {
                    'CJSCPPUID': this.context.bindings.params.cjscppuid,
                    'Content-type': 'application/vnd.progression.add-informant-register+json'
                },
            });
        } catch (err) {
            this.context.log.error('Unexpected error occurred invoking informant register -->>', err);
            this.context.log('outboundInformantRegister --- ' + JSON.stringify(outboundInformantRegister));
        }
    }

    async processRequests() {
        const outboundInformantRegisters = this.context.bindings.params.outboundInformantRegisters;

        for (let outboundInformantRegister of outboundInformantRegisters) {
            await this.sendToInformantRegister(outboundInformantRegister);
        }

        return outboundInformantRegisters;
    }
}

module.exports = async function (context) {
    return await new ProcessOutboundInformantRegister(context).processRequests();
};
