const df = require("durable-functions");

module.exports = async function (context, eventGridEvent) {

    context.log('Subject: ' + eventGridEvent.subject);
    context.log('Time: ' + eventGridEvent.eventTime);
    context.log('Data: ' + JSON.stringify(eventGridEvent.data));

    const client = df.getClient(context);

    var input = {
        hearingId: eventGridEvent.data.hearingId,
        cjscppuid: eventGridEvent.data.userId
    }

    const instanceId = await client.startNew("HearingResultedNowsHandler", undefined, input);
    context.log(`Started HearingResultedNowsHandler orchestration with ID = ${instanceId}.`);
};
