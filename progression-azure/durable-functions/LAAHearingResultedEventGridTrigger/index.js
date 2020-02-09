const df = require("durable-functions");

module.exports = async function (context, eventGridEvent) {

    context.log('Subject: ' + eventGridEvent.subject);
    context.log('Time: ' + eventGridEvent.eventTime);
    context.log('Data: ' + JSON.stringify(eventGridEvent.data));

    const client = df.getClient(context);

    var input = {
        hearingId: eventGridEvent.data.hearingId,
        cjscppuid: process.env.CJSCPPUID
    }

    const instanceId = await client.startNew("LAAHearingResultedPublishHandler", undefined, input);
    context.log(`Started orchestration with ID = ${instanceId}.`);
};
