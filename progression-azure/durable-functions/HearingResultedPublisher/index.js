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

function filterOnSubscriberKey(subscriberKey, subscriberJson) {
    var filteredSubscribers = subscriberJson.filter(function(element) {
        return element.subscriber_key == subscriberKey;
    });
    return filteredSubscribers;
}

function publish(endpoint, publishPayload, context) {

    context.log(`Publishing to ${endpoint}`);
    const response = axiosStatic.post(endpoint, publishPayload,
        publishPayload, {
        headers: {
            'Ocp-Apim-Subscription-Key': process.env.LAA_PUBLISH_RESULTS_API_KEY
        }})
    .then(resp => {
        context.log('Response received');
    })
    .catch(err => {throw err})

    return response

}

module.exports = async function (context) {

    const publishPayload = context.bindings.filteredJson;

    context.log(`Preparing to publish to LAA endpoints`)

    if (!publishPayload || Object.keys(publishPayload).length == 0) {
        context.log(`Not publishing empty payload`)
        return {published_to: []}
    }

    const subscriberJson = context.bindings.subscriberJson;

    const laaSubscribers = filterOnSubscriberKey('LAA', subscriberJson);

    laaSubscribers.forEach(function(laaSubscriber) {
        laaSubscriber.result = null
        try {
            const result = publish(laaSubscriber.subscriber_endpoint_uri, publishPayload, context);
            laaSubscriber.result = result
            context.log(`Publish success to ${laaSubscriber.subscriber_endpoint_uri}`);
        } catch (err) {
            context.log(`Failed to publish to ${laaSubscriber.subscriber_endpoint_uri} - error ${err}`);
        }
    });

    return {published_to: laaSubscribers};
};
