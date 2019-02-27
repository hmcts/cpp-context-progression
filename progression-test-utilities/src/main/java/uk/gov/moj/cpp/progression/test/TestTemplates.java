package uk.gov.moj.cpp.progression.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.DocumentationLanguage;
import uk.gov.justice.core.courts.FinancialOrderDetails;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Now;
import uk.gov.justice.core.courts.NowType;
import uk.gov.justice.core.courts.NowVariant;
import uk.gov.justice.core.courts.NowVariantAddressee;
import uk.gov.justice.core.courts.NowVariantDefendant;
import uk.gov.justice.core.courts.NowVariantKey;
import uk.gov.justice.core.courts.NowVariantResult;
import uk.gov.justice.core.courts.NowVariantResultText;
import uk.gov.justice.core.courts.ResultPrompt;
import uk.gov.justice.core.courts.SharedResultLine;

import java.math.BigDecimal;
import java.util.UUID;

@SuppressWarnings("squid:S1188")
public class TestTemplates {

    private static final String DAVID = "David";
    private static final String BOWIE = "Bowie";

    private static final String IMPRISONMENT_LABEL = "Imprisonment";
    private static final String IMPRISONMENT_DURATION_VALUE = "Imprisonment duration";
    private static final String WORMWOOD_SCRUBS_VALUE = "Wormwood Scrubs";

    private TestTemplates() {
    }

    public static CreateNowsRequest generateNowsRequestTemplate(final UUID defendantId) {
        return generateNowsRequestTemplate(defendantId, JurisdictionType.CROWN, false);
    }

    public static CreateNowsRequest generateNowsRequestTemplate(final UUID defendantId, final JurisdictionType jurisdictionType, boolean convicted) {
        return generateNowsRequestTemplate(defendantId, jurisdictionType, convicted, false);
    }


        public static CreateNowsRequest generateNowsRequestTemplate(final UUID defendantId, final JurisdictionType jurisdictionType, boolean convicted, boolean remotePrintingRequired) {
        final UUID caseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID nowsTypeId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final UUID sharedResultLineId0 = UUID.randomUUID();
        final UUID sharedResultLineId1 = UUID.randomUUID();
        final UUID sharedResultLineId2 = UUID.randomUUID();
        final UUID promptId00 = UUID.randomUUID();
        final UUID promptId01 = UUID.randomUUID();
        final UUID promptId10 = UUID.randomUUID();
        final UUID promptId11 = UUID.randomUUID();
        final UUID promptId20 = UUID.randomUUID();
        final UUID promptId21 = UUID.randomUUID();
        final String promptLabel0 = "Imprisonment Duration";
        final String promptLabel1 = "Prison";
        final String orderedDate = "2012-11-11";
        final DelegatedPowers courtClerk = DelegatedPowers.delegatedPowers()
                .withFirstName(DAVID)
                .withLastName(BOWIE)
                .withUserId(UUID.randomUUID())
                .build();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(jurisdictionType)
                .setStructure(toMap(caseId, toMap(defendantId, singletonList(offenceId))))
                .setConvicted(convicted))
                .build();

