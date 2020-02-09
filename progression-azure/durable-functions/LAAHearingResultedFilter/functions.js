const LAA_APPLICATION_REFERENCE_KEY = 'laaApplnReference';

const laaFilter = function(json, context) {

    if (!json) {
        return null;
    }

    json.prosecutionCases = json.prosecutionCases.map((value)=> {

        value.defendants = value.defendants.filter((value) => {

            const offenceCount = value.offences.filter(function(element) {
                return LAA_APPLICATION_REFERENCE_KEY in element;
            }).length;

            return offenceCount > 0;

        });

        return value;
    });

    return json;

};

exports.laaFilter = laaFilter;
