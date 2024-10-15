package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;

import java.util.UUID;

import javax.json.JsonObject;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseRemovedFromGroupCasesEventProcessorTest {

    public static final String GROUP_ID = "groupId";
    public static final String MASTER_CASE_ID = "masterCaseId";
    public static final String REMOVED_CASE = "removedCase";
    public static final String NEW_GROUP_MASTER = "newGroupMaster";
    public static final String ID = "id";
    public static final String IS_CIVIL = "isCivil";
    public static final String IS_GROUP_MEMBER = "isGroupMember";
    public static final String IS_GROUP_MASTER = "isGroupMaster";

    @Mock
    private Sender sender;

    @InjectMocks
    private CaseRemovedFromGroupCasesProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessCivilCaseRemovedFromGroup() {
        final UUID groupId = UUID.randomUUID();
        final UUID masterCaseId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID newGroupMaster = UUID.randomUUID();
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases = CaseRemovedFromGroupCases.caseRemovedFromGroupCases()
                .withGroupId(groupId)
                .withMasterCaseId(masterCaseId)
                .withRemovedCase(ProsecutionCase.prosecutionCase()
                        .withGroupId(groupId)
                        .withId(caseId)
                        .withIsCivil(Boolean.TRUE)
                        .withIsGroupMember(Boolean.FALSE)
                        .withIsGroupMaster(Boolean.FALSE)
                        .build())
                .withNewGroupMaster(ProsecutionCase.prosecutionCase()
                        .withGroupId(groupId)
                        .withId(newGroupMaster)
                        .withIsCivil(Boolean.TRUE)
                        .withIsGroupMember(Boolean.TRUE)
                        .withIsGroupMaster(Boolean.TRUE)
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(caseRemovedFromGroupCases);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-removed-from-group-cases"),
                payload);

        processor.processEvent(requestMessage);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> event1 = envelopeCaptor.getAllValues().get(0);
        assertThat(event1.metadata().name(), is("public.progression.case-removed-from-group-cases"));
        assertThat(event1.payload().getString(GROUP_ID), is(groupId.toString()));
        assertThat(event1.payload().getString(MASTER_CASE_ID), is(masterCaseId.toString()));
        assertThat(event1.payload().getJsonObject(REMOVED_CASE).getString(GROUP_ID), is(groupId.toString()));
        assertThat(event1.payload().getJsonObject(REMOVED_CASE).getString(ID), is(caseId.toString()));
        assertThat(event1.payload().getJsonObject(REMOVED_CASE).getBoolean(IS_CIVIL), is(true));
        assertThat(event1.payload().getJsonObject(REMOVED_CASE).getBoolean(IS_GROUP_MASTER), is(false));
        assertThat(event1.payload().getJsonObject(REMOVED_CASE).getBoolean(IS_GROUP_MEMBER), is(false));
        assertThat(event1.payload().getJsonObject(NEW_GROUP_MASTER).getString(GROUP_ID), is(groupId.toString()));
        assertThat(event1.payload().getJsonObject(NEW_GROUP_MASTER).getString(ID), is(newGroupMaster.toString()));
        assertThat(event1.payload().getJsonObject(NEW_GROUP_MASTER).getBoolean(IS_CIVIL), is(true));
        assertThat(event1.payload().getJsonObject(NEW_GROUP_MASTER).getBoolean(IS_GROUP_MASTER), is(true));
        assertThat(event1.payload().getJsonObject(NEW_GROUP_MASTER).getBoolean(IS_GROUP_MEMBER), is(true));
    }
}
