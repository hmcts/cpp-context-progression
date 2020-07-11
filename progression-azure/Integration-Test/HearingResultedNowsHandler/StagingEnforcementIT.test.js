const RedisClient = require('../Helper/RedisClient');
const config = require('../config');
const {default: axiosStatic} = require('axios');

describe('Compliance and Enforcement IT', () => {

    let redisClient;

    beforeAll(() => {
        redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
    });

    afterAll(() => {
        redisClient.quit();
    });

    test('Hearing resulted with Fine and Surcharge', async () => {
        redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./HearingResultWithFineAndSurcharge');
        const hearingId = hearingJson.hearing.hearing.id;
        updateOrderDate(hearingJson.hearing.hearing);
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Multi-Defendants', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./Multi-Defendant-2');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Defendant Attendance', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./defendantAttendance');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Counsel Details', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./counsel-details');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with parent-guardian', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./parent-guardian');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Notified Plea', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./NotifiedPlea');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Plea', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./plea');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Community Order', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./public-functional-community-order');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Multi-Offence', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./multi-offence');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Multi-Offence 2', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./multi-offence-2');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Youth rehabilitation order', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./Youth-rehabilitation-order');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted with Crown court order', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./Crown-court-financial');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted Linked Application', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./LinkedApplication');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted Linked Application 2', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./LinkedApplication-2');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted Linked Application 3', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./LinkedApplication-3');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    test('Hearing resulted Defendant is Respondents', async () => {
        const redisClient = new RedisClient(config.REDIS_HOST, config.REDIS_PORT, config.REDIS_KEY);
        const hearingJson = require('./Defendant-Is-Respondents');
        const hearingId = hearingJson.hearing.hearing.id;
        const redisKey = 'INT_' + hearingId + '_result_';
        redisClient.addKey(redisKey, JSON.stringify(hearingJson.hearing)).then(console.log).catch(console.error);
        const statusQueryGetUri = await callOrchestrator(hearingId);
        await checkOrchestratorStatus(statusQueryGetUri);
    });

    const callOrchestrator = async function (hearingId) {
        const orchestrationEndpoint = 'http://localhost:7071/api/orchestrators/HearingResultedNowsHandler';
        console.log(`Calling command ${orchestrationEndpoint}`);
        const payload = {
            'hearingId': hearingId,
            'cjscppuid': 'a085e359-6069-4694-8820-7810e7dfe762'
        };
        try {
            return await axiosStatic.post(orchestrationEndpoint, payload, {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'cjscppuid': '7aee5dea-b0de-4604-b49b-86c7788cfc4b'
                },
            }).then(function (response) {
                console.log('statusQueryGetUri ==> ' + response.data.statusQueryGetUri);
                return response.data.statusQueryGetUri;
            }).catch(function (error) {
                console.log('Error ==> ' + error);
            });
        } catch (err) {
            console.log(`Unexpected error occurred invoking enforcement ${err}`);
        }
    };

    const checkOrchestratorStatus = async function(statusQueryGetUri) {
        await new Promise(r => setTimeout(r, 15000));

        let status = 'Pending';
        while(status !== 'Completed') {
            status = await checkStatus(statusQueryGetUri);
            console.log('status ' + status);
            await new Promise(r => setTimeout(r, 15000));
        }
    };

    const checkStatus = async function (url) {
        console.log(`Calling command ${url}`);
        try {
            return await axiosStatic.get(url, {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'cjscppuid': '7aee5dea-b0de-4604-b49b-86c7788cfc4b'
                },
            }).then(function (response) {
                console.log('Data ==> ' + JSON.stringify(response.data));
                return response.data.runtimeStatus;
            }).catch(function (error) {
                console.log('Error ==> ' + error);
            });
        } catch (err) {
            console.log(`Unexpected error occurred while checking status ${err}`);
        }
    };

    function setOrderDate(judicialResult) {
        judicialResult.orderedDate = new Date().toISOString().slice(0, 10);
    }

    function setJudicialResultsOrderDate(judicialResults) {
        if(judicialResults) {
            judicialResults.forEach(judicialResult => {
                setOrderDate(judicialResult);
            });
        }
    }

    function updateOrderDate(hearing) {
        if(hearing.defendantJudicialResults && hearing.defendantJudicialResults.length) {
            hearing.defendantJudicialResults.forEach(defendantJudicialResult => {
                setOrderDate(defendantJudicialResult.judicialResult);
            });
        }
        if(hearing.courtApplications && hearing.courtApplications.length) {
            hearing.courtApplications.forEach(courtApplication => {
                if(courtApplication.judicialResults && courtApplication.judicialResults.length) {
                    setJudicialResultsOrderDate(courtApplication.judicialResults);
                }
            });
        }
        if(hearing.prosecutionCases && hearing.prosecutionCases.length) {
            hearing.prosecutionCases.forEach(prosecutionCase => {
                prosecutionCase.defendants.forEach(defendant => {
                    if(defendant.defendantCaseJudicialResults && defendant.defendantCaseJudicialResults.length) {
                        setJudicialResultsOrderDate(defendant.judicialResults);
                    }

                    defendant.offences.forEach(offence => {
                        if(offence.judicialResults && offence.judicialResults.length) {
                            setJudicialResultsOrderDate(offence.judicialResults);
                        }
                    });
                });
            });
        }
    }
});
