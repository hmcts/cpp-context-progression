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
const https = require('https');
const ca = require ('win-ca');
const fs = require ('fs');
const { Agent } = require('http');

function filterOnSubscriberKey(subscriberKey, subscriberJson) {
    var filteredSubscribers = subscriberJson.filter(function(element) {
        return element.subscriber_key == subscriberKey;
    });
    return filteredSubscribers;
}

function getHttpsAgent(context) {
    let certificates = []
    context.log(`process.platform = ${process.platform}`)

    if (process.platform == 'win32') {
        context.log("Windows OS detected");
        ca({
            format: ca.der2.pem,
            store: ['My'],
            ondata: crt => certificates.push(crt)
        })
        context.log(`Certificate count under 'My' store is ${certificates.length}`);
        return new https.Agent({ca: certificates})
    } else {
        let certificateLocation = `/var/ssl/certs/${process.env.WEBSITE_LOAD_CERTIFICATES}.der`
        context.log(`Non-Windows OS detected. Looking for certificate in ${certificateLocation}`)
        fs.stat(certificateLocation, function(err, stat) {
            if (err == null) {
                console.log(`Certificate file ${certificateLocation} found. Returning HTTPS agent.`)
                return https.Agent({cert: fs.readFileSync(certificateLocation)})
            } else {
                console.log(`Certificate file ${certificateLocation} not found. Returning null as HTTPS agent.`)
                return null
            }
        })
    }
}

async function postWrapper(endpoint, publishPayload, httpsAgent, context) {

    return axiosStatic.post(endpoint, publishPayload, {
        headers: {
            'Ocp-Apim-Subscription-Key': process.env.LAA_PUBLISH_RESULTS_API_KEY
        },
        httpsAgent: httpsAgent
    })
}

async function publishToAllSubscribers(laaSubscribers, publishPayload, context) {

    let httpsAgent = getHttpsAgent(context);

    try {
        for (i=0; i<laaSubscribers.length; i++) {
            let laaSubscriber = laaSubscribers[i]
            let endpoint = laaSubscriber.subscriber_endpoint_uri
            context.log(`Publishing to ${endpoint}`);

            const result = await postWrapper(endpoint, publishPayload, httpsAgent, context)

            if (result == null) {
                context.log(`Failed to publish to ${endpoint}`);
                laaSubscriber.result = 'failed'
            } else {
                context.log(`Request to publish to ${endpoint} succeeded`);
                laaSubscriber.result = 'succeeded'
            }

        }
    } catch (err) {
        context.log(`Exception caught when trying to publish to subscriber - error ${err}`);
    }
}

module.exports = async function (context) {

    const publishPayload = context.bindings.filteredJson;

    context.log(`Preparing to publish to LAA endpoints --->> ${JSON.stringify(publishPayload)}`);

    if (!publishPayload || Object.keys(publishPayload).length == 0) {
        context.log(`Not publishing empty payload`)
        return {published_to: []}
    }

    const subscriberJson = context.bindings.subscriberJson;

    let laaSubscribers = filterOnSubscriberKey('LAA', subscriberJson);

    await publishToAllSubscribers(laaSubscribers, publishPayload, context)

    return {published_to: laaSubscribers};

};
