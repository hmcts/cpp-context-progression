const Mapper = require('../../Mapper');
const {Case, DefendantCaseOffence} = require('../../../Model/NowContent');
const _ = require('lodash');

class CaseMapper extends Mapper {

    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildProsecutionCases() {
        const cases = [];
        const defendantContextBase = this.nowVariant.registerDefendant;
        const caseIds = Array.from(defendantContextBase.cases);
        const applicationIds = Array.from(defendantContextBase.applications);
        caseIds.forEach(caseId => {
            const prosecutionCaseJson = this.getProsecutionCase(caseId);
            const prosecutionCase = new Case();
            prosecutionCase.reference = this.caseReference(prosecutionCaseJson);
            prosecutionCase.caseMarkers = this.caseMarkers(prosecutionCaseJson);
            prosecutionCase.defendantCaseResults = this.getDefendantCaseResults(prosecutionCaseJson);
            prosecutionCase.defendantCaseOffences = this.getDefendantCaseOffences(prosecutionCaseJson);
            prosecutionCase.prosecutor = prosecutionCaseJson.prosecutionCaseIdentifier.prosecutionAuthorityName;
            prosecutionCase.prosecutorOUCode = prosecutionCaseJson.prosecutionCaseIdentifier.prosecutionAuthorityOUCode;
            prosecutionCase.prosecutorWelsh = undefined;
            const courtApplication = this.getCourtApplication(applicationIds);
            if (courtApplication) {
                prosecutionCase.applicationType = courtApplication.type.applicationType;
                prosecutionCase.applicationTypeWelsh = courtApplication.type.applicationTypeWelsh;
                prosecutionCase.applicationLegislation = courtApplication.type.applicationLegislation;
                prosecutionCase.applicationLegislationWelsh = courtApplication.type.applicationLegislationWelsh;
                prosecutionCase.applicationReceivedDate = courtApplication.applicationReceivedDate;
                prosecutionCase.applicationParticulars = courtApplication.applicationParticulars;
                prosecutionCase.allegationOrComplaintStartDate = undefined; //TODO not sure where to get this
                prosecutionCase.allegationOrComplaintEndDate = undefined; //TODO not sure where to get this
                prosecutionCase.applicationCode = courtApplication.type.applicationCode;
            }
            cases.push(prosecutionCase);
        });

        if(cases.length) {
            return cases;
        }
    }

    getProsecutionCase(caseId) {
        return _(this.hearingJson.prosecutionCases).value()
            .find(pcase => pcase.id === caseId);
    }

    getCourtApplication(applicationIds) {
        if (this.hearingJson.courtApplications) {
            return _(this.hearingJson.courtApplications).value()
                .find(courtApplication => applicationIds.includes(courtApplication.id));
        }
    }

    caseReference(prosecutionCaseJson) {
        if (prosecutionCaseJson.prosecutionCaseIdentifier.caseURN) {
            return prosecutionCaseJson.prosecutionCaseIdentifier.caseURN;
        } else if (prosecutionCaseJson.prosecutionCaseIdentifier.prosecutionAuthorityReference) {
            return prosecutionCaseJson.prosecutionCaseIdentifier.prosecutionAuthorityReference
        }
    }

    caseMarkers(prosecutionCaseJson) {
        const caseMarkers = [];
        if (this.nowVariant.now.includeCaseMarkers && prosecutionCaseJson.caseMarkers) {
            prosecutionCaseJson.caseMarkers.forEach(caseMarker => caseMarkers.push(caseMarker.markerTypeCode));
        }
        if(caseMarkers.length) {
            return caseMarkers;
        }
    }

    getDefendantCaseOffences(prosecutionCaseJson) {
        const defendantCaseOffences = [];
        const now = this.nowVariant.now;

        prosecutionCaseJson.defendants.forEach(defendant => {
            if(this.nowVariant.masterDefendantId === defendant.masterDefendantId) {
                defendant.offences.forEach(offence => {
                    const results = this.getOffenceResults(offence);
                    if(results.length) {
                        const defendantCaseOffence = new DefendantCaseOffence();
                        defendantCaseOffence.wording = offence.wording + '\n'+ offence.offenceLegislation;
                        defendantCaseOffence.welshWording = this.getOffenceWelshWording(offence);
                        defendantCaseOffence.title = offence.offenceTitle;
                        defendantCaseOffence.welshTitle = offence.welshTitle ? offence.welshTitle : undefined;
                        defendantCaseOffence.legislation = offence.offenceLegislation ? offence.offenceLegislation : undefined;
                        defendantCaseOffence.civilOffence = false;
                        defendantCaseOffence.startDate = offence.startDate;
                        defendantCaseOffence.endDate =  offence.endDate ? offence.endDate : undefined;
                        defendantCaseOffence.convictionDate = offence.convictionDate ? offence.convictionDate : undefined;
                        defendantCaseOffence.results = results;
                        defendantCaseOffence.code = offence.offenceCode;
                        defendantCaseOffence.modeOfTrial = offence.modeOfTrial;
                        defendantCaseOffence.allocationDecision = offence.allocationDecision ? offence.allocationDecision.motReasonDescription : undefined;
                        defendantCaseOffence.verdictType = offence.verdict ? offence.verdict.verdictType.description : undefined;
                        if(now.includeDVLAOffenceCode) {
                            defendantCaseOffence.dvlaCode = this.getDvlaCode(offence);
                        }

                        if(now.includeConvictionStatus) {
                            defendantCaseOffence.convictionStatus = offence.convictionDate ? 'Convicted' : 'Not Convicted';
                        }

                        if(now.includePlea) {
                            defendantCaseOffence.plea = offence.plea ? offence.plea.pleaValue : undefined;
                        }

                        if(now.includeVehicleRegistration) {
                            defendantCaseOffence.vehicleRegistration = this.getVehicleRegistration(offence);
                        }
                        if (offence.offenceFacts) {
                            const offenceFacts = offence.offenceFacts;
                            defendantCaseOffence.alcoholReadingAmount = offenceFacts.alcoholReadingAmount
                            defendantCaseOffence.alcoholReadingMethodCode = offenceFacts.alcoholReadingMethodCode
                            defendantCaseOffence.alcoholReadingMethodDescription = offenceFacts.alcoholReadingMethodDescription
                        }
                        defendantCaseOffences.push(defendantCaseOffence);
                    }
                });
            }
        });

        return defendantCaseOffences;
    }

    getOffenceWelshWording(offence) {
        if(offence.wordingWelsh && offence.offenceLegislationWelsh) {
            return offence.wordingWelsh + '\n'+ offence.offenceLegislationWelsh;
        } else if(offence.wordingWelsh) {
            return offence.wordingWelsh;
        }
    }

    getDvlaCode(offence) {
        if(offence.offenceFacts && offence.offenceFacts.vehicleCode) {
            return offence.offenceFacts.vehicleCode;
        }
    }

    getVehicleRegistration(offence) {
        if(offence.offenceFacts && offence.offenceFacts.vehicleRegistration) {
            return offence.offenceFacts.vehicleRegistration;
        }
    }

    getDisplayText() {
        let resultText = undefined;
        this.nowVariant.results.forEach(result => {
            if(result.resultText) {
                resultText = result.resultText;
            }
        });

        return resultText;
    }
}

module.exports = CaseMapper;
