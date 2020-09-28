const Mapper = require('../Mapper');
const _ = require('lodash');
const {Result, ResultPrompt,NowContent, NowText} = require('../../Model/NowContent');
const NowDefendantMapper = require('../NowContent/NowDefendant/NowDefendantMapper');
const ApplicantMapper = require('./Applicant/ApplicantMapper');
const RespondentMapper = require('./Respondent/RespondentMapper');
const FinancialOrderDetailsMapper = require('../NowContent/FinancialOrderDetails/FinancialOrderDetailsMapper');
const NextHearingCourtDetailsMapper = require('../NowContent/NextHearingCourtDetails/NextHearingCourtDetailsMapper');
const CaseMapper = require('../NowContent/Case/CaseMapper');
const OrderAddresseeMapper = require('../NowContent/OrderAddressee/OrderAddresseeMapper');
const OrderCourtMapper = require('../NowContent/OrderCourt/OrderCourtMapper');
const ThirdPartyMapper = require('../NowContent/ThirdParty/ThirdPartyMapper');
const ParentGuardianMapper = require('../NowContent/ParentGuardian/ParentGuardianMapper');

class NowContentMapper extends Mapper {
    constructor(requestId, nowVariant, hearingJson,
                organisationUnitsRefData, enforcementAreaByLjaCode, enforcementAreaByPostCodeMap,
                context) {
        super(nowVariant, hearingJson);
        this.requestId = requestId;
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
        this.organisationUnitsRefData = organisationUnitsRefData;
        this.enforcementAreaByLjaCode = enforcementAreaByLjaCode;
        this.enforcementAreaByPostCodeMap = enforcementAreaByPostCodeMap;
        this.context = context;
    }

    buildNowContent() {
        const now = this.nowVariant.now;
        const defendantContextBase = this.nowVariant.registerDefendant;
        const nowContent = new NowContent();
        nowContent.orderName = this.nowVariant.now.name;
        nowContent.welshOrderName = this.nowVariant.now.welshName;
        nowContent.orderDate = this.nowVariant.orderDate;
        nowContent.amendmentDate = this.nowVariant.amendmentDate;
        nowContent.courtClerkName = this.courtClerkName();
        nowContent.civilNow = false;
        nowContent.caseApplicationReferences = this.getCaseApplicationReferences(defendantContextBase);
        nowContent.orderAddressee = this.getOrderAddresseeMapper();
        nowContent.defendant = this.getDefendantMapper();
        nowContent.financialOrderDetails = this.requestId ? this.getFinancialOrderDetailsMapper() : undefined;
        nowContent.nextHearingCourtDetails = this.getNextHearingCourtDetailsMapper();
        nowContent.nowText = this.getNowTextList(now);
        nowContent.cases = this.getCaseMapper();
        nowContent.orderingCourt = this.getOrderCourtMapper();
        nowContent.distinctResults = this.getDistinctResults(now);
        nowContent.distinctPrompts = this.getDistinctPrompts(now);
        nowContent.nowRequirementText = this.getNowRequirementText(nowContent);
        nowContent.applicants = this.getApplicantMapper();
        nowContent.respondents = this.getRespondentMapper();
        nowContent.thirdParties = this.getThirdPartyMapper();
        nowContent.parentGuardian = this.getParentGuardianMapper();
        return nowContent;
    }

    courtClerkName() {
        const resultsWithCourtClerk = this.nowVariant.results.filter(result => result.courtClerk);
        if (resultsWithCourtClerk.length) {
            const firstName = resultsWithCourtClerk[0].courtClerk.firstName;
            const lastName = resultsWithCourtClerk[0].courtClerk.lastName;
            return firstName + ' ' + lastName;
        }
    }

    getNowTextList(now) {
        const nowTextList = [];

        if(now.nowTextList) {
            now.nowTextList.forEach(nowTextFromMetadata => {
                const nowText = new NowText();
                nowText.label = nowTextFromMetadata.nowReference;
                nowText.value = nowTextFromMetadata.text;
                nowText.welshValue = nowTextFromMetadata.welshText;
                nowTextList.push(nowText);
            });

            if (nowTextList.length) {
                return nowTextList;
            }
        }
    }

    getCaseApplicationReferences(defendantContextBase) {
        const applicationReferences = [];

        const caseIds = Array.from(defendantContextBase.cases);

        caseIds.forEach(caseId => {
            const prosecutionCase = this.getProsecutionCase(caseId);
            const reference = this.getReference(prosecutionCase);
            applicationReferences.push(reference);
        })

        const applicationIds = Array.from(defendantContextBase.applications);

        applicationIds.forEach(applicationId => {
            const application = this.getCourtApplication(applicationId);
            if (application.applicationReference) {
                applicationReferences.push(application.applicationReference);
            }
        });

        if(applicationReferences.length) {
            return applicationReferences;
        }
    }

