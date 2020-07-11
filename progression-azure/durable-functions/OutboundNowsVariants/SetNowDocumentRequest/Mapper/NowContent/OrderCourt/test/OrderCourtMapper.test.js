const OrderCourtMapper = require('../OrderCourtMapper');

describe('OrderCourtMapper mapper builds correctly', () => {
    test('build Order Court with all values', () => {
        const hearingJson = require(
            '../../test/hearing.json');
        const nowVariantJson = require(
            '../../test/now-variant.json');
        const orderCourtMapper = new OrderCourtMapper(nowVariantJson, hearingJson);
        const orderCourt = orderCourtMapper.buildOrderCourt();

        expect(orderCourt.ljaCode).toBe('1800');
        expect(orderCourt.ljaName).toBe('East Hampshire Magistrates\' Court');
        expect(orderCourt.welshLjaName).toBe('East Hampshire Magistrates\' Court');
        expect(orderCourt.courtCentreName).toBe('Lavender Hill');
        expect(orderCourt.welshCourtCentreName).toBe('Lavender Hill');
        expect(orderCourt.address.line1).toBe('Flat 1');
        expect(orderCourt.address.line2).toBe('1 Old Road');
        expect(orderCourt.address.line3).toBe('London');
        expect(orderCourt.address.line4).toBe('Merton');
        expect(orderCourt.address.line5).toBe(undefined);
        expect(orderCourt.address.postCode).toBe('SW99 1AA');
        expect(orderCourt.welshAddress.line1).toBe('Flat 1');
        expect(orderCourt.welshAddress.line2).toBe('1 Old Road');
        expect(orderCourt.welshAddress.line3).toBe('London');
        expect(orderCourt.welshAddress.line4).toBe('Merton');
        expect(orderCourt.welshAddress.line5).toBe(undefined);
        expect(orderCourt.welshAddress.postCode).toBe('SW99 1AA');
    });
});