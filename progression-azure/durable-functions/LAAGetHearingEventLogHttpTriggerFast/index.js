const getHearing = require('../HearingResultedCacheQuery/index');
const { laaFilter } = require('../LAAHearingResultedFilter/functions.js');
const getEventLog = require('../HearingEventLogQuery/index');

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
        const filteredJson = laaFilter(hearingJson);

        if (filteredJson.prosecutionCases && 
            filteredJson.prosecutionCases[0].defendants &&
            filteredJson.prosecutionCases[0].defendants.length > 0) {

            context.log(`LAA reference found for hearingId ${hearingId}`)
            context.log(`Filtered JSON is ` + JSON.stringify(filteredJson));
            context.bindings.params.filteredJson = filteredJson;

            context.bindings.params.hearingDate = hearingDate;

            try {
                context.log(`Getting event log for hearing ${hearingId} with date ${hearingDate}`);
                const eventLogJson = await getEventLog(context);
                context.log(`Returning event log for hearing ${hearingId} with date ${hearingDate}`);
                context.res.body = (!eventLogJson) ? {} : eventLogJson;
            } catch (e) {
                context.log(`A server error occured while retrieving the event log`)
                context.res.body = {'error': `An unknown error has occured while retrieving the event log`}
                context.res.status = 503
            }

        } else {
            context.log(`No LAA reference found for hearing ${hearingId} with date ${hearingDate}`);
            context.res.body = {}
        }
    } else {
        context.log(`No data found for hearing ${hearingId} with date ${hearingDate}`);
        context.res.body = {}
    }

};