        final String templateName = STRING.next();
        return CreateNowsRequest.createNowsRequest()
                .withHearing(hearing)
                .withCourtClerk(courtClerk)
                .withSharedResultLines(asList(
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId0)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId))
                                .withLevel("CASE")
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.ONE))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId00)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId01)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build(),
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId1)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId))
                                .withLevel("DEFENDANT")
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.valueOf(2)))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId10)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId11)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build(),
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId2)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId))
                                .withLevel("OFFENCE")
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.valueOf(3)))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId20)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId21)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build()

                ))
                .withNows(singletonList(Now.now()
                        .withId(UUID.randomUUID())
                        .withNowsTypeId(nowsTypeId)
                        .withDefendantId(defendantId)
                        .withDocumentationLanguage(DocumentationLanguage.ENGLISH)
                        .withRequestedMaterials(singletonList(
                                NowVariant.nowVariant()
                                        .withMaterialId(materialId)
                                        .withIsAmended((true))
                                        .withTemplateName(templateName)
                                        .withStatus(STRING.next())
                                        .withDescription(STRING.next())
                                        .withKey(NowVariantKey.nowVariantKey()
                                                .withDefendantId(defendantId)
                                                .withHearingId(hearing.getId())
                                                .withNowsTypeId(nowsTypeId)
                                                .withUsergroups(asList("Listing Officers", "Crown Court Admin"))
                                                .build())
                                        .withNowResults(asList(
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId0)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId00, promptId01))
                                                        .build(),
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId1)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId10, promptId11)
                                                        ).build(),
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId2)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId20, promptId01)
                                                        ).build()
                                        ))
                                        .withNowVariantAddressee(
                                                NowVariantAddressee.nowVariantAddressee()
                                                        .withAddress(Address.address()
                                                                .withAddress1(STRING.next())
                                                                .build())
                                                        .withName(STRING.next())
                                                        .build())
                                        .withNowVariantDefendant(NowVariantDefendant.nowVariantDefendant()
                                                .withName(STRING.next())
                                                .withAddress(Address.address()
                                                        .withAddress1(STRING.next())
                                                        .build())
                                                .build())
                                        .withIsRemotePrintingRequired(remotePrintingRequired)
                                        .build()
                        ))

                        .withLjaDetails(LjaDetails.ljaDetails()
                                .withLjaCode(STRING.next())
                                .withLjaName(STRING.next())
                                .withAccountDivisionCode(STRING.next())
                                .withBacsAccountNumber(STRING.next())
                                .withBacsBankName(STRING.next())
                                .withBacsSortCode(STRING.next())
                                .withEnforcementAddress(Address.address()
                                        .withAddress1(STRING.next())
                                        .build())
                                .withEnforcementEmail(STRING.next() + "@gmail.com")
                                .withEnforcementPhoneNumber(STRING.next())
                                .build())
                        .build()))
                .withNowTypes(singletonList(NowType.nowType()
                                .withId(nowsTypeId)
                                .withTemplateName("SingleTemplate")
                                .withDescription("Imprisonment Order")
                                .withRank(1)
                                .withRequiresEnforcement(true)
                                .withRequiresBulkPrinting(true)
                                .withStaticText(
                                        (
                                                "<h3>Imprisonment</h3><p>You have been sentenced to a term of imprisonment. If you<ul><li>Do not comply with the requirements of this order during the <u>supervision period</u>; or</li><li>Commit any other offence during the <u>operational period</u></li></ul>you may be liable to serve the <u>custodial period</u> in prison.<br/><br/><br/><p>For the duration of the <u>supervision period</u>, you will be supervised by your Probation Officer, and<br/>You must<ul><li>Keep in touch with your Probation Officer as they tell you</li><li>Tell your Probation Officer if you intend to change your address</li><li>Comply with all other requirements</li></ul><p><strong>Requirements</strong> – Please refer only to the requirements that the court has specified in the details of your order, <u>as set out above</u><p><strong>Unpaid Work Requirement</strong><p>You must carry out unpaid work for the hours specified as you are told and by the date specified in the order. Your Probation Officer will tell you who will be responsible for supervising work.<p><strong>Activity Requirement</strong><p>You must present yourself as directed at the time and on the days specified in the order and you must undertake the activity the court has specified for the duration specified in the order in the way you are told by your Probation Officer<p><strong>Programme Requirement</strong><p>You must participate in the programme specified in the order at the location specified and for the number of days specified in the order<p><strong>Prohibited Activity Requirement</strong><p>You must not take part in the activity that the court has prohibited in the order for the number of days the court specified<p><strong>Curfew Requirement</strong><p>You must remain in the place or places the court has specified during the periods specified. The curfew requirement lasts for the number of days specified in the order<p>See \"Electronic Monitoring Provision\" in this order<p><strong>Exclusion Requirement</strong><p>You must not enter the place or places the court has specified between the hours specified in the order. The exclusion requirement lasts for the number of days specified in the order<p>See \"Electronic Monitoring Provision\" in this order<p><strong>Residence Requirement</strong><p>You must live at the premises the court has specified and obey any rules that apply there for the number of days specified in the order. You may live at ???? with the prior approval of your Probation Officer.<p><strong>Foreign Travel Prohibition Requirement</strong><p>You must not travel to the prohibited location specified in the order during the period the court has specified in the order.<p><strong>Mental Health Treatment Requirement</strong><p>You must have mental health treatment by or under the direction of the practitioner the court has specified at the location specified as a resident patient for the number of days specified in the order.<p><strong>Drug Rehabilitation Requirement</strong><p>You must have treatment for drug dependency by or under the direction of the practitioner the court has specified at the location specified as a resident patient for the number of days specified in the order.<p>To be sure that you do not have any illegal drug in your body, you must provide samples for testing at such times or in such circumstances as your Probation Officer or the person responsible for your treatment will tell you. The results of tests on the samples will be sent to your Probation Officer who will report the results to the court. Your Probation Officer will also tell the court how your order is progressing and the views of your treatment provider.<p>The court will review this order ????. The first review will be on the date and time specified at the court specified.<p>You must / need not attend this review progression.<p><strong>Alcohol Treatment Requirement</strong><p>You must have treatment for alcohol dependency by or under the direction of the practitioner the court has specified at the location specified as a resident patient for the number of days specified in the order.<p><strong>Supervision Requirement</strong><p>You must attend appointments with your Probation Officer or another person at the times and places your Probation Officer says.<p><strong>Attendance Centre Requirement</strong><p>You must attend an attendance centre - see separate sheet for details<p><strong>WARNING</strong><p>If you do not comply with your order, you will be brought back to court. The court may then<ul><li>Change the order by adding extra requirements</li><li>Pass a different sentence for the original offences; or</li><li>Send you to prison</li></ul><p><strong>NOTE</strong><p>Either you or your Probation Officer can ask the court to look again at this order and the court can then change it or cancel it if it feels that is the right thing to do. The court may also pass a different sentence for the original offence(s). If you wish to ask the court to look at your order again you should get in touch with the court at the address above.")
                                )
                                .withWelshStaticText((
                                        "<h3> Prison </h3> <p> Fe'ch dedfrydwyd i dymor o garchar. Os ydych <ul> <li> Peidiwch â chydymffurfio â gofynion y gorchymyn hwn yn ystod y cyfnod goruchwylio </u>; neu </li> <li> Ymrwymo unrhyw drosedd arall yn ystod y cyfnod gweithredol </u> </li> </ul> efallai y byddwch yn atebol i wasanaethu'r cyfnod gwarchodaeth </u> yn y carchar. <br/> <br/> <br/> <p> Yn ystod y cyfnod goruchwylio </u>, byddwch chi'n cael eich goruchwylio gan eich Swyddog Prawf, a <br/> Rhaid ichi <ul> < li> Cadwch mewn cysylltiad â'ch Swyddog Prawf wrth iddyn nhw ddweud wrthych </li> <li> Dywedwch wrth eich Swyddog Prawf os ydych yn bwriadu newid eich cyfeiriad </li> <li> Cydymffurfio â'r holl ofynion eraill </li></ul > <p> <strong> Gofynion </strong> - Cyfeiriwch yn unig at y gofynion a nododd y llys yn manylion eich archeb, fel y nodir uchod </u> <p> <strong> Gwaith Di-dāl Gofyniad </strong><p> Rhaid i chi wneud gwaith di-dāl am yr oriau a bennir fel y dywedir wrthych a chi erbyn y dyddiad a bennir yn y gorchymyn. Bydd eich Swyddog Prawf yn dweud wrthych pwy fydd yn gyfrifol am oruchwylio gwaith.<p> <strong> Gweithgaredd Gofyniad </strong> <p> Rhaid i chi gyflwyno eich hun fel y'i cyfarwyddir ar yr amser ac ar y diwrnodau a bennir yn y gorchymyn a rhaid i chi ymgymryd â chi y gweithgaredd y mae'r llys wedi ei nodi ar gyfer y cyfnod a bennir yn y drefn yn y ffordd y dywedir wrth eich Swyddog Prawf <p> <strong> Gofyniad Rhaglen </strong><p> Rhaid i chi gymryd rhan yn y rhaglen a bennir yn y drefn yn y lleoliad a bennir ac am y nifer o ddyddiau a bennir yn y gorchymyn <p> <strong> Gofyniad Gweithgaredd Gwahardd </strong> <p> Rhaid i chi beidio â chymryd rhan yn y gweithgaredd a waharddodd y llys yn y drefn ar gyfer nifer y dyddiau llys penodol <p> <strong> Curfew Requirement </strong> <p> Rhaid i chi aros yn y lle neu lle mae'r llys wedi nodi yn ystod y cyfnodau a bennir. Mae'r gofyniad cyrffyw yn para am y nifer o ddyddiau a bennir yn y<p> Gweler \"Darpariaeth Monitro Electronig\" yn yr orchymyn hwn <p> <strong> Gofyniad Preswyl </strong> <p> Rhaid i chi fyw yn yr adeilad y llys wedi nodi ac ufuddhau i unrhyw reolau sy'n berthnasol yno am y nifer o ddyddiau a bennir yn y gorchymyn. Efallai y byddwch yn byw yn ???? gyda chymeradwyaeth ymlaen llaw eich Swyddog Prawf. <p> <strong> Gofyniad Gwahardd Teithio Tramor </strong> <p> Rhaid i chi beidio â theithio i'r lleoliad gwaharddedig a bennir yn yr orchymyn yn ystod y cyfnod y mae'r llys wedi'i bennu yn y gorchymyn. < p> <strong> Gofyniad Triniaeth Iechyd Meddwl </strong> <p> Rhaid i chi gael triniaeth iechyd meddwl gan neu o dan gyfarwyddyd yr ymarferydd y mae'r llys wedi ei nodi yn y lleoliad a bennir fel claf preswyl am y nifer o ddyddiau a bennir yn y <p> <strong> Angen Adsefydlu Cyffuriau </strong> <p> Rhaid i chi gael triniaeth ar gyfer dibyniaeth ar gyffuriau gan neu o dan gyfarwyddyd yr ymarferydd y mae'r llys wedi ei nodi yn y lleoliad a bennir fel claf preswyl am nifer y dyddiau <p> Er mwyn sicrhau nad oes gennych unrhyw gyffur anghyfreithlon yn eich corff, rhaid i chi ddarparu samplau i'w profi ar yr adegau hynny neu mewn amgylchiadau o'r fath y bydd eich Swyddog Prawf neu'r person sy'n gyfrifol am eich triniaeth yn dweud wrthych chi . Anfonir canlyniadau'r profion ar y samplau i'ch Swyddog Prawf a fydd yn adrodd y canlyniadau i'r llys. Bydd eich Swyddog Prawf hefyd yn dweud wrth y llys sut mae'ch gorchymyn yn mynd rhagddo a barn eich darparwr triniaeth. <P> Bydd y llys yn adolygu'r gorchymyn hwn ????. Bydd yr adolygiad cyntaf ar y dyddiad a'r amser a bennir yn y llys a bennir. <P> Rhaid i chi / nid oes angen i chi fynychu'r gwrandawiad hwn. <P> <strong> Gofyniad Trin Alcohol </strong> <p> Rhaid i chi gael triniaeth ar gyfer dibyniaeth ar alcohol gan neu o dan gyfarwyddyd yr ymarferydd y mae'r llys wedi ei nodi yn y lleoliad a bennir fel claf preswyl am y nifer o ddyddiau a bennir yn y gorchymyn. <p> <strong> Gofyniad Goruchwylio </strong> <p> Rhaid i chi fynychu penodiadau gyda'ch Swyddog Prawf neu berson arall ar yr adegau a lle mae eich Swyddog Prawf yn dweud. <p> <strong> Gofyniad y Ganolfan Bresennol </strong> <p> Rhaid i chi fynychu canolfan bresenoldeb - <p> <strong> RHYBUDD </strong> <p> Os na fyddwch chi'n cydymffurfio â'ch archeb, fe'ch cewch eich troi'n ôl i'r llys. Gall y llys wedyn <ul> <li> Newid y gorchymyn trwy ychwanegu gofynion ychwanegol </li> <li> Pasiwch frawddeg wahanol ar gyfer y troseddau gwreiddiol; neu </li> <li> Anfonwch chi at y carchar </li> </ul> <p> <strong> NOTE </strong> <p> Naill ai chi neu'ch Swyddog Prawf all ofyn i'r llys edrych eto ar y gorchymyn hwn ac yna gall y llys ei newid neu ei ganslo os yw'n teimlo mai dyna'r peth iawn i'w wneud. Gall y llys hefyd basio brawddeg wahanol ar gyfer y trosedd (wyr) gwreiddiol. Os hoffech ofyn i'r llys edrych ar eich archeb eto dylech gysylltu â'r llys yn y cyfeiriad uchod. ")
                                )
                                .withPriority(("30"))
                                .withJurisdiction("B")
                                .build()
                        )
                )
                .build();
    }


    public static CreateNowsRequest generateNowsRequestTemplateWithConditionalText(final UUID defendantId, final JurisdictionType jurisdictionType, boolean convicted) {
        final UUID caseId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final UUID nowsTypeId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final UUID sharedResultLineId0 = UUID.randomUUID();
        final UUID sharedResultLineId1 = UUID.randomUUID();
        final UUID sharedResultLineId2 = UUID.randomUUID();
        final UUID sharedResultLineId3 = UUID.randomUUID();
        final UUID promptId00 = UUID.randomUUID();
        final UUID promptId01 = UUID.randomUUID();
        final UUID promptId10 = UUID.randomUUID();
        final UUID promptId11 = UUID.randomUUID();
        final UUID promptId20 = UUID.randomUUID();
        final UUID promptId21 = UUID.randomUUID();
        final String promptLabel0 = "Imprisonment Duration";
        final String promptLabel1 = "Prison";
        final String orderedDate = "2012-11-11";
        final DelegatedPowers courtClerk = DelegatedPowers.delegatedPowers()
                .withFirstName(DAVID)
                .withLastName(BOWIE)
                .withUserId(UUID.randomUUID())
                .build();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(jurisdictionType)
                .setStructure(toMap(caseId, toMap(defendantId, asList(offenceId1))))
                .setConvicted(convicted))
                .build();

        final String templateName = "NoticeOrderWarrants";
        final String offence = "OFFENCE";
        return CreateNowsRequest.createNowsRequest()
                .withHearing(hearing)
                .withCourtClerk(courtClerk)
                .withSharedResultLines(asList(
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId0)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId1))
                                .withLevel("CASE")
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.ONE))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId00)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId01)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build(),
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId1)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId1))
                                .withLevel("DEFENDANT")
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.valueOf(2)))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId10)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId11)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build(),
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId2)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId1))
                                .withLevel(offence)
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.valueOf(3)))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId20)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId21)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build(),
                        SharedResultLine.sharedResultLine()
                                .withId(sharedResultLineId3)
                                .withProsecutionCaseId((caseId))
                                .withDefendantId((defendantId))
                                .withOffenceId((offenceId2))
                                .withLevel(offence)
                                .withLabel(IMPRISONMENT_LABEL)
                                .withRank((BigDecimal.valueOf(3)))
                                .withOrderedDate((orderedDate))
                                .withCourtClerk(courtClerk)
                                .withIsAvailableForCourtExtract(true)
                                .withPrompts(asList(
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId20)
                                                .withLabel(promptLabel0)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(IMPRISONMENT_DURATION_VALUE).build(),
                                        ResultPrompt.resultPrompt()
                                                .withId(promptId21)
                                                .withLabel(promptLabel1)
                                                .withIsAvailableForCourtExtract(true)
                                                .withValue(WORMWOOD_SCRUBS_VALUE).build()
                                        )
                                ).build()
                ))
                .withNows(singletonList(Now.now()
                        .withId(UUID.randomUUID())
                        .withNowsTypeId(nowsTypeId)
                        .withDefendantId(defendantId)
                        .withDocumentationLanguage(DocumentationLanguage.ENGLISH)
                        .withFinancialOrders(FinancialOrderDetails.financialOrderDetails()
                                .withAccountReference(STRING.next())
                                .withIsCrownCourt(true)
                                .withTotalAmountImposed(STRING.next())
                                .withTotalBalance(STRING.next())
                                .withPaymentTerms(STRING.next())
                                .build())
                        .withRequestedMaterials(singletonList(
                                NowVariant.nowVariant()
                                        .withMaterialId(materialId)
                                        .withIsAmended((true))
                                        .withTemplateName(templateName)
                                        .withStatus(STRING.next())
                                        .withDescription(STRING.next())
                                        .withKey(NowVariantKey.nowVariantKey()
                                                .withDefendantId(defendantId)
                                                .withHearingId(hearing.getId())
                                                .withNowsTypeId(nowsTypeId)
                                                .withUsergroups(asList("Listing Officers", "Crown Court Admin"))
                                                .build())
                                        .withNowResults(asList(
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId0)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId00, promptId01))
                                                        .build(),
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId1)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId10, promptId11))
                                                        .build(),
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId2)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId20, promptId01))
                                                        .withNowVariantResultText((NowVariantResultText
                                                                .nowVariantResultText()
                                                                .withAdditionalProperty("ABCD", "1234")
                                                                .withAdditionalProperty("1234", "ABCD")
                                                                .build()))
                                                        .build(),
                                                NowVariantResult.nowVariantResult()
                                                        .withSharedResultId(sharedResultLineId3)
                                                        .withSequence(1)
                                                        .withPromptRefs(asList(promptId20, promptId01))
                                                        .withNowVariantResultText((NowVariantResultText
                                                                .nowVariantResultText()
                                                                .withAdditionalProperty("ABCD", "1234")
                                                                .withAdditionalProperty("1234", "ABCD")
                                                                .build()))
                                                        .build()
                                        ))
                                        .withNowVariantAddressee(
                                                NowVariantAddressee.nowVariantAddressee()
                                                        .withAddress(Address.address()
                                                                .withAddress1(STRING.next())
                                                                .build())
                                                        .withName(STRING.next())
                                                        .build())
                                        .withNowVariantDefendant(NowVariantDefendant.nowVariantDefendant()
                                                .withName(STRING.next())
                                                .withDateOfBirth(("1977-01-01"))
                                                .withAddress(Address.address()
                                                        .withAddress1(STRING.next())
                                                        .build())
                                                .build())
                                        .withIsRemotePrintingRequired(BOOLEAN.next())
                                        .build()
                        ))

                        .withLjaDetails(LjaDetails.ljaDetails()
                                .withLjaCode(STRING.next())
                                .withLjaName(STRING.next())
                                .withAccountDivisionCode(STRING.next())
                                .withBacsAccountNumber(STRING.next())
                                .withBacsBankName(STRING.next())
                                .withBacsSortCode(STRING.next())
                                .withEnforcementAddress(Address.address()
                                        .withAddress1(STRING.next())
                                        .build())
                                .withEnforcementEmail(STRING.next()+"@gmail.com")
                                .withEnforcementPhoneNumber(STRING.next())
                                .build())
                        .build()))
                .withNowTypes(singletonList(NowType.nowType()
                                .withId(nowsTypeId)
                                .withTemplateName("SingleTemplate")
                                .withDescription("Imprisonment Order")
                                .withRank(1)
                                .withRequiresEnforcement(true)
                                .withRequiresBulkPrinting(true)
                                .withStaticText(
                                        (
                                                "<h3>Imprisonment</h3><p>You have been sentenced to a term of imprisonment. If you<ul><li>Do not comply with the requirements of this order during the <u>supervision period</u>; or</li><li>Commit any other offence during the <u>operational period</u></li></ul>you may be liable to serve the <u>custodial period</u> in prison.<br/><br/><br/><p>For the duration of the <u>supervision period</u>, you will be supervised by your Probation Officer, and<br/>You must<ul><li>Keep in touch with your Probation Officer as they tell you</li><li>Tell your Probation Officer if you intend to change your address</li><li>Comply with all other requirements</li></ul><p><strong>Requirements</strong> – Please refer only to the requirements that the court has specified in the details of your order, <u>as set out above</u><p><strong>Unpaid Work Requirement</strong><p>You must carry out unpaid work for the hours specified as you are told and by the date specified in the order. Your Probation Officer will tell you who will be responsible for supervising work.<p><strong>Activity Requirement</strong><p>You must present yourself as directed at the time and on the days specified in the order and you must undertake the activity the court has specified for the duration specified in the order in the way you are told by your Probation Officer<p><strong>Programme Requirement</strong><p>You must participate in the programme specified in the order at the location specified and for the number of days specified in the order<p><strong>Prohibited Activity Requirement</strong><p>You must not take part in the activity that the court has prohibited in the order for the number of days the court specified<p><strong>Curfew Requirement</strong><p>You must remain in the place or places the court has specified during the periods specified. The curfew requirement lasts for the number of days specified in the order<p>See \"Electronic Monitoring Provision\" in this order<p><strong>Exclusion Requirement</strong><p>You must not enter the place or places the court has specified between the hours specified in the order. The exclusion requirement lasts for the number of days specified in the order<p>See \"Electronic Monitoring Provision\" in this order<p><strong>Residence Requirement</strong><p>You must live at the premises the court has specified and obey any rules that apply there for the number of days specified in the order. You may live at ???? with the prior approval of your Probation Officer.<p><strong>Foreign Travel Prohibition Requirement</strong><p>You must not travel to the prohibited location specified in the order during the period the court has specified in the order.<p><strong>Mental Health Treatment Requirement</strong><p>You must have mental health treatment by or under the direction of the practitioner the court has specified at the location specified as a resident patient for the number of days specified in the order.<p><strong>Drug Rehabilitation Requirement</strong><p>You must have treatment for drug dependency by or under the direction of the practitioner the court has specified at the location specified as a resident patient for the number of days specified in the order.<p>To be sure that you do not have any illegal drug in your body, you must provide samples for testing at such times or in such circumstances as your Probation Officer or the person responsible for your treatment will tell you. The results of tests on the samples will be sent to your Probation Officer who will report the results to the court. Your Probation Officer will also tell the court how your order is progressing and the views of your treatment provider.<p>The court will review this order ????. The first review will be on the date and time specified at the court specified.<p>You must / need not attend this review progression.<p><strong>Alcohol Treatment Requirement</strong><p>You must have treatment for alcohol dependency by or under the direction of the practitioner the court has specified at the location specified as a resident patient for the number of days specified in the order.<p><strong>Supervision Requirement</strong><p>You must attend appointments with your Probation Officer or another person at the times and places your Probation Officer says.<p><strong>Attendance Centre Requirement</strong><p>You must attend an attendance centre - see separate sheet for details<p><strong>WARNING</strong><p>If you do not comply with your order, you will be brought back to court. The court may then<ul><li>Change the order by adding extra requirements</li><li>Pass a different sentence for the original offences; or</li><li>Send you to prison</li></ul><p><strong>NOTE</strong><p>Either you or your Probation Officer can ask the court to look again at this order and the court can then change it or cancel it if it feels that is the right thing to do. The court may also pass a different sentence for the original offence(s). If you wish to ask the court to look at your order again you should get in touch with the court at the address above.")
                                )
                                .withWelshStaticText((
                                        "<h3> Prison </h3> <p> Fe'ch dedfrydwyd i dymor o garchar. Os ydych <ul> <li> Peidiwch â chydymffurfio â gofynion y gorchymyn hwn yn ystod y cyfnod goruchwylio </u>; neu </li> <li> Ymrwymo unrhyw drosedd arall yn ystod y cyfnod gweithredol </u> </li> </ul> efallai y byddwch yn atebol i wasanaethu'r cyfnod gwarchodaeth </u> yn y carchar. <br/> <br/> <br/> <p> Yn ystod y cyfnod goruchwylio </u>, byddwch chi'n cael eich goruchwylio gan eich Swyddog Prawf, a <br/> Rhaid ichi <ul> < li> Cadwch mewn cysylltiad â'ch Swyddog Prawf wrth iddyn nhw ddweud wrthych </li> <li> Dywedwch wrth eich Swyddog Prawf os ydych yn bwriadu newid eich cyfeiriad </li> <li> Cydymffurfio â'r holl ofynion eraill </li></ul > <p> <strong> Gofynion </strong> - Cyfeiriwch yn unig at y gofynion a nododd y llys yn manylion eich archeb, fel y nodir uchod </u> <p> <strong> Gwaith Di-dāl Gofyniad </strong><p> Rhaid i chi wneud gwaith di-dāl am yr oriau a bennir fel y dywedir wrthych a chi erbyn y dyddiad a bennir yn y gorchymyn. Bydd eich Swyddog Prawf yn dweud wrthych pwy fydd yn gyfrifol am oruchwylio gwaith.<p> <strong> Gweithgaredd Gofyniad </strong> <p> Rhaid i chi gyflwyno eich hun fel y'i cyfarwyddir ar yr amser ac ar y diwrnodau a bennir yn y gorchymyn a rhaid i chi ymgymryd â chi y gweithgaredd y mae'r llys wedi ei nodi ar gyfer y cyfnod a bennir yn y drefn yn y ffordd y dywedir wrth eich Swyddog Prawf <p> <strong> Gofyniad Rhaglen </strong><p> Rhaid i chi gymryd rhan yn y rhaglen a bennir yn y drefn yn y lleoliad a bennir ac am y nifer o ddyddiau a bennir yn y gorchymyn <p> <strong> Gofyniad Gweithgaredd Gwahardd </strong> <p> Rhaid i chi beidio â chymryd rhan yn y gweithgaredd a waharddodd y llys yn y drefn ar gyfer nifer y dyddiau llys penodol <p> <strong> Curfew Requirement </strong> <p> Rhaid i chi aros yn y lle neu lle mae'r llys wedi nodi yn ystod y cyfnodau a bennir. Mae'r gofyniad cyrffyw yn para am y nifer o ddyddiau a bennir yn y<p> Gweler \"Darpariaeth Monitro Electronig\" yn yr orchymyn hwn <p> <strong> Gofyniad Preswyl </strong> <p> Rhaid i chi fyw yn yr adeilad y llys wedi nodi ac ufuddhau i unrhyw reolau sy'n berthnasol yno am y nifer o ddyddiau a bennir yn y gorchymyn. Efallai y byddwch yn byw yn ???? gyda chymeradwyaeth ymlaen llaw eich Swyddog Prawf. <p> <strong> Gofyniad Gwahardd Teithio Tramor </strong> <p> Rhaid i chi beidio â theithio i'r lleoliad gwaharddedig a bennir yn yr orchymyn yn ystod y cyfnod y mae'r llys wedi'i bennu yn y gorchymyn. < p> <strong> Gofyniad Triniaeth Iechyd Meddwl </strong> <p> Rhaid i chi gael triniaeth iechyd meddwl gan neu o dan gyfarwyddyd yr ymarferydd y mae'r llys wedi ei nodi yn y lleoliad a bennir fel claf preswyl am y nifer o ddyddiau a bennir yn y <p> <strong> Angen Adsefydlu Cyffuriau </strong> <p> Rhaid i chi gael triniaeth ar gyfer dibyniaeth ar gyffuriau gan neu o dan gyfarwyddyd yr ymarferydd y mae'r llys wedi ei nodi yn y lleoliad a bennir fel claf preswyl am nifer y dyddiau <p> Er mwyn sicrhau nad oes gennych unrhyw gyffur anghyfreithlon yn eich corff, rhaid i chi ddarparu samplau i'w profi ar yr adegau hynny neu mewn amgylchiadau o'r fath y bydd eich Swyddog Prawf neu'r person sy'n gyfrifol am eich triniaeth yn dweud wrthych chi . Anfonir canlyniadau'r profion ar y samplau i'ch Swyddog Prawf a fydd yn adrodd y canlyniadau i'r llys. Bydd eich Swyddog Prawf hefyd yn dweud wrth y llys sut mae'ch gorchymyn yn mynd rhagddo a barn eich darparwr triniaeth. <P> Bydd y llys yn adolygu'r gorchymyn hwn ????. Bydd yr adolygiad cyntaf ar y dyddiad a'r amser a bennir yn y llys a bennir. <P> Rhaid i chi / nid oes angen i chi fynychu'r gwrandawiad hwn. <P> <strong> Gofyniad Trin Alcohol </strong> <p> Rhaid i chi gael triniaeth ar gyfer dibyniaeth ar alcohol gan neu o dan gyfarwyddyd yr ymarferydd y mae'r llys wedi ei nodi yn y lleoliad a bennir fel claf preswyl am y nifer o ddyddiau a bennir yn y gorchymyn. <p> <strong> Gofyniad Goruchwylio </strong> <p> Rhaid i chi fynychu penodiadau gyda'ch Swyddog Prawf neu berson arall ar yr adegau a lle mae eich Swyddog Prawf yn dweud. <p> <strong> Gofyniad y Ganolfan Bresennol </strong> <p> Rhaid i chi fynychu canolfan bresenoldeb - <p> <strong> RHYBUDD </strong> <p> Os na fyddwch chi'n cydymffurfio â'ch archeb, fe'ch cewch eich troi'n ôl i'r llys. Gall y llys wedyn <ul> <li> Newid y gorchymyn trwy ychwanegu gofynion ychwanegol </li> <li> Pasiwch frawddeg wahanol ar gyfer y troseddau gwreiddiol; neu </li> <li> Anfonwch chi at y carchar </li> </ul> <p> <strong> NOTE </strong> <p> Naill ai chi neu'ch Swyddog Prawf all ofyn i'r llys edrych eto ar y gorchymyn hwn ac yna gall y llys ei newid neu ei ganslo os yw'n teimlo mai dyna'r peth iawn i'w wneud. Gall y llys hefyd basio brawddeg wahanol ar gyfer y trosedd (wyr) gwreiddiol. Os hoffech ofyn i'r llys edrych ar eich archeb eto dylech gysylltu â'r llys yn y cyfeiriad uchod. ")
                                )
                                .withPriority(("30"))
                                .withJurisdiction("B")
                                .build()
                        )
                )
                .build();
    }


}
