const df = require('durable-functions');

module.exports = df.orchestrator(function* (context) {

    context.log(`Get Event Log orchestrator started`);

    const inputs = context.df.getInput();

    var params = {};
    params.hearingId = inputs.hearingId;
    params.hearingDate = inputs.hearingDate;
    params.cjscppuid = inputs.cjscppuid;
    params.redisClient = null;

    context.log(`Calling HearingResultedCacheQuery`);
    const unfilteredJson = yield context.df.callActivity('HearingResultedCacheQuery', params);

    if (unfilteredJson) {
        context.log(`Calling LAAHearingResultedFilter`);
        const filteredJson = yield context.df.callActivity('LAAHearingResultedFilter', unfilteredJson);

        params.filteredJson = filteredJson;
        context.log(`Calling HearingEventLogQuery with params` + JSON.stringify(filteredJson.jurisdictionType));
        const eventLog = yield context.df.callActivity('HearingEventLogQuery', params);

        context.log(`Event log returned from HearingEventLogQuery is ` + JSON.stringify(eventLog));

        return eventLog ? eventLog : {"error": "an unknown error occured"};
    }

});
