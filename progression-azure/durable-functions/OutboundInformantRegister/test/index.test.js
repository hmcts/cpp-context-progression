const OutboundInformantRegister = require('../index');
const context = require('../../testing/defaultContext');

describe('Outbound informant register correctly', () => {
    test('build informant register request', async ()=>{
        const hearingJson = require('./hearing.json');
        const informantRegisterSubscriptions = require('./informantRegisterSubscriptions.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson,
                informantRegisterSubscriptions: informantRegisterSubscriptions
            }
        };

        const outboundInformantRegister = await OutboundInformantRegister(context);

        expect(outboundInformantRegister[0].registerDate).toBe('2020-01-20T11:00:00Z');
        expect(outboundInformantRegister[0].hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
        expect(outboundInformantRegister[0].hearingDate).toBe('2020-01-20T10:00:00Z');
        expect(outboundInformantRegister[0].prosecutionAuthorityId).toBe('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        expect(outboundInformantRegister[0].prosecutionAuthorityCode).toBe('TFL');
        expect(outboundInformantRegister[0].majorCreditorCode).toBe('OUCode');
        expect(outboundInformantRegister[0].prosecutionAuthorityName).toBe('Barnet county court');
        expect(outboundInformantRegister[0].fileName).toBe('InformantRegister_TFL_2020-01-20.csv');

        expect(outboundInformantRegister[0].recipients[0].recipientName).toBe('Fred Smith');
        expect(outboundInformantRegister[0].recipients[0].emailAddress1).toBe('test@hmcst.net');
        expect(outboundInformantRegister[0].recipients[0].emailTemplateName).toBe('ir_standard');
        expect(outboundInformantRegister[0].hearingVenue.courtSessions[0].defendants[0].prosecutionCasesOrApplications.length).toBe(1);
        expect(outboundInformantRegister[0].hearingVenue.courtSessions[0].defendants[0].prosecutionCasesOrApplications[0].caseOrApplicationReference).toBe('TFL4359536');
        expect(outboundInformantRegister[0].hearingVenue.courtSessions[0].defendants[1].prosecutionCasesOrApplications.length).toBe(1);
        expect(outboundInformantRegister[0].hearingVenue.courtSessions[0].defendants[1].prosecutionCasesOrApplications[0].caseOrApplicationReference).toBe('TFL4359536');

    });
});