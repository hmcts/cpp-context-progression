const DefendantContextBaseService = require('./DefendantContextBaseService')

describe('DefendantContextBaseService', () => {

    const getMockedHearingResulted = () => {

        return {
            "prosecutionCases": [
                {
                    "id": "c10e3b71-6a6d-45ef-9b62-34df4d54971a",
                    "defendants": [
                        {
                            "id": "6647df67-a065-4d07-90ba-a8daa064ecc4",
                            "masterDefendantId": "6647df67-a065-4d07-90ba-a8daa064ecc4",
                            "prosecutionCaseId": "c10e3b71-6a6d-45ef-9b62-34df4d54971a",
                            "offences": [
                                {

                                    "judicialResults": [
                                        {
                                            "judicialResultId": "",
                                            "orderedHearingId": "",
                                            "label": "",
                                            "isAdjournmentResult": "",
                                            "isFinancialResult": "",
                                            "isConvictedResult": "",
                                            "isAvailableForCourtExtract": "",
                                            "orderedDate": "",
                                            "category": "",
                                            "resultText": "",
                                            "terminatesOffenceProceedings": "",
                                            "isLifeDuration": "",
                                            "isPublishedAsAPrompt": "",
                                            "isExcludedFromResults": "",
                                            "isAlwaysPublished": "",
                                            "isUrgent": "",
                                            "isD20": ""
                                        }
                                    ],

                                }
                            ],

                        },
                        {
                            "id": "ded76309-c912-436b-a21b-a4c4450bc052",
                            "masterDefendantId": "ded76309-c912-436b-a21b-a4c4450bc052",
                            "prosecutionCaseId": "c10e3b71-6a6d-45ef-9b62-34df4d54971a",

                            "offences": [
                                {

                                    "judicialResults": [
                                        {
                                            "judicialResultId": "c6d26764-5b6a-4e30-8860-2e1093f89a6e",
                                            "orderedHearingId": "f9f41ca5-a963-46ec-b2c2-a307883c1db5",
                                            "label": "Imprisonment",
                                            "isAdjournmentResult": "",
                                            "isFinancialResult": "",
                                            "isConvictedResult": "",
                                            "isAvailableForCourtExtract": "",
                                            "orderedDate": "",
                                            "category": "",
                                            "resultText": "",
                                            "terminatesOffenceProceedings": "",
                                            "isLifeDuration": "",
                                            "isPublishedAsAPrompt": "",
                                            "isExcludedFromResults": "",
                                            "isAlwaysPublished": "",
                                            "isUrgent": "",
                                            "isD20": ""
                                        },
                                        {
                                            "judicialResultId": "b8f91621-135a-4c26-8b38-e83dbeda0b3f",
                                            "orderedHearingId": "7669d6a3-ef75-40c7-ac13-8be3a9875c62",
                                            "label": "Financial Penalty",
                                            "isAdjournmentResult": "",
                                            "isFinancialResult": "",
                                            "isConvictedResult": "",
                                            "isAvailableForCourtExtract": "",
                                            "orderedDate": "",
                                            "category": "",
                                            "resultText": "",
                                            "terminatesOffenceProceedings": "",
                                            "isLifeDuration": "",
                                            "isPublishedAsAPrompt": "",
                                            "isExcludedFromResults": "",
                                            "isAlwaysPublished": "",
                                            "isUrgent": "",
                                            "isD20": ""
                                        }
                                    ],

                                }
                            ],

                        }
                    ]
                },

                {
                    "id": "c10e3b71-6a6d-45ef-9b62-34df4d54971a",
                    "defendants": [
                        {
                            "id": "6647df67-a065-4d07-90ba-a8daa064ecc4",
                            "masterDefendantId": "6647df67-a065-4d07-90ba-a8daa064ecc4",
                            "prosecutionCaseId": "c10e3b71-6a6d-45ef-9b62-34df4d54971a",
                            "offences": [
                                {

                                    "judicialResults": [
                                        {
                                            "judicialResultId": "",
                                            "orderedHearingId": "",
                                            "label": "",
                                            "isAdjournmentResult": "",
                                            "isFinancialResult": "",
                                            "isConvictedResult": "",
                                            "isAvailableForCourtExtract": "",
                                            "orderedDate": "",
                                            "category": "",
                                            "resultText": "",
                                            "terminatesOffenceProceedings": "",
                                            "isLifeDuration": "",
                                            "isPublishedAsAPrompt": "",
                                            "isExcludedFromResults": "",
                                            "isAlwaysPublished": "",
                                            "isUrgent": "",
                                            "isD20": ""
                                        }
                                    ],

                                }
                            ],
                        },
                        {
                            "id": "ded76309-c912-436b-a21b-a4c4450bc052",
                            "masterDefendantId": "ded76309-c912-436b-a21b-a4c4450bc052",
                            "prosecutionCaseId": "c10e3b71-6a6d-45ef-9b62-34df4d54971a",

                            "offences": [
                                {

                                    "judicialResults": [
                                        {
                                            "judicialResultId": "c6d26764-5b6a-4e30-8860-2e1093f89a6e",
                                            "orderedHearingId": "f9f41ca5-a963-46ec-b2c2-a307883c1db5",
                                            "label": "Imprisonment",
                                            "isAdjournmentResult": "",
                                            "isFinancialResult": "",
                                            "isConvictedResult": "",
                                            "isAvailableForCourtExtract": "",
                                            "orderedDate": "",
                                            "category": "",
                                            "resultText": "",
                                            "terminatesOffenceProceedings": "",
                                            "isLifeDuration": "",
                                            "isPublishedAsAPrompt": "",
                                            "isExcludedFromResults": "",
                                            "isAlwaysPublished": "",
                                            "isUrgent": "",
                                            "isD20": ""
                                        },
                                        {
                                            "judicialResultId": "b8f91621-135a-4c26-8b38-e83dbeda0b3f",
                                            "orderedHearingId": "7669d6a3-ef75-40c7-ac13-8be3a9875c62",
                                            "label": "Financial Penalty",
                                            "isAdjournmentResult": "",
                                            "isFinancialResult": "",
                                            "isConvictedResult": "",
                                            "isAvailableForCourtExtract": "",
                                            "orderedDate": "",
                                            "category": "",
                                            "resultText": "",
                                            "terminatesOffenceProceedings": "",
                                            "isLifeDuration": "",
                                            "isPublishedAsAPrompt": "",
                                            "isExcludedFromResults": "",
                                            "isAlwaysPublished": "",
                                            "isUrgent": "",
                                            "isD20": ""
                                        }
                                    ],

                                }
                            ],

                        }
                    ]
                }

            ]
        }
    };

    test('it should return the correct output', () => {

        const mockedHearingResulted = getMockedHearingResulted();

        const service = new DefendantContextBaseService(mockedHearingResulted);

        const defendantBaseList = service.getDefendantContextBaseList();

        expect(defendantBaseList.length).toBe(2);

    });

    test('it should return DefendantContextBaseService with right judicial results', () => {
        const mockedHearingResulted = require('./hearing-results.json');
        const defendantBaseList = new DefendantContextBaseService(
            mockedHearingResulted).getDefendantContextBaseList();
        expect(defendantBaseList.length).toBe(1);
        expect(defendantBaseList[0].results.length).toBe(4);
    });

    test('it should return DefendantContextBaseService with application judicial results', () => {
        const mockedHearingResulted = require('./linked-application');
        const defendantBaseList = new DefendantContextBaseService(
            mockedHearingResulted).getDefendantContextBaseList();
        expect(defendantBaseList.length).toBe(1);
        expect(defendantBaseList[0].results.length).toBe(1);
    });
});
