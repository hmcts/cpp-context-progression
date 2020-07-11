const setInformantRegister = require('../index');
const context = require('../../testing/defaultContext');

describe('Set Informant Register', () => {

    const hearingJson = require('./hearing-results-for-informant-register.json');

    test('should return the correct court Informant fragment', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: hearingJson,
                sharedTime: '2020-06-01T10:00:00Z'
            }
        };

        const informantRegisterFragments = await setInformantRegister(context);

        expect(informantRegisterFragments.length).toBe(2);
        expect(informantRegisterFragments[0].registerDefendants.length).toBe(2);
        expect(informantRegisterFragments[0].registerDefendants[0].results[0].judicialResult.publishedForNows).toBe(false);
        expect(informantRegisterFragments[0].registerDefendants[1].results[0].judicialResult.publishedForNows).toBe(false);
        expect(informantRegisterFragments[0].registerDate).toBe('2020-06-01T11:00:00Z');
        expect(informantRegisterFragments[0].hearingDate).toBe('2020-01-20T00:00:00Z');
        expect(informantRegisterFragments[0].hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
    });

    test('should filter unique prosecutionAuthId', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: {
                    prosecutionCases: [
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-1'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-1',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        },
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-2'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-2',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        }
                    ],
                    "courtCentre": {
                        "id": "6647df67-a065-4d07-90ba-a8daa064ecd9",
                        "name": "Lavender Hill"
                    }
                }
            }
        };

        const informantRegisterFragments = await setInformantRegister(context);

        expect(informantRegisterFragments[0].registerDefendants.length).toBe(1);
        expect(informantRegisterFragments[0].registerDefendants[0].masterDefendantId).toBe('master-defendant-id-1');
        expect(informantRegisterFragments[1].registerDefendants.length).toBe(1);
        expect(informantRegisterFragments[1].registerDefendants[0].masterDefendantId).toBe('master-defendant-id-2');
    });

    test('should filter unique prosecutionAuthId but not master defendant id', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: {
                    prosecutionCases: [
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-1'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-1',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        },
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-2'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-1',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        }
                    ],
                    "courtCentre": {
                        "id": "6647df67-a065-4d07-90ba-a8daa064ecd9",
                        "name": "Lavender Hill"
                    }
                }
            }
        };

        const informantRegisterFragments = await setInformantRegister(context);

        expect(informantRegisterFragments.length).toBe(2);
        expect(informantRegisterFragments[0].registerDefendants.length).toBe(1);
        expect(informantRegisterFragments[0].registerDefendants[0].masterDefendantId).toBe('master-defendant-id-1');
        expect(informantRegisterFragments[1].registerDefendants.length).toBe(1);
        expect(informantRegisterFragments[1].registerDefendants[0].masterDefendantId).toBe('master-defendant-id-1');
    });

    test('should filter out the defendant of the cases with different prosecutionAuthId', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: {
                    prosecutionCases: [
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-1'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-1',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        },
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-1'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-2',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        }
                    ],
                    "courtCentre": {
                        "id": "6647df67-a065-4d07-90ba-a8daa064ecd9",
                        "name": "Lavender Hill"
                    }
                }
            }
        };

        const informantRegisterFragments = await setInformantRegister(context);

        expect(informantRegisterFragments.length).toBe(1);
        expect(informantRegisterFragments[0].registerDefendants.length).toBe(2);
    });

    test('should return the correct master defendant for each prosecution Authority', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: {
                    prosecutionCases: [
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-1'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-1',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        },
                        {
                            prosecutionCaseIdentifier: {
                                prosecutionAuthorityId: 'prosecution-auth-id-1'
                            },
                            defendants: [
                                {
                                    masterDefendantId: 'master-defendant-id-1',
                                    defendantCaseJudicialResults: [],
                                    offences: []
                                }
                            ]
                        }
                    ],
                    "courtCentre": {
                        "id": "6647df67-a065-4d07-90ba-a8daa064ecd9",
                        "name": "Lavender Hill"
                    }
                }
            }
        };

        const informantRegisterFragments = await setInformantRegister(context);

        expect(informantRegisterFragments.length).toBe(1);
        expect(informantRegisterFragments[0].registerDefendants.length).toBe(1);

    });
});