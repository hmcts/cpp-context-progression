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

var redisClient = null;

async function getHearingResult(hearingId, cjscppuid, context) {
    const resultsEndpoint = process.env.RESULTS_CONTEXT_API_BASE_URI +
        `/results-query-api/query/api/rest/results/hearingDetails/${hearingId}`;
    
    context.log(`Querying ${resultsEndpoint}`);

    try {
        const response = await axiosStatic.get(resultsEndpoint, {
            headers: {
                'CJSCPPUID': cjscppuid,
                'Accept': 'application/vnd.results.hearing-information-details+json'
            }
        });

        return response.data;
    } catch (err) {
        return null;
    }
}

function getCacheKey(hearingId) {
    return hearingId + '_result_';
}

function getRedisClient() {

    if (redisClient == null) {
        const {createClient} = require('redis');
        
        redisClient = createClient(process.env.REDIS_PORT, process.env.REDIS_HOST,
            {auth_pass: process.env.REDIS_KEY, tls: {servername: process.env.REDIS_HOST}});
    }

    return redisClient;
}

async function getResultFromCache(hearingId) {

    const {promisify} = require('util');
    const cacheKey = getCacheKey(hearingId);
    const client = getRedisClient();
    const getAsync = promisify(client.get).bind(client);

    const cachedResult = await getAsync(cacheKey);

    if (cachedResult == null) {
        return null;
    }

    return JSON.parse(cachedResult);
}

async function getHearing(hearingId, cjscppuid, context) {
    var result = await getResultFromCache(hearingId);
    if (result == null) {

        context.log(`Hearing ${hearingId} not found in cache`);

        if (cjscppuid) {
            context.log(`Getting hearing result for ${hearingId}`);
            result = await getHearingResult(hearingId, cjscppuid, context);
        } else {
            throw `Hearing ${hearingId} not found in cache and no CJSCPPUID supplied`;
        }

    }

    context.log(`Hearing ${hearingId} retrieved from cache`);

    return result;
}

module.exports = async function (context) {

    const hearingId = context.bindings.params.hearingId;
    const cjscppuid = context.bindings.params.cjscppuid;
    redisClient = context.bindings.params.redisClient;

    return await getHearing(hearingId, cjscppuid, context);
};
