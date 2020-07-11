const ParentGuardian = require('../../Models/ParentGuardian');
const AddressMapper = require('../Address/AddressMapper');
const _ = require('lodash');

class ParentGuardianMapper {

    constructor(registerDefendant, hearingJson) {
        this.registerDefendant = registerDefendant;
        this.hearingJson = hearingJson;
    }

    build() {
        const defendant = this.getDefendant();
        const person = this.findParentOrGuardian(defendant.associatedPersons);
        if (person) {
            const parentGuardian = new ParentGuardian();
            parentGuardian.name = [person.firstName, person.middleName, person.lastName].filter(item => item).join(' ').trim();
            parentGuardian.address = this.getAddressMapper(person.address).build();
            return parentGuardian;
        }
    }

    findParentOrGuardian(associatedPersons) {
        const parent = 'parent';
        const guardian = 'guardian';
        let person = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role.toLowerCase === parent.toLowerCase && associatedPerson.person);
        if (!person) {
            person = associatedPersons && associatedPersons.find(associatedPerson => associatedPerson.role.toLowerCase === guardian.toLowerCase && associatedPerson.person);
        }
        return person && person.person;
    }

    getDefendant() {
        return _(this.hearingJson.prosecutionCases).flatMap('defendants').value()
            .find(defendant => defendant.masterDefendantId
                === this.registerDefendant.masterDefendantId); //find the first defendant
    }

    getAddressMapper(addressInfo){
        return new AddressMapper(addressInfo);
    }

}

module.exports = ParentGuardianMapper;