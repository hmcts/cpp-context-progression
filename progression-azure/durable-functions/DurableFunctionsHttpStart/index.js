const df = require('durable-functions');

module.exports = async function (context, req) {
    context.log(`Creating orchestration client for function ${req.params.functionName}.`);

    const client = df.getClient(context);
    
    context.log(`Starting orchestration for function ${req.params.functionName}.`);

    const instanceId = await client.startNew(req.params.functionName, undefined, req.body);

    context.log(`Started orchestration with ID ${instanceId} for function ${req.params.functionName}.`);

    return client.createCheckStatusResponse(context.bindingData.req, instanceId);
};