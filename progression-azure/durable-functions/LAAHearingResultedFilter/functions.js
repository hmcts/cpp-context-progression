const LAA_APPLICATION_REFERENCE_KEY = 'laaApplnReference';

const laaFilter = function(json, context) {

    if (!json.hearing) {
        context.log(`Hearing element not found when attempting to filter hearing JSON`)
        return null;
    }

    var defendantsFound = 0;

    json.hearing.prosecutionCases = json.hearing.prosecutionCases.map((value)=> {

        value.defendants = value.defendants.filter((value) => {

            const offenceCount = value.offences.filter(function(element) {
                return LAA_APPLICATION_REFERENCE_KEY in element;
            }).length;

            if (offenceCount > 0) {
                defendantsFound ++;
            }

            return offenceCount > 0;

        });

        return value;
    });

    context.log(`Found ${defendantsFound} defendants with LAA reference against one or more offences`)

    return defendantsFound > 0 ? json : {};

};

exports.laaFilter = laaFilter;
