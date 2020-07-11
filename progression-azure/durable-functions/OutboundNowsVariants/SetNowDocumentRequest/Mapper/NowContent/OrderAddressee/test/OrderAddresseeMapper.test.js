const OrderAddresseeMapper = require('../OrderAddresseeMapper');

describe('OrderAddressee mapper builds correctly', () => {
    test('build Order Addressee with all values', () => {
        const hearingJson = require(
            '../../test/hearing.json');
        const nowVariantJson = require(
            '../../test/now-variant.json');
        const orderAddresseeMapper = new OrderAddresseeMapper(nowVariantJson, hearingJson);
        const orderAddressee = orderAddresseeMapper.buildOrderAddressee();

        expect(orderAddressee.name).toBe('Fred Smith');
        expect(orderAddressee.address.line1).toBe('Flat 1');
        expect(orderAddressee.address.line2).toBe('1 Old Road');
        expect(orderAddressee.address.line3).toBe('London');
        expect(orderAddressee.address.line4).toBe('Merton');
        expect(orderAddressee.address.line5).toBe(undefined);
        expect(orderAddressee.address.postCode).toBe('SW99 1AA');
    });

    test('build Order Addressee with applicant address details', () => {
        const hearingJson = require(
            './hearing_with_applicant.json');
        const nowVariantJson = require(
            './now-variant-with-recipients.json');
        const orderAddresseeMapper = new OrderAddresseeMapper(nowVariantJson, hearingJson);
        const orderAddressee = orderAddresseeMapper.buildOrderAddressee();

        expect(orderAddressee.name).toBe('Fred Smith');
        expect(orderAddressee.address.line1).toBe('Flat 1');
        expect(orderAddressee.address.line2).toBe('1 Old Road');
        expect(orderAddressee.address.line3).toBe('London');
        expect(orderAddressee.address.line4).toBe('Merton');
        expect(orderAddressee.address.line5).toBe(undefined);
        expect(orderAddressee.address.postCode).toBe('SW99 1AA');
    });

    test('build Order Addressee with Prosecution Authority address details', () => {
        const hearingJson = require('./hearing_with_applicant.json');
        const nowVariantJson = require('./now-variant-with-prosecution-authority.json');
        const orderAddresseeMapper = new OrderAddresseeMapper(nowVariantJson, hearingJson);
        const orderAddressee = orderAddresseeMapper.buildOrderAddressee();

        expect(orderAddressee.name).toBe('Prosecution Authority Name1');
        expect(orderAddressee.address.line1).toBe('Flat 11');
        expect(orderAddressee.address.line2).toBe('11 Old Road');
        expect(orderAddressee.address.line3).toBe('London');
        expect(orderAddressee.address.line4).toBe('Merton');
        expect(orderAddressee.address.line5).toBe(undefined);
        expect(orderAddressee.address.postCode).toBe('SW99 1AA');
    });

    test('build Order Addressee with Legal entity details', () => {
        const hearingJson = require('./hearing-with-legal-entity.json');
        const nowVariantJson = require('./now-variant-with-legal-entity.json');
        const orderAddresseeMapper = new OrderAddresseeMapper(nowVariantJson, hearingJson);
        const orderAddressee = orderAddresseeMapper.buildOrderAddressee();

        expect(orderAddressee.name).toBe('HMCTS');
        expect(orderAddressee.address.line1).toBe('Yc0umNuhNh');
        expect(orderAddressee.address.line2).toBe('56Police House');
        expect(orderAddressee.address.line3).toBe('StreetDescription3');
        expect(orderAddressee.address.line4).toBe('Locality3');
        expect(orderAddressee.address.line5).toBe('Town6');
        expect(orderAddressee.address.postCode).toBe('TW14 9XD');
    });
});