    getProsecutionCase(caseId) {
        return _(this.hearingJson.prosecutionCases).value()
            .find(pcase => pcase.id === caseId);
    }

    getCourtApplication(applicationId) {
        return _(this.hearingJson.courtApplications).value()
            .find(application => application.id === applicationId);
    }

    getDefendantMapper() {
        return new NowDefendantMapper(this.nowVariant, this.hearingJson).buildDefendant();
    }

    getApplicantMapper() {
        return new ApplicantMapper(this.nowVariant, this.hearingJson).buildApplicants();
    }

    getRespondentMapper() {
        return new RespondentMapper(this.nowVariant, this.hearingJson).buildRespondents();
    }

    getThirdPartyMapper() {
        return new ThirdPartyMapper(this.nowVariant, this.hearingJson).buildThirdParties();
    }

    getParentGuardianMapper() {
        return new ParentGuardianMapper(this.nowVariant, this.hearingJson).buildParentGuardian();
    }

    getFinancialOrderDetailsMapper() {
        return new FinancialOrderDetailsMapper(this.nowVariant, this.hearingJson,
                                               this.organisationUnitsRefData,
                                               this.enforcementAreaByLjaCode,
                                               this.enforcementAreaByPostCodeMap,
                                               this.context).buildFinancialOrderDetails();
    }

    getNextHearingCourtDetailsMapper() {
        return new NextHearingCourtDetailsMapper(this.nowVariant,
                                                 this.hearingJson).buildNextHearingDetails();
    }

    getCaseMapper() {
        return new CaseMapper(this.nowVariant, this.hearingJson).buildProsecutionCases();
    }

    getOrderAddresseeMapper() {
        if(this.nowVariant.matchedSubscription.recipient){
            return new OrderAddresseeMapper(this.nowVariant, this.hearingJson, this.context).buildOrderAddressee();
        }
    }

    getOrderCourtMapper() {
        return new OrderCourtMapper(this.nowVariant, this.hearingJson).buildOrderCourt();
    }

