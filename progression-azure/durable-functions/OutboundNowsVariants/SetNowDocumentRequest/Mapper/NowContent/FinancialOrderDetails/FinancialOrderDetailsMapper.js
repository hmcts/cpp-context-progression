const {FinancialOrderDetails, NowAddress} = require('../../../Model/NowContent');
const Mapper = require('../../Mapper');
const PaymentTermsCalculator = require('./PaymentTermsCalculator');
const FinancialResultCalculator = require('./FinancialResultCalculator');

class FinancialOrderDetailsMapper extends Mapper {
    constructor(nowVariant, hearingJson, organisationUnitsRefData, enforcementAreaByLjaCode, enforcementAreaByPostCodeMap, context) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
        this.organisationUnitsRefData = organisationUnitsRefData;
        this.enforcementAreaByLjaCode = enforcementAreaByLjaCode;
        this.enforcementAreaByPostCodeMap = enforcementAreaByPostCodeMap;
        this.context = context;
    }

    buildFinancialOrderDetails() {
        const courtCentre = this.hearingJson.courtCentre;
        const defendantFromHearingJson = this.getDefendant();
        const defendantPostCode = this.postcode(defendantFromHearingJson);
        const enforcementAreaByPostCode = this.enforcementAreaByPostCodeMap.get(defendantPostCode);

        let financialOrderDetails;

        if (defendantPostCode && this.organisationUnitsRefData && enforcementAreaByPostCode) {
            financialOrderDetails = this.getLjaDetails(courtCentre.id, this.organisationUnitsRefData, enforcementAreaByPostCode);
        }

        if (!enforcementAreaByPostCode && courtCentre.lja && this.organisationUnitsRefData) {
            financialOrderDetails = this.getLjaDetails(courtCentre.id, this.organisationUnitsRefData, this.enforcementAreaByLjaCode);
        }

        const amount = this.getFinancialResults();
        if(amount) {
            financialOrderDetails.totalAmountImposed = isNaN(amount.total) ? "£0" : "£"+amount.total;
            financialOrderDetails.totalBalance = isNaN(amount.outstandingBalance) ? "£0" : "£"+amount.outstandingBalance;
        }
        financialOrderDetails.paymentTerms = this.getPaymentTerms();
        return financialOrderDetails;
    }

    postcode(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.address.postcode) {
            return defendantFromHearingJson.personDefendant.personDetails.address.postcode;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.address.postcode) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.postcode;
        }
    }

    isLegalEntityDefendantAddressAvailable(defendantJson) {
        return !!(defendantJson.legalEntityDefendant &&
                  defendantJson.legalEntityDefendant.organisation.address);

    }

    getLjaDetails(courtCentreId, organisationUnitsRefData, enforcementArea) {
        const address = new NowAddress();
        address.line1 = enforcementArea.address1;
        address.line2 = enforcementArea.address2;
        address.line3 = enforcementArea.address3;
        address.line4 = enforcementArea.address4;
        address.postCode = enforcementArea.postcode;
        address.emailAddress1 = enforcementArea.email;

        const enforcementAreaBACS = organisationUnitsRefData.enforcementArea;

        const financialOrderDetails = new FinancialOrderDetails();
        financialOrderDetails.enforcementPhoneNumber = enforcementArea.phone;
        financialOrderDetails.enforcementAddress = address;
        financialOrderDetails.enforcementEmail = enforcementArea.email;
        financialOrderDetails.accountingDivisionCode = enforcementArea.accountDivisionCode;
        if(enforcementAreaBACS) {
            financialOrderDetails.bacsBankName = enforcementAreaBACS.bankAccntName;
            financialOrderDetails.bacsSortCode = enforcementAreaBACS.bankAccntSortCode;
            financialOrderDetails.bacsAccountNumber = enforcementAreaBACS.bankAccntNum;
        }
        return financialOrderDetails;
    }

    getPaymentTerms() {
        return new PaymentTermsCalculator(this.nowVariant.results).buildPaymentTerms();
    }

    getFinancialResults() {
        return new FinancialResultCalculator(this.nowVariant.results).buildFinancialCalculations();
    }

    isAddressAvailable(defendantJson) {
        return !!(defendantJson.personDefendant &&
                  defendantJson.personDefendant.personDetails.address);
    }

}
module.exports = FinancialOrderDetailsMapper;
