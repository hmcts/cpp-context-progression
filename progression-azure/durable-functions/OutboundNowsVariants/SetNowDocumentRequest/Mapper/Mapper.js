const _ = require('lodash');
const {Result, ResultPrompt, NowText, ParentGuardian} = require('../Model/NowContent');

class Mapper {

    constructor(nowVariant, hearingJson) {
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    getDefenceOrganisation() {
        const defendantFromHearingJson = _(this.hearingJson.prosecutionCases).flatMapDeep('defendants').value()
            .find(defendant => defendant.masterDefendantId
                               === this.nowVariant.masterDefendantId);

        return defendantFromHearingJson.defenceOrganisation;
    }

    getParentGuardianDetails() {
        const parentGuardian = new ParentGuardian();
        const defendant = this.getDefendant();
        const person = this.findParentOrGuardian(defendant.associatedPersons);
        if(person){
            parentGuardian.name = [person.firstName, person.middleName,person.lastName].filter(item => item).join(' ').trim();

            if(person.address) {
                parentGuardian.address1 = person.address.address1;
                parentGuardian.address2 = person.address.address2;
                parentGuardian.address3 = person.address.address3;
                parentGuardian.address4 = person.address.address4;
                parentGuardian.address5 = person.address.address5;
                parentGuardian.postCode = person.address.postcode;
            }

            if(person.contact) {
                parentGuardian.emailAddress1 = person.contact.primaryEmail;
                parentGuardian.emailAddress2 = person.contact.secondaryEmail;
            }

            return parentGuardian;
        }
        return undefined;
    }

    findParentOrGuardian(associatedPersons) {
        const parent = "parent";
        const guardian = "guardian";
        let person = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role === parent && associatedPerson.person);
        if(!person){
            person = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role === guardian && associatedPerson.person);
        }
        return person && person.person;
    }

    getProsecutionCase() {
        //Note: what happens when hearing has multiple prosecution cases
        //which prosecutionAuthority details should be set.
        return _(this.hearingJson.prosecutionCases)
            .find(pc => pc.id === this.nowVariant.registerDefendant.cases[0]); //find the first prosecution case
    }

    getDefendant() {
        return _(this.hearingJson.prosecutionCases).flatMapDeep('defendants').value()
            .find(defendant => defendant.masterDefendantId
                               === this.nowVariant.masterDefendantId); //find the first defendant
    }

    getApplicantDetails() {
        if (this.hearingJson.courtApplications) {
            const applicants = _(this.hearingJson.courtApplications).flatMap('applicant').value();
            if(applicants) {
                return applicants.filter(applicant => !!applicant.defendant)
                    .filter(applicant => applicant.defendant.masterDefendantId === this.nowVariant.masterDefendantId);
            }
        }
    }

    getRespondentDetails() {
        if (this.hearingJson.courtApplications) {
            const respondents = _(this.hearingJson.courtApplications).flatMap('respondents').value();

            if(respondents) {
                return respondents.filter(respondent => !!respondent && !!respondent.partyDetails.defendant)
                    .filter(respondent => respondent.partyDetails.defendant.masterDefendantId === this.nowVariant.masterDefendantId);
            }
        }
    }

    getThirdParties() {
        if (this.hearingJson.courtApplications) {
            const thirdParties = _(this.hearingJson.courtApplications).flatMap('thirdParties').value();

            if(thirdParties) {
                return thirdParties.filter(thirdParty => !!thirdParty && !!thirdParty.partyDetails.defendant)
                    .filter(thirdParty => thirdParty.partyDetails.defendant.masterDefendantId === this.nowVariant.masterDefendantId);
            }
        }
    }

    getParentGuardian(defendant) {
        if (defendant.associatedPersons && defendant.associatedPersons.length) {
            const associatedPersons = defendant.associatedPersons;
            const parent = "parent";
            const guardian = "guardian";
            const parentGuardian = "ParentGuardian";

            let parentPerson = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role.toLocaleLowerCase() === parent.toLocaleLowerCase() && associatedPerson.person);
            let guardianPerson = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role.toLocaleLowerCase() === guardian.toLocaleLowerCase() && associatedPerson.person);
            let parentGuardianPerson = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role.toLocaleLowerCase() === parentGuardian.toLocaleLowerCase() && associatedPerson.person);

            if(parentPerson) {
                return parentPerson && parentPerson.person;
            }

            if(guardianPerson) {
                return guardianPerson && guardianPerson.person;
            }

            if(parentGuardianPerson) {
                return parentGuardianPerson && parentGuardianPerson.person;
            }
        }
    }

    extractFlattenNowRequirements(nowRequirements, flattenNowRequirements) {
        nowRequirements.forEach(nowRequirement => {
            if (nowRequirement.nowRequirements) {
                this.extractFlattenNowRequirements(nowRequirement.nowRequirements,
                                                   flattenNowRequirements);
            }
            flattenNowRequirements.push(nowRequirement);
        });
    }

    getNowRequirementTextForResult(defendantResult, flattenNowRequirements) {
        const nowRequirementText = [];
        flattenNowRequirements.forEach(nowRequirement => {
            if (nowRequirement.resultDefinitionId === defendantResult.judicialResultTypeId
                && nowRequirement.nowRequirementText && nowRequirement.nowRequirementText.length) {
                nowRequirement.nowRequirementText.forEach(nowReqText => {
                    const nowText = new NowText();
                    nowText.label = nowReqText.nowReference;
                    nowText.value = nowReqText.text;
                    nowText.welshValue = nowReqText.welshText;

                    nowRequirementText.push(nowText);
                });
                return;
            }

            if (defendantResult.judicialResultPrompts) {
                defendantResult.judicialResultPrompts.forEach(judicialPrompt => {
                    if (judicialPrompt.judicialResultPromptTypeId
                        === nowRequirement.resultDefinitionId
                        && nowRequirement.nowRequirementText
                        && nowRequirement.nowRequirementText.length) {
                        nowRequirement.nowRequirementText.forEach(nowReqText => {
                            const nowText = new NowText();
                            nowText.label = nowReqText.nowReference;
                            nowText.value = nowReqText.text;
                            nowText.welshValue = nowReqText.welshText;

                            nowRequirementText.push(nowText);
                        });
                    }
                });
            }

        });

        if (nowRequirementText.length) {
            return nowRequirementText;
        }
    }

    getFilterDefendantResults() {
        let defendantResults = [];
        const allDefendantResults = this.nowVariant.results.filter(result => result.level === 'D');

        if (allDefendantResults.length) {
            defendantResults = this.enrichResultWithPrompts(allDefendantResults);
        }
        if (defendantResults.length) {
            return defendantResults;
        }
    }

    getOffenceResults(offence) {
        let defendantResults = [];
        const results = this.nowVariant.results.filter(result => result.level === 'O');
        const filteredResults = this.offenceResults(offence, results);
        if (filteredResults && filteredResults.length) {
            defendantResults = this.enrichResultWithPrompts(filteredResults);
        }
        return defendantResults;
    }

    getDefendantCaseResults(prosecutionCaseJson) {
        let defendantCaseResults = [];
        let filteredResults = [];

        const results = this.nowVariant.results.filter(result => result.level === 'C');

        prosecutionCaseJson.defendants.forEach(defendant => {
            if (this.nowVariant.masterDefendantId === defendant.masterDefendantId) {
                const resultsArray = this.defendantCaseResults(defendant, results);
                if(resultsArray && resultsArray.length) {
                    resultsArray.forEach(r => filteredResults.push(r));
                }
            }
        });

        if (filteredResults && filteredResults.length) {
            defendantCaseResults = this.enrichResultWithPrompts(filteredResults);
        }

        if (defendantCaseResults.length) {
            return defendantCaseResults;
        }
    }

    offenceResults(offence, results) {
        if (offence.judicialResults) {
            const judicialResults = [];
            offence.judicialResults.forEach(jr => {
                const filterResults = results.filter(result => result.judicialResultId === jr.judicialResultId);
                if(filterResults && filterResults.length) {
                    filterResults.forEach(r => judicialResults.push(r));
                }
            });
            return judicialResults;
        }
    }

    defendantCaseResults(defendant, results) {
        if (defendant.defendantCaseJudicialResults || defendant.judicialResults) {
            const defendantCaseJudicialResults = [];
            if (defendant.defendantCaseJudicialResults) {
                defendant.defendantCaseJudicialResults.forEach(jr => {
                    const filterResults = results.filter(result => result.judicialResultId === jr.judicialResultId);
                    if(filterResults && filterResults.length) {
                        filterResults.forEach(r => defendantCaseJudicialResults.push(r));
                    }
                });
                return defendantCaseJudicialResults;
            }

            if (defendant.judicialResults) {
                const judicialResults = [];
                defendant.judicialResults.forEach(jr => {
                    const filterResults = results.filter(result => result.judicialResultId === jr.judicialResultId);
                    if(filterResults && filterResults.length) {
                        filterResults.forEach(r => defendantCaseJudicialResults.push(r));
                    }
                });
                return judicialResults;
            }
        }
    }

    enrichResultWithPrompts(allDefendantResults) {
        const defendantResults = [];

        const originalNowRequirements = this.nowVariant.now.nowRequirements;
        const flattenNowRequirements = [];
        this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);

        allDefendantResults.forEach(defendantResult => {
            const result = new Result();
            result.label = defendantResult.label;
            result.welshLabel = defendantResult.welshLabel;
            result.resultIdentifier = defendantResult.judicialResultTypeId;
            result.publishedForNows = defendantResult.publishedForNows;
            result.nowRequirementText = this.getNowRequirementTextForResult(defendantResult, flattenNowRequirements);
            result.prompts = this.getResultPrompts(defendantResult);
            result.resultWording = defendantResult.resultWording;
            result.welshResultWording = defendantResult.welshResultWording;
            result.resultDefinitionGroup = defendantResult.resultDefinitionGroup;
            defendantResults.push(result);
        });

        if (defendantResults.length) {
            return defendantResults;
        }
    }

    getResultPrompts(defendantResult) {
        const resultPrompts = [];
        if (defendantResult.judicialResultPrompts) {
            defendantResult.judicialResultPrompts.forEach(judicialPrompt => {
                const resultPrompt = new ResultPrompt();
                resultPrompt.label = judicialPrompt.label;
                resultPrompt.value = this.getEnglishValue(judicialPrompt);
                resultPrompt.welshLabel = judicialPrompt.welshLabel;
                resultPrompt.welshValue = this.getWelshValue(judicialPrompt);
                resultPrompt.promptIdentifier = judicialPrompt.judicialResultPromptTypeId;
                resultPrompt.promptReference = judicialPrompt.promptReference;
                resultPrompts.push(resultPrompt);
            });
        }

        if (resultPrompts.length) {
            return resultPrompts;
        }
    }

    getEnglishValue(judicialPrompt) {
        if (judicialPrompt.type && judicialPrompt.type.toUpperCase() === 'BOOLEAN' && judicialPrompt.value) {
            const booleanValue = judicialPrompt.value.toLowerCase();
            return booleanValue === 'true' ? 'Yes' :
                   booleanValue === 'false' ? 'No' :
                   judicialPrompt.value;
        }
        return judicialPrompt.value;
    }

    getWelshValue(judicialPrompt) {
        if (judicialPrompt.type && judicialPrompt.type.toUpperCase() === 'BOOLEAN' && judicialPrompt.value) {
            const booleanValue = judicialPrompt.value.toLowerCase();
            return booleanValue === 'true' ? 'Ydw' :
                   booleanValue === 'false' ? 'Na' :
                   judicialPrompt.welshValue;
        }
        return judicialPrompt.welshValue;
    }

    getPromptValueByReference(allPromptsFromJudicialResults, promptReference) {
        const matchingPromptReference = allPromptsFromJudicialResults && allPromptsFromJudicialResults.find(
            resultPrompt => resultPrompt.promptReference === promptReference
        );
        return matchingPromptReference && matchingPromptReference.value;
    }

    getFullNameOfPerson(person) {
        if (person && person.personDetails) {
            const personDetails = person.personDetails;
            return [personDetails.firstName, personDetails.middleName,
                    personDetails.lastName].filter(item => item).join(' ').trim();
        }
    }

    getReference(prosecutionCaseJson) {
        if (prosecutionCaseJson.prosecutionCaseIdentifier.caseURN) {
            return prosecutionCaseJson.prosecutionCaseIdentifier.caseURN;
        } else if (prosecutionCaseJson.prosecutionCaseIdentifier.prosecutionAuthorityReference) {
            return prosecutionCaseJson.prosecutionCaseIdentifier.prosecutionAuthorityReference
        }
    }
}

module.exports = Mapper;
