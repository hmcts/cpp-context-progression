package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.PUBLIC_CASE_DEFENDANT_CHANGED;

@RunWith(MockitoJUnitRunner.class)
public class DefendantDefenceOrganisationChangedProcessorTest {

    @InjectMocks
    private DefendantDefenceOrganisationChangedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private DefendantDefenceOrganisationChanged defendantDefenceOrganisationChanged;

    @Mock
    private ProgressionService progressionService;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;



    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleDefendantDefenceOrganisationChanged() throws Exception {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantDefenceOrganisationChanged.class))
                .thenReturn(defendantDefenceOrganisationChanged);
        when(objectToJsonObjectConverter.convert(Mockito.any(Defendant.class))).thenReturn(payload);
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withAssociationStartDate(LocalDate.now())
                .withIsAssociatedByLAA(true)
                .withFundingType(FundingType.REPRESENTATION_ORDER)
                .withAssociationEndDate(LocalDate.now().plusYears(1))
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("LAA123")
                        .withOrganisation(Organisation.organisation()
                                .withName("Org1")
                                .build())
                        .build())
                .build();
        final ProsecutionCase prosCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(getDefendants(defendantId, prosecutionCaseId, associatedDefenceOrganisation))
                .build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendantId.toString()).build())
                                .build())
                        .build()).build();

        when(defendantDefenceOrganisationChanged.getAssociatedDefenceOrganisation()).thenReturn(associatedDefenceOrganisation);
        when(defendantDefenceOrganisationChanged.getProsecutionCaseId()).thenReturn(prosecutionCaseId);
        when(defendantDefenceOrganisationChanged.getDefendantId()).thenReturn(defendantId);

        when(enveloper.withMetadataFrom(envelope, PUBLIC_CASE_DEFENDANT_CHANGED)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.getProsecutionCaseDetailById(envelope, prosecutionCaseId.toString())).thenReturn(Optional.of(jsonObject));
        when(jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class)).thenReturn(prosCase);
        when(objectToJsonObjectConverter.convert(prosCase)).thenReturn(jsonObject);

        //When
        this.eventProcessor.handleDefendantDefenceOrganisationChanged(envelope);

        //Then
        verify(sender).send(finalEnvelope);

    }

    private List<Defendant> getDefendants(final UUID defendantId, final UUID prosecutionCaseId, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        List<Defendant> defendantList = new ArrayList<>();
        defendantList.add(Defendant.defendant().
                withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .withLegalAidStatus("Granted")
                .build());
        return defendantList;
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }

}
