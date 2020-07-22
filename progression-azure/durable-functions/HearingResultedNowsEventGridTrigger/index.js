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

    if(eventGridEvent.data.hearingId){
        context.log("Hearing id in the HearingResultedNowsEventGridTrigger -->> " + eventGridEvent.data.hearingId)
        const instanceId = await client.startNew("HearingResultedNowsHandler", undefined, input);
        context.log(`Started HearingResultedNowsHandler orchestration with ID = ${instanceId}.`);
    }else{
        context.log('Hearing id undefined, not executing HearingResultedNowsHandler orchestration');
    }
};
