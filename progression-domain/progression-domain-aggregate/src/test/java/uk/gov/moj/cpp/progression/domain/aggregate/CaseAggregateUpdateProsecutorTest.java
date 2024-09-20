package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CpsProsecutorUpdated;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAggregateUpdateProsecutorTest {

    private static final uk.gov.justice.core.courts.Defendant defendant = Defendant.defendant().withId(randomUUID())
            .withPersonDefendant(PersonDefendant.personDefendant().build())
            .withOffences(singletonList(Offence.offence().build())).build();
    static final List<uk.gov.justice.core.courts.Defendant> defendants = new ArrayList<Defendant>() {{
        add(defendant);
    }};

    private static final ProsecutionCase prosecutionCase = prosecutionCase()
            .withCaseStatus("caseStatus")
            .withId(randomUUID())
            .withDefendants(defendants)
            .withOriginatingOrganisation("originatingOrganisation")
            .withInitiationCode(InitiationCode.C)
            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                    .withProsecutionAuthorityReference("G01FT01AB")
                    .withProsecutionAuthorityCode("C05LV00")
                    .withProsecutionAuthorityId(randomUUID())
                    .withCaseURN("87GD9945217")
                    .build())
            .build();

    @Mock
    JsonEnvelope envelope;
    @Mock
    JsonObject jsonObj;
    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @InjectMocks
    private CaseAggregate caseAggregate;

    @BeforeEach
    public void setUp() {
        this.caseAggregate = new CaseAggregate();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    /**
     * should trigger the event CaseCpsProsecutorUpdated with isCpsOrgVerifyError as true When
     * either cps org value in case is null or if cps org is not present in reference data (in both
     * these scenarios, prosecution case identifier will be null)
     */
    @Test
    public void shouldUpdateCpsProsecutorWithCpsOrgVerifyErrorWhenCpsOrgIsNullOrWhenCpsOrgNotInReferenceData() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        CaseCpsProsecutorUpdated expectedEvent = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                .withProsecutionCaseId(prosecutionCase.getId())
                .withIsCpsOrgVerifyError(true)
                .build();

        final List<Object> eventStream = caseAggregate.updateCaseProsecutorDetails(null).collect(toList());

        assertThat(eventStream.size(), is(1));

        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));
        assertThat(object, is(expectedEvent));
        assertThat(caseAggregate.getProsecutionCase().getProsecutionCaseIdentifier(), is(prosecutionCase.getProsecutionCaseIdentifier()));
        assertNull(caseAggregate.getProsecutionCase().getProsecutor());
    }


    @Test
    public void shouldUpdateCpsProsecutorWithoutCpsOrgVerifyErrorWhenCpsOrgIsValid() {

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = getProsecutionCaseIdentifier();
        final CaseCpsProsecutorUpdated expectedEvent = getExpectedCaseCpsProsecutorUpdatedEvent(prosecutionCaseIdentifier);

        final List<Object> eventStream = caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object actualCaseCpsProsecutorUpdated = eventStream.get(0);
        assertThat(actualCaseCpsProsecutorUpdated.getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));
        assertThat(actualCaseCpsProsecutorUpdated, is(expectedEvent));

        // should'nt update the prosecutionCaseIdentifier details and should update only prosecutor in prosecutionCase
        assertThat(caseAggregate.getProsecutionCase().getProsecutionCaseIdentifier(), is(prosecutionCase.getProsecutionCaseIdentifier()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorId(), is(prosecutionCaseIdentifier.getProsecutionAuthorityId()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorCode(), is(prosecutionCaseIdentifier.getProsecutionAuthorityCode()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorName(), is(prosecutionCaseIdentifier.getProsecutionAuthorityName()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getAddress(), is(prosecutionCaseIdentifier.getAddress()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getIsCps(), is(true));
    }

    @Test
    public void shouldSetCpsOrgVerifyErrorToFalseWhenProsecutorIsManuallyUpdated() {

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = getProsecutionCaseIdentifier();
        final CaseCpsProsecutorUpdated expectedEvent = getExpectedCaseCpsProsecutorUpdatedEvent(prosecutionCaseIdentifier);

        //As CpsProsecutor is false , it sets CpsOrgVerifyError to true
        caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier).collect(toList());
        // when prosecutor is manually updated , then it should set CpsOrgVerifyError to false and update prosecutor object
        final List<Object> eventStream = caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier, null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));
        final Object actualCaseCpsProsecutorUpdated = eventStream.get(0);
        assertThat(actualCaseCpsProsecutorUpdated.getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));
        assertThat(actualCaseCpsProsecutorUpdated, is(expectedEvent));

        // should'nt update the prosecutionCaseIdentifier details and should update only prosecutor in prosecutionCase
        assertThat(caseAggregate.getProsecutionCase().getProsecutionCaseIdentifier(), is(prosecutionCase.getProsecutionCaseIdentifier()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorId(), is(prosecutionCaseIdentifier.getProsecutionAuthorityId()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorCode(), is(prosecutionCaseIdentifier.getProsecutionAuthorityCode()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorName(), is(prosecutionCaseIdentifier.getProsecutionAuthorityName()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getAddress(), is(prosecutionCaseIdentifier.getAddress()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getIsCps(), is(true));
    }

    @Test
    public void shouldUpdateProsecutorAndOldProsecutorWhenProsecutorIsManuallyUpdated() {

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = getProsecutionCaseIdentifier();
        final CaseCpsProsecutorUpdated expectedEvent = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withValuesFrom(getExpectedCaseCpsProsecutorUpdatedEvent(prosecutionCaseIdentifier))
                .withOldCpsProsecutor("C05LV01")
                .build();

        //As CpsProsecutor is false , it sets CpsOrgVerifyError to true
        caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier).collect(toList());
        // when prosecutor is manually updated , then it should set CpsOrgVerifyError to false and update prosecutor object
        final List<Object> eventStream = caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier, "C05LV01").collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));
        final Object actualCaseCpsProsecutorUpdated = eventStream.get(0);
        assertThat(actualCaseCpsProsecutorUpdated.getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));
        assertThat(actualCaseCpsProsecutorUpdated, equalTo(expectedEvent));
        final CpsProsecutorUpdated cpsProsecutorUpdated = (CpsProsecutorUpdated) eventStream.get(1);
        assertThat(cpsProsecutorUpdated.getOldCpsProsecutor(), is("C05LV01"));
        assertThat(cpsProsecutorUpdated.getProsecutionAuthorityCode(), is(prosecutionCaseIdentifier.getProsecutionAuthorityCode()));
        assertThat(cpsProsecutorUpdated.getProsecutionCaseId(), is(prosecutionCase.getId()));

        // should'nt update the prosecutionCaseIdentifier details and should update only prosecutor in prosecutionCase
        assertThat(caseAggregate.getProsecutionCase().getProsecutionCaseIdentifier(), is(prosecutionCase.getProsecutionCaseIdentifier()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorId(), is(prosecutionCaseIdentifier.getProsecutionAuthorityId()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorCode(), is(prosecutionCaseIdentifier.getProsecutionAuthorityCode()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorName(), is(prosecutionCaseIdentifier.getProsecutionAuthorityName()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getAddress(), is(prosecutionCaseIdentifier.getAddress()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getIsCps(), is(true));
    }

    private CaseCpsProsecutorUpdated getExpectedCaseCpsProsecutorUpdatedEvent(ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .withProsecutionAuthorityId(prosecutionCaseIdentifier.getProsecutionAuthorityId())
                .withProsecutionAuthorityName(prosecutionCaseIdentifier.getProsecutionAuthorityName())
                .withMajorCreditorCode(prosecutionCaseIdentifier.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                .withCaseURN(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                .withProsecutionAuthorityReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                .withContact(prosecutionCaseIdentifier.getContact())
                .withProsecutionCaseId(prosecutionCase.getId())
                .withOldCpsProsecutor(null)
                .withIsCpsOrgVerifyError(false)
                .withAddress(prosecutionCaseIdentifier.getAddress()).build();
    }

    private ProsecutionCaseIdentifier getProsecutionCaseIdentifier() {
        ContactNumber contact = ContactNumber.contactNumber().withPrimaryEmail("aaa@bbb.com").build();
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityCode("TFL")
                .withProsecutionAuthorityId(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                .withProsecutionAuthorityName("prosecutionAuthorityName")
                .withMajorCreditorCode("MajorCreditorCode")
                .withProsecutionAuthorityOUCode("ouCode")
                .withContact(contact)
                .withAddress(Address.address().
                        withAddress1("Address1")
                        .withPostcode("SE1Q AEW")
                        .build()).build();
    }
}
