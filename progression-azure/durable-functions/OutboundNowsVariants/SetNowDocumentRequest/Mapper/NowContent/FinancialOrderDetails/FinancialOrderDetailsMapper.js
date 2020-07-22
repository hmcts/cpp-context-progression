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

        const amount = this.getFinancialResults();
        if(amount) {
            const financialOrderDetails = new FinancialOrderDetails();
            if(this.organisationUnitsRefData) {
                this.getEnforcementAreaBacs(this.organisationUnitsRefData, financialOrderDetails)
            }

            if (defendantPostCode && enforcementAreaByPostCode) {
                this.getEnforcementDetails(enforcementAreaByPostCode, financialOrderDetails);
            }

            if (!enforcementAreaByPostCode && courtCentre.lja && this.organisationUnitsRefData) {
                this.getEnforcementDetails(this.enforcementAreaByLjaCode, financialOrderDetails);
            }

            financialOrderDetails.paymentTerms = this.getPaymentTerms();

            financialOrderDetails.totalAmountImposed = isNaN(amount.total) ? "£0" : "£"+amount.total;
            financialOrderDetails.totalBalance = isNaN(amount.outstandingBalance) ? "£0" : "£"+amount.outstandingBalance;
            return financialOrderDetails;
        }
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

    getEnforcementAreaBacs(organisationUnitsRefData, financialOrderDetails) {
        const enforcementAreaBACS = organisationUnitsRefData.enforcementArea;
        if(enforcementAreaBACS) {
            financialOrderDetails.bacsBankName = enforcementAreaBACS.bankAccntName;
            financialOrderDetails.bacsSortCode = enforcementAreaBACS.bankAccntSortCode;
            financialOrderDetails.bacsAccountNumber = enforcementAreaBACS.bankAccntNum;
        }
    }

    getEnforcementDetails(enforcementArea, financialOrderDetails) {
        if (enforcementArea) {
            const address = new NowAddress();
            address.line1 = enforcementArea.address1;
            address.line2 = enforcementArea.address2;
            address.line3 = enforcementArea.address3;
            address.line4 = enforcementArea.address4;
            address.postCode = enforcementArea.postcode;
            address.emailAddress1 = enforcementArea.email;

            financialOrderDetails.enforcementPhoneNumber = enforcementArea.phone;
            financialOrderDetails.enforcementAddress = address;
            financialOrderDetails.enforcementEmail = enforcementArea.email;
            financialOrderDetails.accountingDivisionCode = enforcementArea.accountDivisionCode;
        }
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
