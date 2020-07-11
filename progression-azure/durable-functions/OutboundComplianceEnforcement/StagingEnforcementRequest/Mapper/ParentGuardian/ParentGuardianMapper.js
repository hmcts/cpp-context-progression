const Mapper = require('../Mapper');
const ParentGuardian = require('../../Model/ParentGuardian');

class ParentGuardianMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    buildParentGuardian() {
        const parentGuardian = new ParentGuardian();
        const defendant = this.getDefendant();
        const person = this.findParentOrGuardian(defendant.associatedPersons);
        if(person){
            parentGuardian.name = [person.firstName, person.middleName,person.lastName].filter(item => item).join(' ').trim();
            parentGuardian.address1 = person.address.address1;
            parentGuardian.address2 = person.address.address2;
            parentGuardian.address3 = person.address.address3;
            parentGuardian.address4 = person.address.address4;
            parentGuardian.address5 = person.address.address5;
            parentGuardian.postCode = person.address.postcode;
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

}

module.exports = ParentGuardianMapper;