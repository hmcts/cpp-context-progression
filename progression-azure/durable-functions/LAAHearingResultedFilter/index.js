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
const {laaFilter} = require('./functions.js');

module.exports = async function (context) {

    var json = context.bindings.unfilteredJson;

    if (!json || !json.hearing) {
        throw 'Invalid hearing JSON';
    }

    context.log(`Ready to filter JSON`)
    var result = laaFilter(json, context);

    if (result == null) {
        throw 'Unable to filter JSON - incorrect JSON structure';
    }

    context.log(`JSON filtered successfully`)

    return result

};