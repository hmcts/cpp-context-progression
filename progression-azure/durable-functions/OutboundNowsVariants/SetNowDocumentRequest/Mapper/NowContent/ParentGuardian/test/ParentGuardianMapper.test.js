const ParentGuardianMapper = require('../ParentGuardianMapper');

describe('ParentGuardian mapper builds correctly', () => {
    test('build parent guardian with all values when role is parent', () => {
        const hearingJson = require('./hearing-with-parent.json');
        const nowVariantJson = require('../../test/now-variant.json');

        const parentGuardianMapper = new ParentGuardianMapper(nowVariantJson, hearingJson);

        const parentGuardian = parentGuardianMapper.buildParentGuardian();

        expect(parentGuardian.name).toBe('ParentFirst Parentlast');
        expect(parentGuardian.firstName).toBe('ParentFirst');
        expect(parentGuardian.lastName).toBe('Parentlast');
        expect(parentGuardian.address.line1).toBe('Flat 1');
        expect(parentGuardian.address.line2).toBe('1 Old Road');
    });

    test('build parent guardian with all values when role is parent guardian', () => {
        const hearingJson = require('./hearing-with-parent-guardian.json');
        const nowVariantJson = require('../../test/now-variant.json');

        const parentGuardianMapper = new ParentGuardianMapper(nowVariantJson, hearingJson);

        const parentGuardian = parentGuardianMapper.buildParentGuardian();

        expect(parentGuardian.name).toBe('ParentGuardianFirst ParentGuardianlast');
        expect(parentGuardian.firstName).toBe('ParentGuardianFirst');
        expect(parentGuardian.lastName).toBe('ParentGuardianlast');
        expect(parentGuardian.address.line1).toBe('Flat 1');
        expect(parentGuardian.address.line2).toBe('1 Old Road');
    });
});