const NowDocumentRequestMapper = require('../NowDocumentRequestMapper');
const context = require('../../../../../testing/defaultContext');
const moment = require('moment');

describe('Now Document mapper builds correctly', () => {
    test('build now document with all values', () => {
        const hearingJson = require(
            '../../NowContent/test/hearing.json');
        const nowVariantJson = require(
            '../test/now-variant.json');

        const organisationUnitJson = require('./organisation-unit.json');
        const enforcementArea = require('./enforcement-area.json');

        const enforcementAreaByPostCodeMap = new Map();
        enforcementAreaByPostCodeMap.set('SW99 1AA', enforcementArea);

        const nowDocumentRequest = new NowDocumentRequestMapper(nowVariantJson, hearingJson, organisationUnitJson, enforcementArea, enforcementAreaByPostCodeMap, context).buildNowDocumentRequest();

        expect(nowDocumentRequest.hearingId).toBe("1828f356-f746-4f2d-932b-79ef2df95c80");
        expect(nowDocumentRequest.nowTypeId).toBe("b4b55110-1d50-11e8-accf-0ed5f89f718b");
        expect(nowDocumentRequest.materialId).toBeDefined();
        expect(nowDocumentRequest.templateName).toBe("NoticeOrderWarrants");
        expect(nowDocumentRequest.subscriberName).toBe("Defendant Subscription");
        expect(nowDocumentRequest.visibleToUserGroups[0]).toBe("courtClerk");
        expect(nowDocumentRequest.visibleToUserGroups[1]).toBe("Listing Officers");
        expect(nowDocumentRequest.nowDistribution.firstClassLetter).toBe(true);
        expect(nowDocumentRequest.nowDistribution.email).toBe(undefined);
        expect(nowDocumentRequest.nowDistribution.secondClassLetter).toBe(undefined);
        expect(nowDocumentRequest.nowContent.orderDate).toBe("2019-03-21");
        expect(nowDocumentRequest.nowContent.orderingCourt.ljaCode).toBe("1800");
        expect(nowDocumentRequest.nowContent.orderingCourt.ljaName).toBe("East Hampshire Magistrates' Court");
        const startDate = moment().add(90, 'd').format('YYYY-MM-DD');
        expect(nowDocumentRequest.nowContent.financialOrderDetails.paymentTerms).toBe("Instalments only with instalment amount 20, monthly, instalment start date "+startDate);
        expect(nowDocumentRequest.nowContent.defendant.prosecutingAuthorityReference).toBe("ASN2,ASN3,ASN4");
    });

    test('build now document with all values for email delivery', () => {
        const hearingJson = require(
            '../../NowContent/test/hearing.json');
        const nowVariantJson = require(
            '../test/now-variant-email-delivery.json');
        const organisationUnitJson = require('./organisation-unit.json');
        const enforcementArea = require('./enforcement-area.json');
        const enforcementAreaByPostCodeMap = new Map();
        enforcementAreaByPostCodeMap.set('SW99 1AA', enforcementArea);
        const nowDocumentRequest = new NowDocumentRequestMapper(nowVariantJson, hearingJson, organisationUnitJson, enforcementArea, enforcementAreaByPostCodeMap, context).buildNowDocumentRequest();
        expect(nowDocumentRequest.hearingId).toBe("1828f356-f746-4f2d-932b-79ef2df95c80");
        expect(nowDocumentRequest.nowTypeId).toBe("b4b55110-1d50-11e8-accf-0ed5f89f718b");
        expect(nowDocumentRequest.materialId).toBeDefined();
        expect(nowDocumentRequest.templateName).toBe("NoticeOrderWarrants");
        expect(nowDocumentRequest.subscriberName).toBe("Defendant Subscription");
        expect(nowDocumentRequest.visibleToUserGroups[0]).toBe("courtClerk");
        expect(nowDocumentRequest.visibleToUserGroups[1]).toBe("Listing Officers");
        expect(nowDocumentRequest.nowDistribution.firstClassLetter).toBe(undefined);
        expect(nowDocumentRequest.nowDistribution.email).toBe(true);
        expect(nowDocumentRequest.nowDistribution.secondClassLetter).toBe(undefined);
        expect(nowDocumentRequest.nowContent.orderDate).toBe("2019-03-21");
        expect(nowDocumentRequest.nowContent.orderingCourt.ljaCode).toBe("1800");
        expect(nowDocumentRequest.nowContent.orderingCourt.ljaName).toBe("East Hampshire Magistrates' Court");
        expect(nowDocumentRequest.nowDistribution.emailContent[0].label).toBe('subject');
        expect(nowDocumentRequest.nowDistribution.emailContent[0].value).toBe('DIGITAL WARRANT: Lavender Hill; Fred Smith; 1965-12-27; TFL4359536,TVL298320922');
    });
});