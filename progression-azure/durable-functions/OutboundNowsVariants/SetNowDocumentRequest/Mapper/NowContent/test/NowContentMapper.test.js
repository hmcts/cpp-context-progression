const NowContentMapper = require('../NowContentMapper');
const context = require('../../../../../testing/defaultContext');
const uuidv4 = require('uuid/v4');

describe('Now Content mapper builds correctly', () => {

    const organisationUnitJson = require('./organisation-unit.json');
    const enforcementArea = require('./enforcement-area.json');

    test('build now content with all values', () => {
        const hearingJson = require('../test/hearing.json');
        const nowVariantJson = require('../test/now-variant.json');

        const enforcementAreaByPostCodeMap = new Map();
        enforcementAreaByPostCodeMap.set('SW99 1AA', enforcementArea);
        const nowContentMapper = new NowContentMapper(uuidv4(), nowVariantJson, hearingJson, organisationUnitJson, enforcementArea, enforcementAreaByPostCodeMap, context);
        const nowContent = nowContentMapper.buildNowContent();
        expect(nowContent.orderName).toBe("Warrant for custodial sentence");
        expect(nowContent.welshOrderName).toBe(undefined);
        expect(nowContent.orderDate).toBe("2019-03-21");
        expect(nowContent.amendmentDate).toBe("2019-03-25");
        expect(nowContent.civilNow).toBe(false);
        expect(nowContent.courtClerkName).toBe("Helen Roberts");
        expect(nowContent.orderAddressee.name).toBe("Fred Smith");
        expect(nowContent.orderAddressee.address.line1).toBe('Flat 1');
        expect(nowContent.orderingCourt.ljaCode).toBe('1800');
        expect(nowContent.orderingCourt.ljaName).toBe('East Hampshire Magistrates\' Court');
        expect(nowContent.orderingCourt.welshLjaName).toBe('East Hampshire Magistrates\' Court');
        expect(nowContent.orderingCourt.courtCentreName).toBe('Lavender Hill');
        expect(nowContent.orderingCourt.welshCourtCentreName).toBe('Lavender Hill');
        expect(nowContent.orderingCourt.address.line1).toBe('Flat 1');
        expect(nowContent.defendant.name).toBe('Fred Smith');
        expect(nowContent.defendant.address.line1).toBe('Flat 1');
        expect(nowContent.applicants[0].name).toBe('Fred Smith');
        expect(nowContent.applicants[0].address.line1).toBe('Flat 1');
        expect(nowContent.respondents[0].name).toBe('Fred Smith');
        expect(nowContent.respondents[0].address.line1).toBe('Flat 1');
        expect(nowContent.nextHearingCourtDetails.courtName).toBe('Wood Green Crown Court');
        expect(nowContent.nowText[0].label).toBe('orderText');
        expect(nowContent.nowText[0].value).toBe('<p><strong>Direction</strong></p><p>The authorised officers of the prisoner escort contractors shall take the defendant to the nominated prison establishment and deliver the defendant to the Governor who shall detain the defendant for the overall length of the sentence shown.</p><p><strong>Note: Prisoner Voting Rights</strong></p> <p><span>Convicted offenders sentenced to imprisonment lose the right to vote while they are detained in custody</span></p> <p><strong>Additional Note</strong></p> <p>Previous Convictions and Pre-Sentence Report available separately <span>&nbsp;</span></p>');
        expect(nowContent.nowText[1].label).toBe('orderText1');
        expect(nowContent.cases.length).toBe(1);
        expect(nowContent.cases[0].reference).toBe("TFL4359536");
        expect(nowContent.cases[0].caseMarkers[0]).toBe("WP");
        expect(nowContent.cases[0].caseMarkers[1]).toBe("PN");
        expect(nowContent.cases[0].defendantCaseOffences[0].results[0].label).toBe("Financial Penalty");
        expect(nowContent.cases[0].defendantCaseOffences[0].alcoholReadingAmount).toBe(100);
        expect(nowContent.cases[0].defendantCaseOffences[0].alcoholReadingMethodCode).toBe("A");
        expect(nowContent.cases[0].defendantCaseOffences[0].alcoholReadingMethodDescription).toBe("Blood");
        expect(nowContent.distinctResults.length).toBe(2);
        expect(nowContent.distinctResults[0].label).toBe("Imprisonment");
        expect(nowContent.distinctResults[0].nowRequirementText[0].label).toBe("collOrd");
        expect(nowContent.distinctResults[0].nowRequirementText[0].value).toBe("<h4>COLLECTION ORDER</h4> <p><strong>A collection order has been made, which means the court has powers to collect the money</strong></p> <p><strong>See above for how to pay</strong></p> <p>If you don&rsquo;t pay, we may:</p> <ul> <li>take money from your earnings or benefits</li> <li>increase your fine</li> <li>issue a warrant to seize your possessions which will mean extra costs</li> <li>log this in the Register of Judgements, Orders and Fines, making it harder for you to get credit</li> <li>apply to the court for a warrant for your arrest to bring you to court</li> </ul> <p><strong>If you still don’t pay, you could be sent to prison for non-payment.</strong></p>");
        expect(nowContent.distinctResults[1].label).toBe("Financial Penalty");
        expect(nowContent.distinctResults[1].nowRequirementText[0].label).toBe("collOrd2");
        expect(nowContent.distinctResults[1].nowRequirementText[0].value).toBe("TEXT");
        expect(nowContent.distinctPrompts[1].value).toBe("20");
        expect(nowContent.distinctPrompts[2].value).toBe("30");
        expect(nowContent.nowRequirementText[0].label).toBe('collOrd');
        expect(nowContent.nowRequirementText[0].value).toBe("<h4>COLLECTION ORDER</h4> <p><strong>A collection order has been made, which means the court has powers to collect the money</strong></p> <p><strong>See above for how to pay</strong></p> <p>If you don&rsquo;t pay, we may:</p> <ul> <li>take money from your earnings or benefits</li> <li>increase your fine</li> <li>issue a warrant to seize your possessions which will mean extra costs</li> <li>log this in the Register of Judgements, Orders and Fines, making it harder for you to get credit</li> <li>apply to the court for a warrant for your arrest to bring you to court</li> </ul> <p><strong>If you still don’t pay, you could be sent to prison for non-payment.</strong></p>");
        expect(nowContent.nowRequirementText[1].label).toBe('collOrd2');
        expect(nowContent.nowRequirementText[1].value).toBe("TEXT");
        expect(nowContent.defendant.solicitor.name).toBe('Sonja & Co LLP');
        expect(nowContent.defendant.solicitor.address.line1).toBe('Legal House');
    });

    test('Test defendant with multi offences and multi result prompts', () => {
        const hearingJson = require('./hearing-multi-ofences.json');
        const nowVariantJson = require('./now-variant-multi-prompts.json');

        const enforcementAreaByPostCodeMap = new Map();
        enforcementAreaByPostCodeMap.set('SW99 1AA', enforcementArea);
        const nowContentMapper = new NowContentMapper(uuidv4(), nowVariantJson, hearingJson,
                                                      organisationUnitJson, enforcementArea,
                                                      enforcementAreaByPostCodeMap, context);
        const nowContent = nowContentMapper.buildNowContent();
        //RESULTS
        expect(nowContent.distinctResults.length).toBe(1);
        expect(nowContent.distinctResults[0].label).toBe("Interim criminal behaviour order made until further order");
        expect(nowContent.distinctResults[0].welshLabel).toBeUndefined();
        expect(nowContent.distinctResults[0].resultIdentifier).toBe("c2fa19f6-dabf-43fc-9f12-2347fc9b8cc5");
        expect(nowContent.distinctResults[0].publishedForNows).toBeTruthy();
        expect(nowContent.distinctResults[0].resultDefinitionGroup).toBe("result definition group");
        expect(nowContent.distinctResults[0].nowRequirementText).toBeUndefined();
        expect(nowContent.distinctResults[0].prompts.length).toBe(2);

        //PROMPTS
        expect(nowContent.distinctPrompts.length).toBe(4);

        expect(nowContent.distinctPrompts[0].label).toBe("Requirements");
        expect(nowContent.distinctPrompts[0].value).toBe("Requirements");
        expect(nowContent.distinctPrompts[0].promptIdentifier).toBe("4461f7f5-f984-4b6d-9f60-3084e5071382");
        expect(nowContent.distinctPrompts[0].promptReference).toBe("requirements");

        expect(nowContent.distinctPrompts[1].label).toBe("Requirements");
        expect(nowContent.distinctPrompts[1].value).toBe("Yes");
        expect(nowContent.distinctPrompts[1].welshValue).toBe("Ydw");
        expect(nowContent.distinctPrompts[1].promptIdentifier).toBe("4461f7f5-f984-4b6d-9f60-3084e5071382");
        expect(nowContent.distinctPrompts[1].promptReference).toBe("requirements");

        expect(nowContent.distinctPrompts[2].label).toBe("Supervisor organisation name");
        expect(nowContent.distinctPrompts[2].value).toBe("No");
        expect(nowContent.distinctPrompts[2].welshValue).toBe("Na");
        expect(nowContent.distinctPrompts[2].promptIdentifier).toBe("49806ee4-d2d8-40ce-b651-5fee1510aff4");
        expect(nowContent.distinctPrompts[2].promptReference).toBe("supervisorOrganisationName");

        expect(nowContent.distinctPrompts[3].label).toBe("Supervisor organisation name");
        expect(nowContent.distinctPrompts[3].value).toBe("organisation name1");
        expect(nowContent.distinctPrompts[3].promptIdentifier).toBe("49806ee4-d2d8-40ce-b651-5fee1510aff4");
        expect(nowContent.distinctPrompts[3].promptReference).toBe("supervisorOrganisationName");
    });
});