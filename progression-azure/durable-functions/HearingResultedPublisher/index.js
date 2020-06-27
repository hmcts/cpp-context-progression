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
const { Agent } = require('http');

function filterOnSubscriberKey(subscriberKey, subscriberJson) {
    var filteredSubscribers = subscriberJson.filter(function(element) {
        return element.subscriber_key == subscriberKey;
    });
    return filteredSubscribers;
}

function getCertificates(context) {
    let certificates = []

    ca({
        format: ca.der2.pem,
        store: ['My'],
        ondata: crt => certificates.push(crt)
    })

    context.log("Certificate count under 'My' store is " + certificates.length);
    return certificates;
}

async function postWrapper(endpoint, publishPayload, certificates, context) {

    return axiosStatic.post(endpoint, publishPayload, {
        headers: {
            'Ocp-Apim-Subscription-Key': process.env.LAA_PUBLISH_RESULTS_API_KEY
        },
        httpsAgent: new https.Agent({ca: certificates})
    })
}

async function publishToAllSubscribers(laaSubscribers, publishPayload, context) {

    let certificates = getCertificates(context);

    try {
        for (i=0; i<laaSubscribers.length; i++) {
            let laaSubscriber = laaSubscribers[i]
            let endpoint = laaSubscriber.subscriber_endpoint_uri
            context.log(`Publishing to ${endpoint}`);

            const result = await postWrapper(endpoint, publishPayload, certificates, context)

            laaSubscriber.result = result
            if (result == null) {
                context.log(`Failed to publish to ${endpoint}`);
            } else {
                context.log(`Request to publish to ${endpoint} succeeded`);
            }

        }
    } catch (err) {
        context.log(`Exception caught when trying to publish to subscriber - error ${err}`);
    }
}

module.exports = async function (context) {

    const publishPayload = context.bindings.filteredJson;

    context.log(`Preparing to publish to LAA endpoints`)

    if (!publishPayload || Object.keys(publishPayload).length == 0) {
        context.log(`Not publishing empty payload`)
        return {published_to: []}
    }

    const subscriberJson = context.bindings.subscriberJson;

    let laaSubscribers = filterOnSubscriberKey('LAA', subscriberJson);

    await publishToAllSubscribers(laaSubscribers, publishPayload, context)

    return {published_to: laaSubscribers};

};
