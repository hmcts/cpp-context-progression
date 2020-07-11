const NowDistributionMapper = require('../NowDistributionMapper');

describe('NowDistribution mapper builds correctly', () => {
    test('build NowDistribution with all values', () => {
        const hearingJson = require(
            '../../NowContent/test/hearing');
        const nowVariantJson = require(
            '../../NowContent/test/now-variant.json');
        const nowDistributionMapper = new NowDistributionMapper(nowVariantJson, hearingJson);
        const nowDistribution = nowDistributionMapper.buildNowDistribution();
        expect(nowDistribution.firstClassLetter).toBe(true);
        expect(nowDistribution.email).toBe(undefined);
        expect(nowDistribution.secondClassLetter).toBe(undefined);
    });

    test('build NowDistribution with all values email delivery', () => {
        const hearingJson = require(
            '../../NowContent/test/hearing');
        const nowVariantJson = require(
            '../../NowContent/test/now-variant-email-delivery.json');
        const nowDistributionMapper = new NowDistributionMapper(nowVariantJson, hearingJson);
        const nowDistribution = nowDistributionMapper.buildNowDistribution();
        expect(nowDistribution.firstClassLetter).toBe(undefined);
        expect(nowDistribution.email).toBe(true);
        expect(nowDistribution.secondClassLetter).toBe(undefined);
        expect(nowDistribution.emailContent[0].label).toBe('subject');
        expect(nowDistribution.emailContent[0].value).toBe('DIGITAL WARRANT: Lavender Hill; Fred Smith; 1965-12-27; TFL4359536,TVL298320922');
    });
});