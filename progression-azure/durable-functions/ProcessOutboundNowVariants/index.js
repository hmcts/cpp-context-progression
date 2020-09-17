const {default: axiosStatic} = require('axios');

class ProcessOutboundNowVariants {

    constructor(context) {
        this.context = context;
        this.outboundNowsVariants = context.bindings.params.outboundNowsVariants;
    }

    async sendToProgression(outboundNowsVariant) {

        const nowDocumentRequestEndpoint = process.env.PROGRESSION_CONTEXT_API_BASE_URI + '/progression-command-api/command/api/rest/progression/nows';

        this.context.log(`Calling command ${nowDocumentRequestEndpoint}  - ${this.context.bindings.params.cjscppuid} - ${outboundNowsVariant.masterDefendantId} - ${outboundNowsVariant.materialId}`);
        try {
            return await axiosStatic.post(nowDocumentRequestEndpoint, outboundNowsVariant, {
                headers: {
                    'CJSCPPUID': this.context.bindings.params.cjscppuid,
                    'Content-type': 'application/vnd.progression.add-now-document-request+json'
                },
            });
        } catch (err) {
            this.context.log.error(`Unexpected error occurred invoking progression ${err}`);
            this.context.log('outboundNowsVariant --> ' + JSON.stringify(outboundNowsVariant));
        }
    }

    async processRequests() {
        this.context.log(`${this.outboundNowsVariants.length} nows variants has created.`);
        for(let outboundNowsVariant of this.outboundNowsVariants){
            // this.context.log('outboundNowsVariant --> ' + JSON.stringify(outboundNowsVariant));
            await this.sendToProgression(outboundNowsVariant);
        }
        this.outboundNowsVariants = {};
        return this.outboundNowsVariants;
    }
}

module.exports = async (context) => {
    return await new ProcessOutboundNowVariants(context).processRequests();
};
