const MinorCreditorMapper = require('../MinorCreditorMapper');

describe('MinorCreditor Mapper build correctly', () => {
    test('should construct distinct minor creditor details', () => {
        const complianceEnforcementJson = require(
            './enforcement-with-minor-creditor-as-defendant.json');
        const minorCreditorMapper = new MinorCreditorMapper(complianceEnforcementJson);
        const minorCreditor = minorCreditorMapper.buildMinorCreditor();
        expect(minorCreditor.length).toBe(1);
        expect(minorCreditor[0].minorCreditorId).toBeDefined();
        expect(minorCreditor[0].minorCreditorSurname).toBe("lastName");
        expect(minorCreditor[0].minorCreditorForenames).toBe("indFirstName middleName");
        expect(minorCreditor[0].minorCreditorAddressLine1).toBe("line1");
        expect(minorCreditor[0].minorCreditorAddressLine2).toBe("line2");
        expect(minorCreditor[0].minorCreditorAddressLine3).toBe("line3");
        expect(minorCreditor[0].minorCreditorAddressLine4).toBe("line4");
        expect(minorCreditor[0].minorCreditorAddressLine5).toBe("line5");
        expect(minorCreditor[0].minorCreditorPostcode).toBe("SL2 5FZ");
        expect(minorCreditor[0].minorCreditorPayByBACS).toBe("N");
    });

    test('should construct distinct minor creditor details as organisation', () => {
        const complianceEnforcementJson = require(
            './enforcement-with-minor-creditor-as-organisation.json');
        const minorCreditorMapper = new MinorCreditorMapper(complianceEnforcementJson);
        const minorCreditor = minorCreditorMapper.buildMinorCreditor();
        expect(minorCreditor.length).toBe(1);
        expect(minorCreditor[0].minorCreditorId).toBeDefined();
        expect(minorCreditor[0].companyName).toBe("OrgName");
        expect(minorCreditor[0].minorCreditorAddressLine1).toBe("line1");
        expect(minorCreditor[0].minorCreditorAddressLine2).toBe("line2");
        expect(minorCreditor[0].minorCreditorAddressLine3).toBe("line3");
        expect(minorCreditor[0].minorCreditorAddressLine4).toBe("line4");
        expect(minorCreditor[0].minorCreditorAddressLine5).toBe("line5");
        expect(minorCreditor[0].minorCreditorPostcode).toBe("SL2 5FZ");
        expect(minorCreditor[0].minorCreditorPayByBACS).toBe("N");
    });
});