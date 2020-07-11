const VocabularyService = require('./VocabularyService');
const LOCATION_TYPE = require('./LocationTypeEnum');

const getMockedHearingResulted = (locationType, defendantId) => {
    return {
        prosecutionCases: [
            {
                defendants: [{
                    id: defendantId,
                    masterDefendantId: '6647df67-a065-4d07-90ba-a8daa064ecc4',
                    personDefendant: {
                        custodialEstablishment: {
                            custody: locationType
                        }
                    }
                }]
            }
        ],
        "defendantAttendance": [
            {
                "attendanceDays": [
                    {
                        "attendanceType": "IN_PERSON",
                        "day": "2020-05-11"
                    }
                ],
                "defendantId": defendantId
            }
        ],
        "courtCentre": {
            "id": "6647df67-a065-4d07-90ba-a8daa064ecd9",
            "name": "Lavender Hill",
            "welshCourtCentre": true
        }
    };
};

const getMockedDefendantContextBase = () => {

    return {
        results: [
            {
                "judicialResult": {
                    judicialResultId: '',
                    orderedHearingId: '',
                    label: '',
                    isAdjournmentResult: '',
                    isFinancialResult: '',
                    isConvictedResult: '',
                    isAvailableForCourtExtract: '',
                    orderedDate: '2020-05-11',
                    category: '',
                    resultText: '',
                    terminatesOffenceProceedings: '',
                    isLifeDuration: '',
                    isPublishedAsAPrompt: '',
                    isExcludedFromResults: '',
                    isAlwaysPublished: '',
                    isUrgent: '',
                    isD20: ''
                }
            },
            {
                "judicialResult": {
                    judicialResultId: '',
                    orderedHearingId: '',
                    label: '',
                    isAdjournmentResult: '',
                    isFinancialResult: '',
                    isConvictedResult: '',
                    isAvailableForCourtExtract: '',
                    orderedDate: '2020-05-11',
                    category: '',
                    resultText: '',
                    terminatesOffenceProceedings: '',
                    isLifeDuration: '',
                    isPublishedAsAPrompt: '',
                    isExcludedFromResults: '',
                    isAlwaysPublished: '',
                    isUrgent: '',
                    isD20: ''
                }
            }
        ],
        cases: ['c10e3b71-6a6d-45ef-9b62-34df4d54971a'],
        defendantIds: ['6647df67-a065-4d07-90ba-a8daa064ecc4'],
        applications: [],
        masterDefendantId: '6647df67-a065-4d07-90ba-a8daa064ecc4',
        isYouthDefendant: true,
        matchedSubscriptions: undefined
    };

}

describe("Vocabulary Service", () => {

    test("Should set the correct police custody", () => {

        const defendantId = '6647df67-a065-4d07-90ba-a8daa064ecc4';
        const hearingResulted = getMockedHearingResulted(LOCATION_TYPE.POLICE_STATION, defendantId);
        const defendantContextBase = getMockedDefendantContextBase();
        const service = new VocabularyService(hearingResulted, defendantContextBase);

        const vocabularyInfo = service.getVocabularyInfo();

        expect(vocabularyInfo.custodyLocationIsPolice).toBe(true);
        expect(vocabularyInfo.custodyLocationIsPrison).toBe(false);
        expect(vocabularyInfo.atleastOneCustodialResult).toBe(false);
        expect(vocabularyInfo.appearedInPerson).toBe(true);
        expect(vocabularyInfo.appearedByVideoLink).toBe(false);
        expect(vocabularyInfo.allNonCustodialResults).toBe(true);
        expect(vocabularyInfo.atleastOneNonCustodialResult).toBe(true);
        expect(vocabularyInfo.anyAppearance).toBe(true);
        expect(vocabularyInfo.inCustody).toBe(true);
        expect(vocabularyInfo.youthDefendant).toBe(true);
        expect(vocabularyInfo.adultDefendant).toBe(false);
        expect(vocabularyInfo.adultOrYouthDefendant).toBe(true);
        expect(vocabularyInfo.welshCourtHearing).toBe(true);
        expect(vocabularyInfo.englishCourtHearing).toBe(false);
        expect(vocabularyInfo.anyCourtHearing).toBe(true);
    });

    test("Should set the correct prison custody", () => {

        const defendantId = '6647df67-a065-4d07-90ba-a8daa064ecc4';
        const hearingResulted = getMockedHearingResulted(LOCATION_TYPE.PRISON, defendantId);
        const defendantContextBase = getMockedDefendantContextBase();
        const service = new VocabularyService(hearingResulted, defendantContextBase);

        const vocabularyInfo = service.getVocabularyInfo();

        expect(vocabularyInfo.custodyLocationIsPolice).toBe(false);
        expect(vocabularyInfo.custodyLocationIsPrison).toBe(true);
        expect(vocabularyInfo.atleastOneCustodialResult).toBe(false);
        expect(vocabularyInfo.appearedInPerson).toBe(true);
        expect(vocabularyInfo.appearedByVideoLink).toBe(false);
        expect(vocabularyInfo.allNonCustodialResults).toBe(true);
        expect(vocabularyInfo.atleastOneNonCustodialResult).toBe(true);
        expect(vocabularyInfo.anyAppearance).toBe(true);
        expect(vocabularyInfo.inCustody).toBe(true);
        expect(vocabularyInfo.youthDefendant).toBe(true);
        expect(vocabularyInfo.adultDefendant).toBe(false);
        expect(vocabularyInfo.adultOrYouthDefendant).toBe(true);
        expect(vocabularyInfo.welshCourtHearing).toBe(true);
        expect(vocabularyInfo.englishCourtHearing).toBe(false);
        expect(vocabularyInfo.anyCourtHearing).toBe(true);
    });
});
