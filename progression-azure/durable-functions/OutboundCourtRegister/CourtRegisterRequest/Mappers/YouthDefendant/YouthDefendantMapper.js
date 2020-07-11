const YouthDefendant = require('../../Models/YouthDefendant');
const ParentGuardianMapper = require('../ParentGuardian/ParentGuardianMapper');
const AliasMapper = require('../Alias/AliasMapper');
const HearingMapper = require('../Hearing/HearingMapper');
const CounselMapper = require('../Counsel/CounselMapper');
const ResultMapper = require('../Result/ResultMapper');
const ProsecutionCaseOrApplicationMapper = require('../ProsecutionCaseOrApplication/ProsecutionCaseOrApplicationMapper');
const AddressMapper = require('../Address/AddressMapper');
const _ = require('lodash');
const LEVEL_TYPE = require('../../../../NowsHelper/service/LevelTypeEnum');
const NOT_APPLICABLE = 'Not Applicable';

class YouthDefendantMapper {

    constructor(context, registerYouthDefendants, hearingJson, courtRegisterFragment) {
        this.context = context;
        this.registerYouthDefendants = registerYouthDefendants;
        this.hearingJson = hearingJson;
        this.courtRegisterFragment = courtRegisterFragment;
    }

    build() {
        const youthDefendantList = [];

        this.registerYouthDefendants.forEach(registerDefendant => {

            // Get all the youth defendants for the masterDefendant Id
            const defendants = this.getDefendants(registerDefendant.masterDefendantId);

            // Take the first defendant
            const defendant = defendants[0];

            const defendantDetails = defendant.personDefendant.personDetails;

            const youthDefendantInfo = new YouthDefendant();
            youthDefendantInfo.name = [defendantDetails.firstName, defendantDetails.middleName, defendantDetails.lastName].filter(item => item).join(' ').trim();
            youthDefendantInfo.dateOfBirth = defendantDetails.dateOfBirth;
            youthDefendantInfo.address = this.getAddressMapper(defendantDetails.address);
            youthDefendantInfo.gender = defendantDetails.gender;
            youthDefendantInfo.postHearingCustodyStatus = this.postHearingCustodyStatus(defendant);
            youthDefendantInfo.masterDefendantId = registerDefendant.masterDefendantId;
            youthDefendantInfo.nationality = defendantDetails.nationalityDescription;
            youthDefendantInfo.ethnicity = this.ethnicity(defendantDetails);
            youthDefendantInfo.parentGuardian = this.getParentGuardianMapper(registerDefendant);
            youthDefendantInfo.hearing = this.getHearingMapper(defendant);
            youthDefendantInfo.aliases = this.getAliasMapper(defendant.aliases);
            youthDefendantInfo.prosecutionCasesOrApplications = this.getProsecutionCaseOrApplicationMapper(registerDefendant);
            youthDefendantInfo.defendantResults = this.getResultMapper(registerDefendant);
            youthDefendantInfo.defenceCounsels = this.getCounselMapper(registerDefendant);

            youthDefendantList.push(youthDefendantInfo);
        });

        return youthDefendantList;
    }

    postHearingCustodyStatus(defendant) {
        let postHearingCustodyStatus = NOT_APPLICABLE;
        if (defendant.defendantCaseJudicialResults) {
            const filteredCustodyStatuses = defendant.defendantCaseJudicialResults.filter(
                j => j.postHearingCustodyStatus !== NOT_APPLICABLE);
            if (filteredCustodyStatuses.length) {
                postHearingCustodyStatus = filteredCustodyStatuses[0].postHearingCustodyStatus;
            }
        }
        return postHearingCustodyStatus;
    }

    ethnicity(defendantDetails) {
        if (defendantDetails.ethnicity && defendantDetails.ethnicity.observedEthnicityDescription && defendantDetails.ethnicity.selfDefinedEthnicityDescription) {
            return defendantDetails.ethnicity.observedEthnicityDescription || defendantDetails.ethnicity.selfDefinedEthnicityDescription;
        }
    }

    getParentGuardianMapper(registerDefendant) {
        return new ParentGuardianMapper(registerDefendant, this.hearingJson).build();
    }

    getAliasMapper(aliases) {
        return new AliasMapper(aliases).build();
    }

    getHearingMapper(defendant) {
        return new HearingMapper(this.hearingJson, this.courtRegisterFragment, defendant).build();
    }

    getAddressMapper(addressInfo) {
        return new AddressMapper(addressInfo).build();
    }

    getCounselMapper(registerDefendant) {
        const defenceCounsels = [];
        if (this.hearingJson.defenceCounsels) {
            this.hearingJson.defenceCounsels.forEach(defenceCounsel => {
                if (defenceCounsel.defendants.some(d => registerDefendant.defendantIds.includes(d))) {
                    defenceCounsels.push(defenceCounsel);
                }
            });
        }
        return new CounselMapper(defenceCounsels).build();
    }

    getResultMapper(registerDefendant) {
        const defendantJudicialResults = registerDefendant.results.filter(r => r.level === LEVEL_TYPE.DEFENDANT).map(r => r.judicialResult);
        return new ResultMapper(defendantJudicialResults).build();
    }

    getProsecutionCaseOrApplicationMapper(registerDefendant) {
        return new ProsecutionCaseOrApplicationMapper(this.context, registerDefendant, this.hearingJson).build();
    }

    getDefendants(masterDefendantId) {
        return _(this.hearingJson.prosecutionCases).flatMap('defendants').value().filter(defendant => defendant.masterDefendantId === masterDefendantId);
    }
}

module.exports = YouthDefendantMapper;
