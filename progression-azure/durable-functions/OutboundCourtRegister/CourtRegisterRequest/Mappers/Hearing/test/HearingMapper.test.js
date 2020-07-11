const HearingMapper = require('../HearingMapper');

describe('Hearing Mapper', () => {

    test('Should return correct values', () => {
        const fakeHearingJson = {
            "id": "1828f356-f746-4f2d-932b-79ef2df95c80",
            "jurisdictionType": "MAGISTRATES",
            "type": {
                "description": "Sentence",
                "id": "c6d76309-c912-436b-a21b-a4c4450bc052"
            },
            "defendantAttendance": [
                {
                    "defendantId": "6647df67-a065-4d07-90ba-a8daa064ecc4",
                    "attendanceDays": [
                        {
                            "day": "2019-02-01",
                            "isInAttendance": false
                        }
                    ]
                }
            ]
        };

        const fakeCourtRegisterFragment = {
            "registerDate": "2019-02-01"
        };

        const fakeDefendant = {
            "id": "6647df67-a065-4d07-90ba-a8daa064ecc4",
            "associatedDefenceOrganisation": {
                "defenceOrganisation": {
                    "organisation": {
                        "name": "Test defence Organisation"
                    }
                }
            }
        };

        const result = new HearingMapper(fakeHearingJson, fakeCourtRegisterFragment, fakeDefendant).build();
        
        expect(result).toBeTruthy();      
        expect(result.jurisdiction).toBe(fakeHearingJson.jurisdictionType);
        expect(result.hearingType).toBe(fakeHearingJson.type.description);
        expect(result.defendantPresent).toBe(true);
        expect(result.attendingSolicitorName).toBe(fakeDefendant.associatedDefenceOrganisation.defenceOrganisation.organisation.name);

    });
});