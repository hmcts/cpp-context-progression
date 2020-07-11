const {default: axiosStatic} = require('axios');

class ProcessOutboundCourtRegister {

    constructor(context) {
        this.context = context;
    }

    async processRequests() {
        const outboundCourtRegister = this.context.bindings.params.outboundCourtRegister;
        try {
            const endPoint = process.env.PROGRESSION_CONTEXT_API_BASE_URI + '/progression-command-api/command/api/rest/progression/court-register';
            this.context.log(`Calling command ${endPoint}`);
            await axiosStatic.post(endPoint, outboundCourtRegister, {
                headers: {
                    'CJSCPPUID': this.context.bindings.params.cjscppuid,
                    'Content-type': 'application/vnd.progression.add-court-register+json'
                },
            });
        } catch (err) {
            this.context.log.error('Unexpected error occurred invoking progression court-register', err);
        }
        return outboundCourtRegister;
    }
}

module.exports = async function (context) {
    return await new ProcessOutboundCourtRegister(context).processRequests();
};
