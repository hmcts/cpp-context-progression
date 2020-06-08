const getHearing = require('../HearingResultedCacheQuery/index');
const { laaFilter } = require('../LAAHearingResultedFilter/functions.js');
const getEventLog = require('../HearingEventLogQuery/index');

function error(context, logMessage) {
    context.log(`An error occured while filtering the hearing result: ${logMessage}`);
    context.res.body = {'error': `An unknown error has occured while filtering the hearing result`};
    context.res.status = 503
}

module.exports = async function (context, req) {
    context.log('JavaScript HTTP trigger function processed a request.');

    if (!req.query.date) {
        context.log(`date param not provided`);
        context.res = {
            status: 400,
            body: 'A date is required either in the query string'
        };
        return
    }

    if (!req.query.hearingId) {
        context.log(`hearingId param not provided`);
        context.res = {
            status: 400,
            body: 'A hearingId is required either in the query string'
        };
        return
    }

    const hearingId = (req.query.hearingId || req.body.hearingId);
    const hearingDate = req.query.date;

    context.bindings = {
        params:  {
            hearingId: hearingId,
            redisClient: null,
            cjscppuid: req.headers.cjscppuid
        }
    };

    context.res.status = 200;
    context.log(`Getting details for hearing ${hearingId}`);
    const hearingJson = await getHearing(context);
    context.log(`Returning details for hearing ${hearingId}`);

    if (hearingJson) {

        const filteredJson = laaFilter(hearingJson, context);

        if (filteredJson == null) error(context, 'filtered JSON is null'); else
        if (Object.keys(filteredJson).length === 0) error(context, `No LAA reference found for hearing ${hearingId} with date ${hearingDate}`)
        if (!filteredJson.hearing) error(context, 'filtered JSON has no hearing element'); else
        if (!filteredJson.hearing.prosecutionCases) error(context, 'filtered JSON has no hearing.prosecutionCases element'); else {

            context.log(`Element 'hearing' found in filtered JSON, as expected`)

            if (filteredJson.hearing.prosecutionCases[0].defendants &&
                filteredJson.hearing.prosecutionCases[0].defendants.length > 0) {

                context.bindings.params.filteredJson = filteredJson;
                context.bindings.params.hearingDate = hearingDate;

                try {
                    context.log(`Getting event log for hearing ${hearingId} with date ${hearingDate}`);
                    const eventLogJson = await getEventLog(context);
                    context.log(`Returning event log for hearing ${hearingId} with date ${hearingDate}`);
                    context.res.body = (!eventLogJson) ? {} : eventLogJson;
                } catch (e) {
                    error(context, `Could not retrieve event log for hearing ${hearingId}`)
                }

            }
        }
    } else {
        context.log(`No data found for hearing ${hearingId} with date ${hearingDate}`);
        context.res.body = {}
    }

};
