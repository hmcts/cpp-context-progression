
const df = require("durable-functions");

module.exports = async function (context, req) {

    if (!req.query.hearingId && !(req.body && req.body.hearingId)) {
        context.log(`hearingId not provided`);

        context.res = {
            status: 400,
            body: 'A hearingId is required either on the query string (GET) or in the request body (POST)'
        };
        return;
    }

    if (!req.query.hearingDate) {
        context.log(`Missing query parameter: hearingDate`);

        context.res = {
            status: 400,
            body: 'Missing query parameter: hearingDate'
        };
        return;
    }

    if (!req.headers.cjscppuid) {
        context.log(`CJSCPPUID not provided`);

        context.res = {
            status: 400,
            body: 'A CJSCPPUID request header is required'
        };
        return;

    }

    const hearingId = (req.query.hearingId || req.body.hearingId);
    const hearingDate = req.query.hearingDate;

    context.log(`Get hearing event log http trigger started for hearing ${hearingId}`);

    const client = df.getClient(context);

    var input = {
        hearingId: hearingId,
        hearingDate: hearingDate,
        cjscppuid: req.headers.cjscppuid
    }

    context.log(`Obtained orchestration client. starting LAAGetHearingEventLogOrchestrator`);

    const instanceId = await client.startNew('LAAGetHearingEventLogOrchestrator', undefined, input);
    context.log(`Started orchestration with ID = ${instanceId}.`);

    const json = await client.waitForCompletionOrCreateCheckStatusResponse(
        context.bindingData.req, instanceId, 60000, 1000
    );

    context.log(`JSON returned from waitForCompletionOrCreateCheckStatusResponse is ` + JSON.stringify(json));

    if (json.status != 200) {
        context.res = {
            status: 408,
            body: 'Timeout'
        };
        return;
    }

    context.res = json;

};
