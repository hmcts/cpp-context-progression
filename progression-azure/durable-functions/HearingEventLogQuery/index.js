/*
 * This function is not intended to be invoked directly. Instead it will be
 * triggered by an orchestrator function.
 * 
 * Before running this sample, please:
 * - create a Durable orchestration function
 * - create a Durable HTTP starter function
 * - run 'npm install durable-functions' from the wwwroot folder of your
 *   function app in Kudu
 */

const { default: axiosStatic } = require('axios');

async function getHearingEventLog(hearingId, hearingDate, cjscppuid, context) {

    if (cjscppuid) {
        context.log(`Getting hearing event log for ${hearingId} for date ${hearingDate}`);
        const eventLogEndpoint = process.env.RESULTS_CONTEXT_API_BASE_URI +
            `/hearing-query-api/query/api/rest/hearing/hearings/${hearingId}/event-log?date=${hearingDate}`;
        
        context.log(`Querying ${eventLogEndpoint}`);

        try {
            const response = await axiosStatic.get(eventLogEndpoint, {
                headers: {
                    'CJSCPPUID': cjscppuid,
                    'Accept': 'application/vnd.hearing.hearing-event-log+json'
                }
            });

            return response.data;
        } catch (err) {
            context.log(err);
            throw `An error occured while querying the event log for hearing id ${hearingId} on date ${hearingDate}`;
        }
    } else {
        throw `No CJSCPPUID supplied for hearing ${hearingId}`;
    }

}

module.exports = async function (context) {

    const { filteredJson, hearingId, hearingDate, cjscppuid } = context.bindings.params;

    context.log(`Event log query called`);

    if (!filteredJson || Object.keys(filteredJson).length == 0) {
        context.log(`No data found in filtered JSON for hearing ${hearingId} when event log requested.`)
        return null;
    }

    context.log(`LAA reference found. Proceeding to get hearing event log for hearing ${hearingId} on date ${hearingDate}`);
    
    const eventLog = await getHearingEventLog(hearingId, hearingDate, cjscppuid, context);
    context.log(`Event log returned from getHearingEventLog function is ` + JSON.stringify(eventLog))
    return eventLog;

};
