const NowSolicitorMapper = require('../NowSolicitorMapper');

describe('Solicitor mapper builds correctly', () => {
    test('build solicitor with all values', () => {
        const hearingJson = require(
            '../../test/hearing.json');
        const nowVariantJson = require(
            '../../test/now-variant.json');
        const solicitorMapper = new NowSolicitorMapper(nowVariantJson, hearingJson);
        const solicitor = solicitorMapper.buildNowSolicitor();

        expect(solicitor.name).toBe('Sonja & Co LLP');
        expect(solicitor.address.line1).toBe('Legal House');
        expect(solicitor.address.line2).toBe('1 Old Road');
        expect(solicitor.address.line3).toBe('London');
        expect(solicitor.address.line4).toBe('Merton');
        expect(solicitor.address.line5).toBe(undefined);
        expect(solicitor.address.postCode).toBe('SW99 1AA');
    });
});