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

const {default: axiosStatic} = require('axios');

var redisClient = null;

async function getPrefixHearing(resultsEndpoint, contentType, cjscppuid, context) {
    context.log(`Querying ${resultsEndpoint}`);

    try {
        const response = await axiosStatic.get(resultsEndpoint, {
            headers: {
                'CJSCPPUID': cjscppuid,
                'Accept': contentType
            }
        });

        const on = new Date().toISOString().slice(0, 10);

        context.log(`response.data for ${on} -- ${response.data}`);

        if (isNotEmpty(response.data)) {
            return response.data;
        }
    } catch (err) {
        return null;
    }
}

async function getHearingResult(hearingId, cjscppuid, context, payloadPrefix) {
    const externalResultsEndpoint = process.env.RESULTS_CONTEXT_API_BASE_URI +
                            `/results-query-api/query/api/rest/results/hearingDetails/${hearingId}`;

    const internalResultsEndpoint = process.env.RESULTS_CONTEXT_API_BASE_URI +
                            `/results-query-api/query/api/rest/results/hearingDetails/internal/${hearingId}`;

    const internalContentType = 'application/vnd.results.hearing-details-internal+json';

    const externalContentType = 'application/vnd.results.hearing-information-details+json';

    if(payloadPrefix === 'EXT_') {
        return await getPrefixHearing(externalResultsEndpoint, externalContentType, cjscppuid, context);
    }

    if(payloadPrefix === 'INT_') {
        return await getPrefixHearing(internalResultsEndpoint, internalContentType, cjscppuid, context);
    }
}

function isNotEmpty(data) {
    return data && Object.keys(data).length > 0;
}

function getCacheKey(hearingId, payloadPrefix) {
    return payloadPrefix + hearingId + '_result_';
}

function getRedisClient() {

    if (redisClient == null) {
        const {createClient} = require('redis');
        redisClient = createClient(process.env.REDIS_PORT, process.env.REDIS_HOST,
                                   {
                                       auth_pass: process.env.REDIS_KEY,
                                       tls: {servername: process.env.REDIS_HOST}
                                   });
    }

    return redisClient;
}

async function getResultFromCache(hearingId, payloadPrefix) {
    const {promisify} = require('util');
    const cacheKey = getCacheKey(hearingId, payloadPrefix);
    const client = getRedisClient();
    const getAsync = promisify(client.get).bind(client);
    const cachedResult = await getAsync(cacheKey);

    if (cachedResult == null) {
        return null;
    }

    return JSON.parse(cachedResult);
}

async function getHearing(hearingId, cjscppuid, payloadPrefix, context) {
    context.log('1. About to query redis cache -->'+hearingId);
    let result = await getResultFromCache(hearingId, payloadPrefix);
    context.log('2. redis cache queried');

    if (result == null) {

        context.log(`3.1 Hearing ${hearingId} not found in cache`);

        if (cjscppuid && hearingId) {
            context.log(`4. Getting hearing result for ${hearingId}`);
            result = await getHearingResult(hearingId, cjscppuid, context, payloadPrefix);
            context.log('5. Queried result context for hearing id -->'+hearingId +" "+ JSON.stringify(result));
        } else {
            context.log(`3.3 Hearing ${hearingId} not found in cache and no CJSCPPUID supplied`);
        }

    } else {
        context.log(`3.2 Hearing ${hearingId} retrieved from cache`);
    }

    return result;
}

module.exports = async function (context) {

    const hearingId = context.bindings.params.hearingId;
    const cjscppuid = context.bindings.params.cjscppuid;
    const payloadPrefix = context.bindings.params.payloadPrefix;

    redisClient = context.bindings.params.redisClient;

    return await getHearing(hearingId, cjscppuid, payloadPrefix, context);
};
