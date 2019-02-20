package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.test.matchers.ElementAtListMatcher.first;

import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Now;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.event.nows.order.Address;
import uk.gov.moj.cpp.progression.event.nows.order.Cases;
import uk.gov.moj.cpp.progression.event.nows.order.DefendantCaseOffences;
import uk.gov.moj.cpp.progression.event.nows.order.DefendantCaseResults;
import uk.gov.moj.cpp.progression.event.nows.order.FinancialOrderDetails;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.event.nows.order.Prompts;
import uk.gov.moj.cpp.progression.event.nows.order.Results;
import uk.gov.moj.cpp.progression.test.TestTemplates;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NowsRequestedToOrderConverterTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void testCrownCourtConversion() throws IOException {
        testConversion(JurisdictionType.CROWN, false);
    }

    @Test
    public void testMagsCourtConversion() throws IOException {
        testConversion(JurisdictionType.MAGISTRATES, true);
    }

    @Test
    public void testNowsRequestTemplateWithConditionalText() {
        final CreateNowsRequest input = TestTemplates.generateNowsRequestTemplateWithConditionalText(UUID.randomUUID(), JurisdictionType.MAGISTRATES, true);

        final Hearing hearing = input.getHearing();
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final Offence offence = defendant.getOffences().get(0);
        final Now now0 = input.getNows().get(0);
        final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrders = NowsRequestedToOrderConverter.convert(input);

        final NowsDocumentOrder nowsDocumentOrder = nowsDocumentOrders.keySet().iterator().next();

        final NowsNotificationDocumentState nowsNotificationDocumentState = nowsDocumentOrders.values().iterator().next();

        final String defendantName = now0.getRequestedMaterials().get(0).getNowVariantDefendant().getName();

        final String dob = now0.getRequestedMaterials().get(0).getNowVariantDefendant().getDateOfBirth();

        final uk.gov.justice.core.courts.Address address = now0.getRequestedMaterials().get(0).getNowVariantDefendant().getAddress();

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();

        final String expectedCaseRef = prosecutionCaseIdentifier.getProsecutionAuthorityReference();

        final String expectedConvictionDate = offence.getConvictionDate();

        uk.gov.justice.core.courts.FinancialOrderDetails financialOrderDetailsIn = now0.getFinancialOrders();

        assertThat(nowsDocumentOrder, isBean(NowsDocumentOrder.class)
                .with(NowsDocumentOrder::getMaterialId, is(input.getNows().get(0).getRequestedMaterials().get(0).getMaterialId()))
                .with(NowsDocumentOrder::getIsAmended, is(input.getNows().get(0).getRequestedMaterials().get(0).getIsAmended()))

                .with(NowsDocumentOrder::getPriority, is(input.getNowTypes().get(0).getPriority()))
                .with(NowsDocumentOrder::getOrderName, is(input.getNowTypes().get(0).getDescription()))

                .with(NowsDocumentOrder::getCourtCentreName, is(input.getHearing().getCourtCentre().getName()))

                .with(NowsDocumentOrder::getCourtClerkName, is(format("%s %s", input.getCourtClerk().getFirstName(), input.getCourtClerk().getLastName())))
                .with(NowsDocumentOrder::getOrderDate, is(input.getSharedResultLines().get(0).getOrderedDate()))
                .with(NowsDocumentOrder::getDefendant, isBean(uk.gov.moj.cpp.progression.event.nows.order.Defendant.class)
                        .with(uk.gov.moj.cpp.progression.event.nows.order.Defendant::getName, is(defendantName))
                        .with(uk.gov.moj.cpp.progression.event.nows.order.Defendant::getDateOfBirth, is(dob))
                        .with(uk.gov.moj.cpp.progression.event.nows.order.Defendant::getAddress, isBean(Address.class)
                                .with(Address::getLine1, is(address.getAddress1()))
                                .with(Address::getLine2, nullValue())
                                .with(Address::getLine3, nullValue())
                                .with(Address::getLine4, nullValue())
                                .with(Address::getPostCode, nullValue())
                        )
                )
                .with(NowsDocumentOrder::getCaseUrns, hasItem(expectedCaseRef))
                .with(NowsDocumentOrder::getFinancialOrderDetails, isBean(FinancialOrderDetails.class)
                        .withValue(FinancialOrderDetails::getPaymentTerms, financialOrderDetailsIn.getPaymentTerms())
                )
                .with(NowsDocumentOrder::getCases, first(isBean(Cases.class)
                        .with(Cases::getUrn, is(expectedCaseRef))
                        .with(Cases::getDefendantCaseResults, first(isBean(DefendantCaseResults.class)
                                .with(DefendantCaseResults::getLabel, is(input.getSharedResultLines().get(0).getLabel()))
                                .with(DefendantCaseResults::getPrompts, first(isBean(Prompts.class)
                                        .with(Prompts::getLabel, is(input.getSharedResultLines().get(0).getPrompts().get(0).getLabel()))
                                        .with(Prompts::getValue, is(input.getSharedResultLines().get(0).getPrompts().get(0).getValue()))
                                ))
                        ))
                        .with(Cases::getDefendantCaseResults, first(isBean(DefendantCaseResults.class)
                                .with(DefendantCaseResults::getLabel, is(input.getSharedResultLines().get(1).getLabel()))
                                .with(DefendantCaseResults::getPrompts, first(isBean(Prompts.class)
                                        .with(Prompts::getLabel, is(input.getSharedResultLines().get(1).getPrompts().get(0).getLabel()))
                                        .with(Prompts::getValue, is(input.getSharedResultLines().get(1).getPrompts().get(0).getValue()))
                                ))
                        ))
                        .with(Cases::getDefendantCaseOffences, first(isBean(DefendantCaseOffences.class)
                                .with(DefendantCaseOffences::getWording, is(offence.getWording()))
                                .with(DefendantCaseOffences::getStartDate, is(offence.getStartDate()))
                                .with(DefendantCaseOffences::getConvictionDate, is(expectedConvictionDate))
                                .with(DefendantCaseOffences::getResults, first(isBean(Results.class)
                                        .with(Results::getLabel, is(input.getSharedResultLines().get(2).getLabel()))
                                        .with(Results::getPrompts, first(isBean(Prompts.class)
                                                .with(Prompts::getLabel, is(input.getSharedResultLines().get(2).getPrompts().get(0).getLabel()))
                                                .with(Prompts::getValue, is(input.getSharedResultLines().get(2).getPrompts().get(0).getValue()))
                                        ))
                                ))
                        ))
                ))
                .with(NowsDocumentOrder::getNowText, is(input.getNowTypes().get(0).getStaticText()))
        );

        assertThat(nowsDocumentOrder.getNowResultDefinitionsText().getAdditionalProperties().get("ABCD"),
                is(input.getNows().get(0).getRequestedMaterials().get(0).getNowResults().get(2).getNowVariantResultText().getAdditionalProperties().get("ABCD")));

        assertThat(nowsNotificationDocumentState, isBean(NowsNotificationDocumentState.class)
                .with(NowsNotificationDocumentState::getOriginatingCourtCentreId, is(hearing.getCourtCentre().getId()))
                .with(NowsNotificationDocumentState::getUsergroups, hasItems(input.getNows().get(0).getRequestedMaterials().get(0).getKey().getUsergroups().get(0)))
                .with(NowsNotificationDocumentState::getNowsTypeId, is(input.getNows().get(0).getNowsTypeId()))
                .with(NowsNotificationDocumentState::getCaseUrns, hasItems(expectedCaseRef))
                .with(NowsNotificationDocumentState::getCourtClerkName,
                        is(format("%s %s", input.getCourtClerk().getFirstName(), input.getCourtClerk().getLastName())))
                .with(NowsNotificationDocumentState::getDefendantName, is(defendantName))
                .with(NowsNotificationDocumentState::getJurisdiction, is(input.getNowTypes().get(0).getJurisdiction()))
                .with(NowsNotificationDocumentState::getCourtCentreName, is(hearing.getCourtCentre().getName()))
                .with(NowsNotificationDocumentState::getOrderName, is(input.getNowTypes().get(0).getDescription()))
                .with(NowsNotificationDocumentState::getPriority, is(input.getNowTypes().get(0).getPriority()))
                .with(NowsNotificationDocumentState::getMaterialId, is(input.getNows().get(0).getRequestedMaterials().get(0).getMaterialId()))
        );
    }

    private void testConversion(final JurisdictionType jurisdictionType, final boolean expectedRemotePrintingRequired) throws IOException {

        final CreateNowsRequest input = TestTemplates.generateNowsRequestTemplate(UUID.randomUUID(), jurisdictionType, true, expectedRemotePrintingRequired);

        final Hearing hearing = input.getHearing();
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final Offence offence = defendant.getOffences().get(0);
        final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrders = NowsRequestedToOrderConverter.convert(input);

        final NowsDocumentOrder nowsDocumentOrder = nowsDocumentOrders.keySet().iterator().next();

        final NowsNotificationDocumentState nowsNotificationDocumentState = nowsDocumentOrders.values().iterator().next();

        final String defendantName = input.getNows().get(0).getRequestedMaterials().get(0).getNowVariantDefendant().getName();

        final uk.gov.justice.core.courts.Address address = input.getNows().get(0).getRequestedMaterials().get(0).getNowVariantDefendant().getAddress();

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();

        final String expectedCaseRef = jurisdictionType == JurisdictionType.MAGISTRATES ? prosecutionCaseIdentifier.getProsecutionAuthorityReference() : prosecutionCaseIdentifier.getCaseURN();

        final String expectedConvictionDate = StringUtils.defaultString(offence.getConvictionDate());

        final Boolean expectedIsCrownCourt = jurisdictionType.equals(JurisdictionType.CROWN);

        assertThat(nowsDocumentOrder, isBean(NowsDocumentOrder.class)
                .with(NowsDocumentOrder::getMaterialId, is(input.getNows().get(0).getRequestedMaterials().get(0).getMaterialId()))
                .with(NowsDocumentOrder::getIsAmended, is(input.getNows().get(0).getRequestedMaterials().get(0).getIsAmended()))

                .with(NowsDocumentOrder::getPriority, is(input.getNowTypes().get(0).getPriority()))
                .with(NowsDocumentOrder::getOrderName, is(input.getNowTypes().get(0).getDescription()))

                .with(NowsDocumentOrder::getCourtCentreName, is(input.getHearing().getCourtCentre().getName()))

                .with(NowsDocumentOrder::getCourtClerkName, is(format("%s %s", input.getCourtClerk().getFirstName(), input.getCourtClerk().getLastName())))
                .with(NowsDocumentOrder::getOrderDate, is(input.getSharedResultLines().get(0).getOrderedDate()))
                .with(NowsDocumentOrder::getDefendant, isBean(uk.gov.moj.cpp.progression.event.nows.order.Defendant.class)
                        .with(uk.gov.moj.cpp.progression.event.nows.order.Defendant::getName, is(defendantName))
                        .with(uk.gov.moj.cpp.progression.event.nows.order.Defendant::getAddress, isBean(Address.class)
                                .with(Address::getLine1, is(address.getAddress1()))
                                .with(Address::getLine2, nullValue())
                                .with(Address::getLine3, nullValue())
                                .with(Address::getLine4, nullValue())
                                .with(Address::getPostCode, nullValue())
                        )
                )
                .with(NowsDocumentOrder::getCaseUrns, hasItem(expectedCaseRef))
                .with(NowsDocumentOrder::getCases, first(isBean(Cases.class)
                        .with(Cases::getUrn, is(expectedCaseRef))
                        .with(Cases::getDefendantCaseResults, first(isBean(DefendantCaseResults.class)
                                .with(DefendantCaseResults::getLabel, is(input.getSharedResultLines().get(0).getLabel()))
                                .with(DefendantCaseResults::getPrompts, first(isBean(Prompts.class)
                                        .with(Prompts::getLabel, is(input.getSharedResultLines().get(0).getPrompts().get(0).getLabel()))
                                        .with(Prompts::getValue, is(input.getSharedResultLines().get(0).getPrompts().get(0).getValue()))
                                ))
                        ))
                        .with(Cases::getDefendantCaseResults, first(isBean(DefendantCaseResults.class)
                                .with(DefendantCaseResults::getLabel, is(input.getSharedResultLines().get(1).getLabel()))
                                .with(DefendantCaseResults::getPrompts, first(isBean(Prompts.class)
                                        .with(Prompts::getLabel, is(input.getSharedResultLines().get(1).getPrompts().get(0).getLabel()))
                                        .with(Prompts::getValue, is(input.getSharedResultLines().get(1).getPrompts().get(0).getValue()))
                                ))
                        ))
                        .with(Cases::getDefendantCaseOffences, first(isBean(DefendantCaseOffences.class)
                                .with(DefendantCaseOffences::getWording, is(offence.getWording()))
                                .with(DefendantCaseOffences::getStartDate, is(offence.getStartDate().toString()))
                                .with(DefendantCaseOffences::getConvictionDate, is(expectedConvictionDate))
                                .with(DefendantCaseOffences::getResults, first(isBean(Results.class)
                                        .with(Results::getLabel, is(input.getSharedResultLines().get(2).getLabel()))
                                        .with(Results::getPrompts, first(isBean(Prompts.class)
                                                .with(Prompts::getLabel, is(input.getSharedResultLines().get(2).getPrompts().get(0).getLabel()))
                                                .with(Prompts::getValue, is(input.getSharedResultLines().get(2).getPrompts().get(0).getValue()))
                                        ))
                                ))

                        ))
                ))
                .with(NowsDocumentOrder::getNowText, is(input.getNowTypes().get(0).getStaticText()))
                .withValue(NowsDocumentOrder::getIsCrownCourt, expectedIsCrownCourt)
        );

        assertThat(nowsNotificationDocumentState, isBean(NowsNotificationDocumentState.class)
                .with(NowsNotificationDocumentState::getOriginatingCourtCentreId, is(hearing.getCourtCentre().getId()))
                .with(NowsNotificationDocumentState::getUsergroups, hasItems(input.getNows().get(0).getRequestedMaterials().get(0).getKey().getUsergroups().get(0)))
                .with(NowsNotificationDocumentState::getNowsTypeId, is(input.getNows().get(0).getNowsTypeId()))
                .with(NowsNotificationDocumentState::getCaseUrns, hasItems(expectedCaseRef))
                .with(NowsNotificationDocumentState::getCourtClerkName,
                        is(format("%s %s", input.getCourtClerk().getFirstName(), input.getCourtClerk().getLastName())))
                .with(NowsNotificationDocumentState::getDefendantName, is(defendantName))
                .with(NowsNotificationDocumentState::getJurisdiction, is(input.getNowTypes().get(0).getJurisdiction()))
                .with(NowsNotificationDocumentState::getCourtCentreName, is(hearing.getCourtCentre().getName()))
                .with(NowsNotificationDocumentState::getOrderName, is(input.getNowTypes().get(0).getDescription()))
                .with(NowsNotificationDocumentState::getPriority, is(input.getNowTypes().get(0).getPriority()))
                .with(NowsNotificationDocumentState::getMaterialId, is(input.getNows().get(0).getRequestedMaterials().get(0).getMaterialId()))
                .withValue(NowsNotificationDocumentState::getIsRemotePrintingRequired, expectedRemotePrintingRequired)
        );
    }
}
