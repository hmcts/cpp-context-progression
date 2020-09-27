const FinancialOrderDetailsMapper = require('../FinancialOrderDetailsMapper');
const context = require('../../../../../../testing/defaultContext');
const referenceDataService = require('../../../../../../NowsHelper/service/ReferenceDataService');

jest.mock('ReferenceDataService');

describe('Financial Order Details mapper builds correctly', () => {
    const organisationUnitJson = require('./organisation-unit.json');
    const enforcementArea = require('./enforcement-area.json');
    const hearingJson = require('../../test/hearing.json');

    beforeEach(() => {
        referenceDataService.mockImplementation(() => {
            return {
                getOrganisationUnit: () => {
                    return Promise.resolve(organisationUnitJson);
                },
                getEnforcementAreaByPostcode: () => {
                    return Promise.resolve(enforcementArea);
                },
                getEnforcementAreaByLja: () => {
                    return Promise.resolve(enforcementArea);
                }
            };
        });
    });


    test('build financial order details with all values', async () => {
        const nowVariantJson = require(
            '../test/now-variant.json');

        const enforcementAreaByPostCodeMap = new Map();
        enforcementAreaByPostCodeMap.set('SW99 1AA', enforcementArea);
        const financialOrderDetails = await new FinancialOrderDetailsMapper(nowVariantJson, hearingJson, organisationUnitJson, enforcementArea, enforcementAreaByPostCodeMap, context).buildFinancialOrderDetails();
        expect(financialOrderDetails.totalAmountImposed).toBe("£61.50");
        expect(financialOrderDetails.totalBalance).toBe("£61.50");
        expect(financialOrderDetails.accountingDivisionCode).toBe(1111);
        expect(financialOrderDetails.bacsBankName).toBe("HMCTS Cambridgeshire");
        expect(financialOrderDetails.bacsSortCode).toBe("56-00-33");
        expect(financialOrderDetails.enforcementPhoneNumber).toBe("020 7556 8500");
        expect(financialOrderDetails.enforcementAddress.line1).toBe("LCCC Enforcement Unit");
        expect(financialOrderDetails.paymentTerms).toBe("Lump sum plus instalments\\nLump sum amount £61.5\nInstalment amount £61.5\nPayment frequency fortnightly\nInstalment start date 23 May 2020\nParent / Guardian to pay true\nNumber of days in default 15\nPayment card required true");
    });

    test('should build financial order details only with amounts when there is no enforcement area and bacs details', async () => {
        const nowVariantJson = require(
            '../test/now-variant.json');
        const enforcementAreaByPostCodeMap = new Map();
        const financialOrderDetails = await new FinancialOrderDetailsMapper(nowVariantJson, hearingJson, undefined, undefined, enforcementAreaByPostCodeMap, context).buildFinancialOrderDetails();
        expect(financialOrderDetails.totalAmountImposed).toBe("£61.50");
        expect(financialOrderDetails.totalBalance).toBe("£61.50");
        expect(financialOrderDetails.paymentTerms).toBe("Lump sum plus instalments\\nLump sum amount £61.5\nInstalment amount £61.5\nPayment frequency fortnightly\nInstalment start date 23 May 2020\nParent / Guardian to pay true\nNumber of days in default 15\nPayment card required true");
    });

    test('should build financial order details with other information when there is no enforcement area', async () => {
        const nowVariantJson = require(
            '../test/now-variant.json');
        const enforcementAreaByPostCodeMap = new Map();
        const financialOrderDetails = await new FinancialOrderDetailsMapper(nowVariantJson, hearingJson, organisationUnitJson, undefined, enforcementAreaByPostCodeMap, context).buildFinancialOrderDetails();
        expect(financialOrderDetails.totalAmountImposed).toBe("£61.50");
        expect(financialOrderDetails.totalBalance).toBe("£61.50");
        expect(financialOrderDetails.accountingDivisionCode).toBe(undefined);
        expect(financialOrderDetails.bacsBankName).toBe("HMCTS Cambridgeshire");
        expect(financialOrderDetails.bacsSortCode).toBe("56-00-33");
        expect(financialOrderDetails.enforcementPhoneNumber).toBe(undefined);
        expect(financialOrderDetails.enforcementAddress).toBe(undefined);
        expect(financialOrderDetails.paymentTerms).toBe("Lump sum plus instalments\\nLump sum amount £61.5\nInstalment amount £61.5\nPayment frequency fortnightly\nInstalment start date 23 May 2020\nParent / Guardian to pay true\nNumber of days in default 15\nPayment card required true");
    });
});
