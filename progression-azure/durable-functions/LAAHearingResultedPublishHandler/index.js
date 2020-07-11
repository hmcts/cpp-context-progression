/*
 * This function is not intended to be invoked directly. Instead it will be
 * triggered by an HTTP starter function.
 * 
 * Before running this sample, please:
 * - create a Durable activity function (default name is 'Hello')
 * - create a Durable HTTP starter function
 * - run 'npm install durable-functions' from the wwwroot folder of your 
 *    function app in Kudu
 */

const df = require('durable-functions');

module.exports = df.orchestrator(function* (context) {

    const inputs = context.df.getInput();

    var params = {};
    params.hearingId = inputs.hearingId;
    params.cjscppuid = inputs.cjscppuid;
    params.payloadPrefix = "EXT_";
    params.redisClient = null;

    const unfilteredJson = yield context.df.callActivity('HearingResultedCacheQuery', params);

    const filteredJson = yield context.df.callActivity('LAAHearingResultedFilter', unfilteredJson);

    return yield context.df.callActivity('HearingResultedPublisher', filteredJson);

});