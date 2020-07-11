const CustodyLocation = require('../../Model/CustodyLocation');
const CustodyAddress = require('../../Model/CustodyAddress');
const _ = require('lodash');

class CustodyLocationMapper {
    constructor(prisonCourtRegister, prisonsCustodySuitesRefData, hearingJson) {
        this.prisonCourtRegister = prisonCourtRegister;
        this.hearingJson = hearingJson;
        this.prisonsCustodySuitesRefData = prisonsCustodySuitesRefData;
    }

    build() {
        const defendant = this.getDefendant();
        if(defendant.personDefendant && defendant.personDefendant.custodialEstablishment) {
            const custodialEstablishment = defendant.personDefendant.custodialEstablishment;
            const prisonsCustodySuite = this.prisonsCustodySuitesRefData['prisons-custody-suites'].find(prisonCustodySuite => prisonCustodySuite.id === custodialEstablishment.id);
            if(prisonsCustodySuite) {
                const custodyLocation = new CustodyLocation();
                custodyLocation.name = prisonsCustodySuite.name;
                if(prisonsCustodySuite.emails && prisonsCustodySuite.emails.length) {
                    const custodyAddress = new CustodyAddress();
                    custodyAddress.email = prisonsCustodySuite.emails[0].emailAddress;
                    custodyLocation.custodyAddress = custodyAddress;
                }
                return custodyLocation;
            }
        }
    }

    getDefendant() {
        return _(this.hearingJson.prosecutionCases).flatMapDeep('defendants').value()
            .find(defendant => defendant.masterDefendantId
                               === this.prisonCourtRegister.registerDefendant.masterDefendantId);
    }
}

module.exports = CustodyLocationMapper;