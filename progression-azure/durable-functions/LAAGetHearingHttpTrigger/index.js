const getHearing = require('../HearingResultedCacheQuery/index');
const { laaFilter } = require('../LAAHearingResultedFilter/functions.js');

module.exports = async function (context, req) {
    context.log('JavaScript HTTP trigger function processed a request.');

    if (req.query.hearingId || (req.body && req.body.hearingId)) {
        const hearingId = (req.query.hearingId || req.body.hearingId);

        context.bindings = {
            params:  {
                hearingId: hearingId,
                redisClient: null,
                cjscppuid: req.headers.cjscppuid,
                payloadPrefix: "EXT_"
            }
        };

        context.res.status = 200;

        context.log(`Getting details for hearing ${hearingId}`);
        const json = await getHearing(context);

        context.log(`Returning details for hearing ${hearingId}`);
        context.res.body = (!json) ? {} : laaFilter(json, context);
    }
    else {
        context.log(`hearingId not provided`);

        context.res = {
            status: 400,
            body: 'A hearingId is required either on the query string (GET) or in the request body (POST)'
        };
    }
};
