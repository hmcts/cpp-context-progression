const OutboundNowsVariants = require('../index');
const context = require('../../testing/defaultContext');
const moment = require('moment');

const referenceDataService = require('../../NowsHelper/service/ReferenceDataService');

jest.mock('ReferenceDataService');

describe('Now Document mapper builds correctly', () => {

    beforeEach(() => {
        referenceDataService.mockImplementation(() => {
            return {
                getOrganisationUnit: () => {
                    return Promise.resolve({});
                },
                getEnforcementAreaByPostcode: () => {
                    return Promise.resolve({});
                },
                getEnforcementAreaByLja: () => {
                    return Promise.resolve({});
                }
            };
        });
    });

    test('build now document with all values', async () => {
        const hearingJson = require(
            '../../OutboundNowsVariants/SetNowDocumentRequest/Mapper/NowContent/test/hearing.json');
        const nowVariantsJson = require(
            '../test/now-variants.json');
        context.bindings = {
            params: {
                cjscppuid: 'undefined',
                nowsVariantsSubscriptions: nowVariantsJson,
                hearingResultedObj: hearingJson
            }
        };

        const nows = await OutboundNowsVariants(context);

        expect(nows.length).toBe(1);
        expect(nows[0].hearingId).toBe("1828f356-f746-4f2d-932b-79ef2df95c80");
        expect(nows[0].nowTypeId).toBe("b4b55110-1d50-11e8-accf-0ed5f89f718b");
        expect(nows[0].materialId).toBeDefined();
        expect(nows[0].templateName).toBe("NoticeOrderWarrants");
        expect(nows[0].subscriberName).toBe("Defendant Subscription");
        expect(nows[0].visibleToUserGroups[0]).toBe("courtClerk");
        expect(nows[0].visibleToUserGroups[1]).toBe("Listing Officers");
        expect(nows[0].nowDistribution.firstClassLetter).toBe(true);
        expect(nows[0].nowDistribution.email).toBe(undefined);
        expect(nows[0].nowDistribution.secondClassLetter).toBe(undefined);
        expect(nows[0].nowContent.orderDate).toBe("2019-03-21");
        expect(nows[0].nowContent.orderingCourt.ljaCode).toBe("1800");
        expect(nows[0].nowContent.orderingCourt.ljaName).toBe("East Hampshire Magistrates' Court");
        expect(nows[0].nowContent.financialOrderDetails.paymentTerms).toBe("Instalments only with instalment amount 20, monthly, instalment start date "+(moment().add(90, 'd').format('YYYY-MM-DD')));
        expect(nows[0].nowContent.defendant.prosecutingAuthorityReference).toBe("ASN2,ASN3,ASN4");
    });
});

