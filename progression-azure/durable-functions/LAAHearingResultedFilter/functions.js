const {default: axiosStatic} = require('axios');
const _ = require('lodash');

const LAA_APPLICATION_REFERENCE_KEY = 'laaApplnReference';

const laaFilter = async function (json, context) {

    if (!json.hearing) {
        context.log(`Hearing element not found when attempting to filter hearing JSON`)
        return null;
    }

    let defendantIdWithLaaReference = [];

    defendantIdWithLaaReference = (json.hearing.prosecutionCases.map((value) => {
        return (value.defendants.filter((value) => {
            return value.offences.filter(function (element) {
                return LAA_APPLICATION_REFERENCE_KEY in element;
            }).length > 0;
        }))
    }));

    defendantIdWithLaaReference = [].concat.apply([], defendantIdWithLaaReference).map(defendant => defendant.id);
    context.log(`Found ${JSON.stringify(defendantIdWithLaaReference)} defendants with LAA reference against one or more offences`)
    if (defendantIdWithLaaReference.length > 0) {
        json.hearing.prosecutionCases = json.hearing.prosecutionCases.map(prosecutionCase => {
            prosecutionCase.defendants = prosecutionCase.defendants.filter((value) => {
                return defendantIdWithLaaReference.includes(value.id);
            });
            return prosecutionCase;
        });
        return json;
    } else {
        for (const prosecutionCase of json.hearing.prosecutionCases) {
            defendantIdWithLaaReference = [];
            if (prosecutionCase.prosecutionCaseIdentifier) {
                const queriedCase = await queryUnifiedSearchForUrn(context, prosecutionCase.prosecutionCaseIdentifier.caseURN,
                                                                   context.bindings.params.cjscppuid);
                if (queriedCase && queriedCase.defendantSummary) {
                    defendantIdWithLaaReference = queriedCase.defendantSummary.filter(defendantSum => {
                        return defendantSum.offenceSummary && defendantSum.offenceSummary.filter(offenceSum => {
                            return Object.keys(offenceSum.laaApplnReference).length > 0;
                        }).length > 0;
                    }).map(defendant => defendant.defendantId);
                }
            }
        }
        if (defendantIdWithLaaReference.length > 0) {
            context.log(`Queried Unified Search, found ${JSON.stringify(defendantIdWithLaaReference)} defendants with LAA reference against one or more offences`)
            json.hearing.prosecutionCases = json.hearing.prosecutionCases.map(prosecutionCase => {
                prosecutionCase.defendants = prosecutionCase.defendants.filter((value) => {
                    return defendantIdWithLaaReference.includes(value.id);
                });
                return prosecutionCase;
            });
        }
        return defendantIdWithLaaReference.length > 0 ? json : {};
    }

};

async function queryUnifiedSearchForUrn(context, urn, cjscppuid) {
    context.log(`Querying unified search ${urn}`);
    const resultsEndpoint = process.env.UNIFIED_SEARCH_CONTEXT +
                            `/unifiedsearchquery-query-api/query/api/rest/unifiedsearchquery/laa-cases?prosecutionCaseReference=${urn}`;

    try {
        const response = await axiosStatic.get(resultsEndpoint, {
            headers: {
                'CJSCPPUID': process.env.CJSCPPUID,
                'Accept': 'application/vnd.unifiedsearch.query.laa.cases+json'
            }
        });

        if (isNotEmpty(response.data) && response.data.cases.length > 0) {
            return response.data.cases[0];
        }
    } catch (err) {
        context.log("Exception querying unified search context " + JSON.stringify(err));
        return null;
    }
}

function isNotEmpty(data) {
    return data && Object.keys(data).length > 0;
}

exports.laaFilter = laaFilter;
