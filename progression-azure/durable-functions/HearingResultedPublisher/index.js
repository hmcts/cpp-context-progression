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

function filterOnSubscriberKey(subscriberKey, subscriberJson) {
    var filteredSubscribers = subscriberJson.filter(function(element) {
        return element.subscriber_key == subscriberKey;
    });
    return filteredSubscribers;
}

async function publish(endpoint, context) {
    const { default: axiosStatic } = require('axios');

    context.log(`Publishing to ${endpoint}`);
    const response = axiosStatic.post(endpoint, {
        headers: {
        }
    }).then(response => response.data);

    return response;
}

module.exports = async function (context) {

    const subscriberJson = context.bindings.subscriberJson;

    const laaSubscribers = filterOnSubscriberKey('LAA', subscriberJson);

    laaSubscribers.forEach(function(laaSubscriber) {
        publish(laaSubscriber.subscriber_endpoint_uri, context);
    });

    return {published_to: laaSubscribers};
};