    getDistinctResults(now) {
        this.context.log('now == ' + now.name + ' (' + now.id + ')');
        const originalNowRequirements = now.nowRequirements;
        const flattenNowRequirements = [];
        this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);
        const resultDefinitionIdsWithDistinctFlagTrue = this.getResultDefinitionIdsWithDistinctFlagTrue(flattenNowRequirements);
        this.context.log('Distinct Results in the NOWs are ' + JSON.stringify(resultDefinitionIdsWithDistinctFlagTrue));
        if (resultDefinitionIdsWithDistinctFlagTrue) {
            const distinctResults = [];
            resultDefinitionIdsWithDistinctFlagTrue.forEach(resultDefinitionId => {
                const results = this.nowVariant.results.filter(result => result.judicialResultTypeId === resultDefinitionId);
                if(results.length) {
                    const result = new Result();
                    result.label = results[0].label;
                    result.welshLabel = results[0].welshLabel;
                    result.resultIdentifier = results[0].judicialResultTypeId;
                    result.publishedForNows = results[0].publishedForNows;
                    result.resultWording = results[0].resultWording;
                    result.welshResultWording = results[0].welshResultWording;
                    result.sequence = results[0].sequence;
                    result.nowRequirementText = this.getNowRequirementTextForResult(results[0], flattenNowRequirements);
                    if (results[0].judicialResultPrompts) {
                        result.prompts = this.getResultPrompts(results[0]);
                    }
                    result.resultDefinitionGroup = results[0].resultDefinitionGroup;
                    distinctResults.push(result);
                }
            });

            if(distinctResults.length) {
                return distinctResults.sort((a, b) => a.sequence - b.sequence);
            }
        }
    }

    getResultDefinitionIdsWithDistinctFlagTrue(flattenRequirements) {
        const idsWithDistinctFlagTrue = new Set();
        flattenRequirements.filter(requirement => requirement.distinctResultTypes)
            .forEach(requirement => {
                idsWithDistinctFlagTrue.add(requirement.resultDefinitionId);
            });
        return [...idsWithDistinctFlagTrue];
    }

    getDistinctPrompts(now) {
        const originalNowRequirements = now.nowRequirements;
        const flattenNowRequirements = [];
        this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);

        const promptIdsWithDistinctFlagTrue = this.getPromptIdsWithDistinctFlagTrue(flattenNowRequirements);
        this.context.log('Distinct Prompts in the NOWs are ' + JSON.stringify(promptIdsWithDistinctFlagTrue));

        const uniquePromptIdsFromResults = this.getUniquePromptIdsFromResults(this.nowVariant.results);

        const distinctPromptIds = uniquePromptIdsFromResults.filter(element => promptIdsWithDistinctFlagTrue.includes(element));
        this.context.log('distinct promptIds found in the judicial results are ' + JSON.stringify(distinctPromptIds));

        const distinctResultPrompts = [];

        if (distinctPromptIds.length) {

            distinctPromptIds.forEach(distinctPromptId => {

                const prompts = this.getPrompts(distinctPromptId, this.nowVariant.results);

                const distinctPrompts = this.getDistinctPromptsValues(prompts);

                distinctPrompts.forEach(distinctPrompt => {
                    const resultPrompt = new ResultPrompt();
                    resultPrompt.label = distinctPrompt.label;
                    resultPrompt.value = this.getEnglishValue(distinctPrompt);
                    resultPrompt.promptIdentifier = distinctPrompt.judicialResultPromptTypeId;

                    if (distinctPrompt.promptReference) {
                        resultPrompt.promptReference = distinctPrompt.promptReference;
                    }

                    if (distinctPrompt.welshLabel) {
                        resultPrompt.welshLabel = distinctPrompt.welshLabel;
                    }

                    if (distinctPrompt.welshValue) {
                        resultPrompt.welshValue = this.getWelshValue(distinctPrompt);
                    }

                    distinctResultPrompts.push(resultPrompt);
                })
            });

            if(distinctResultPrompts.length) {
                return distinctResultPrompts;
            }
        }
    }

    getPromptIdsWithDistinctFlagTrue(flattenRequirements) {
        const idsWithDistinctFlagTrue = new Set();
        flattenRequirements.filter(requirement => requirement.prompts)
            .forEach(requirement => {
                requirement.prompts.filter(prompt => prompt.distinctPromptTypes)
                    .forEach(prompt => {
                        idsWithDistinctFlagTrue.add(prompt.promptId);
                    });
            });
        return [...idsWithDistinctFlagTrue];
    }

    getNowRequirementText(nowContent) {
        const allNowRequirementText = [];
        const nowRequirementLabel = [];
        if(nowContent.defendant.defendantResults) {
            nowContent.defendant.defendantResults.forEach(defendantResult => {
                if(defendantResult.nowRequirementText && defendantResult.nowRequirementText.length) {
                    defendantResult.nowRequirementText.forEach(text => {
                        if(!nowRequirementLabel.includes(text.label)) {
                            allNowRequirementText.push(text);
                            nowRequirementLabel.push(text.label);
                        }
                    });
                }
            });
        }
        nowContent.cases.forEach(caze => {
            if(caze.defendantCaseResults) {
                caze.defendantCaseResults.forEach(defendantCaseResult => {
                    if(defendantCaseResult.nowRequirementText && defendantCaseResult.nowRequirementText.length) {
                        defendantCaseResult.nowRequirementText.forEach(text => {
                            if(!nowRequirementLabel.includes(text.label)) {
                                allNowRequirementText.push(text);
                                nowRequirementLabel.push(text.label);
                            }
                        });
                    }
                });
            }

            if(caze.defendantCaseOffences) {
                caze.defendantCaseOffences.forEach(defendantCaseOffence => {
                    if(defendantCaseOffence.results) {
                        defendantCaseOffence.results.forEach(result => {
                            if(result.nowRequirementText && result.nowRequirementText.length) {
                                result.nowRequirementText.forEach(text => {
                                    if(!nowRequirementLabel.includes(text.label)) {
                                        allNowRequirementText.push(text);
                                        nowRequirementLabel.push(text.label);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        if(allNowRequirementText.length) {
            return allNowRequirementText;
        }
    }

    getUniquePromptIdsFromResults(results) {
        const uniquePromptIds = new Set();
        results.forEach(result => {
            if(result.judicialResultPrompts && result.judicialResultPrompts.length) {
                result.judicialResultPrompts.forEach(prompt => {
                    uniquePromptIds.add(prompt.judicialResultPromptTypeId);
                })
            }
        })
        return [...uniquePromptIds];
    }

    getPrompts(distinctPromptId, results) {
        let prompts = [];
        results.forEach(result => {
            if(result.judicialResultPrompts && result.judicialResultPrompts.length) {
                const resultPrompts = result.judicialResultPrompts.filter(prompt => prompt.judicialResultPromptTypeId === distinctPromptId);
                prompts = prompts.concat(resultPrompts);
            }
        });
        return prompts;
    }

    getDistinctPromptsValues(prompts) {
        const promptValues = new Set();
        const distinctPrompts = [];
        prompts.forEach(prompt => {
            if(!promptValues.has(prompt.value)) {
                distinctPrompts.push(prompt);
                promptValues.add(prompt.value);
            }
        });
        return distinctPrompts;
    }
}

module.exports = NowContentMapper;
