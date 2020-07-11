const AliasMapper = require('../Alias/AliasMapper');
const AddressMapper = require('../Address/AddressMapper');
const HearingMapper = require('../Hearing/HearingMapper');
const CounselMapper = require('../Counsel/CounselMapper');
const ResultMapper = require('../Result/ResultMapper');
const Defendant = require('../../Model/Defendant');
const ProsecutionCaseOrApplicationMapper = require('../ProsecutionCaseOrApplication/ProsecutionCaseOrApplicationMapper');
const LEVEL_TYPE = require('../../../../NowsHelper/service/LevelTypeEnum');
const _ = require('lodash');

const NOT_APPLICABLE = 'Not Applicable';

class DefendantMapper {

    constructor(context, hearing, prisonCourtRegister) {
        this.context = context;
        this.hearing = hearing;
        this.prisonCourtRegister = prisonCourtRegister;
    }

    build() {
        const defendants = this.getFirstDefendants(this.prisonCourtRegister.registerDefendant.defendantIds);
        const defendant = defendants[0];
        const personDetails = defendant.personDefendant.personDetails;
        const defendantInfo = new Defendant();
        defendantInfo.name = this.populateName(personDetails);
        defendantInfo.dateOfBirth = personDetails.dateOfBirth;
        defendantInfo.address = this.getAddressMapper(personDetails.address);
        defendantInfo.gender = personDetails.gender;
        defendantInfo.postHearingCustodyStatus = this.populatePostHearingCustodyStatus(defendant);
        defendantInfo.masterDefendantId = this.prisonCourtRegister.registerDefendant.masterDefendantId;
        defendantInfo.nationality = personDetails.nationalityDescription;
        defendantInfo.hearing = this.getHearingMapper(defendant);
        defendantInfo.aliases = this.getAliasMapper(defendant.aliases);
        defendantInfo.defenceCounsels = this.getCounselMapper();
        defendantInfo.prosecutionCasesOrApplications = this.getProsecutionCaseOrApplicationMapper();
        defendantInfo.defendantResults = this.getResultMapper();
        return defendantInfo;
    }

    getAddressMapper(addressInfo) {
        return new AddressMapper(addressInfo).build();
    }

    populatePostHearingCustodyStatus(defendant) {
        let postHearingCustodyStatus = NOT_APPLICABLE;
        if (defendant.defendantCaseJudicialResults) {
            const filteredCustodyStatuses = defendant.defendantCaseJudicialResults.filter(j => j.postHearingCustodyStatus !== NOT_APPLICABLE);
            if (filteredCustodyStatuses.length) {
                return filteredCustodyStatuses[0].postHearingCustodyStatus;
            }
        }
        return postHearingCustodyStatus;
    }

    populateName(personDetails) {
        return Array.of(personDetails.firstName, personDetails.middleName, personDetails.lastName)
            .filter((n) => (n)).reduce((a, b) => a + ' ' + b);
    }

    getAliasMapper(aliases) {
        return new AliasMapper(aliases).build();
    }

    getHearingMapper(defendant) {
        return new HearingMapper(this.hearing, this.prisonCourtRegister, defendant).build();
    }

    getCounselMapper() {
        const defenceCounsels = [];
        if (this.hearing.defenceCounsels && this.hearing.defenceCounsels.length) {
            this.hearing.defenceCounsels.forEach(defenceCounsel => {
                if (defenceCounsel.defendants.some(
                    d => this.prisonCourtRegister.registerDefendant.defendantIds.includes(d))) {
                    defenceCounsels.push(defenceCounsel);
                }
            });
        }
        return new CounselMapper(defenceCounsels).build();
    }

    getResultMapper() {
        const defendantJudicialResults = this.prisonCourtRegister.registerDefendant.results.filter(r => r.level === LEVEL_TYPE.DEFENDANT).map(r => r.judicialResult);
        return new ResultMapper(defendantJudicialResults).build();
    }

    getProsecutionCaseOrApplicationMapper() {
        return new ProsecutionCaseOrApplicationMapper(this.prisonCourtRegister.registerDefendant, this.hearing).build();
    }

    getFirstDefendants(defendantIds) {
        return _(this.hearing.prosecutionCases).flatMap('defendants').value()
            .filter(defendant => defendantIds.includes(defendant.id));
    }

}

module.exports = DefendantMapper;