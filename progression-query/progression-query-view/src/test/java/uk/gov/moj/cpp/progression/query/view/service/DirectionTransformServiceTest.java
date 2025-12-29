
package uk.gov.moj.cpp.progression.query.view.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt1Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt2Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt3Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt4Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt5Transformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.Prompt6Transformer;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.progression.query.view.utils.FileUtil.getJsonPayload;

@ExtendWith(MockitoExtension.class)
public class DirectionTransformServiceTest {


    @Mock
    private Prompt1Transformer prompt1Transformer;

    @Mock
    private Prompt2Transformer prompt2Transformer;

    @Mock
    private Prompt3Transformer prompt3Transformer;

    @Mock
    private Prompt4Transformer prompt4Transformer;

    @Mock
    private Prompt5Transformer prompt5Transformer;

    @Mock
    private Prompt6Transformer prompt6Transformer;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private DirectionTransformService directionTransformService;


    HashMap<UUID,String> dummyWitness= Maps.newHashMap(
    ImmutableMap .<UUID, String>builder().
    put(UUID.randomUUID(), "Witness One").
    put(UUID.randomUUID(), "Witness Two").
    build());

    HashMap<UUID,String> dummyAssignees= Maps.newHashMap(
            ImmutableMap .<UUID, String>builder().
                    put(UUID.randomUUID(), "Assignee One").
                    put(UUID.randomUUID(), "Assignee Two").
                    build());

    private static final String QUERY_DIRECTION = "directionsmanagement.query.direction";
    final UUID directionId = UUID.fromString("0a18eadf-0970-42ff-b980-b7f383391391");
    final UUID caseId = UUID.fromString("3277f30a-f51a-489f-926e-7ecf84236d98");


    @Test
    public void shouldCallDirectionTwelveDotOneTransformMethods() throws IOException {
        //given
        final JsonObject refDataResponse =
                createReader(getClass().getClassLoader().
                        getResourceAsStream("refdata-direction-one-prompt1-response.json")).
                        readObject();
        final Direction direction = jsonObjectConverter.convert(refDataResponse, Direction.class);

        final JsonEnvelope query = createQueryDirectionEnvelope();
        when(prompt1Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());
        when(prompt2Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());

        //when
        List<Prompt> prompts = directionTransformService.transform(direction, new ArrayList<>(), new HashMap<>(), dummyWitness, dummyAssignees, false, "PET");

        //then
        verify(prompt1Transformer, times(1)).transform(any(Prompt.class));
        verify(prompt2Transformer, times(1)).transform(any(Prompt.class));

        assertThat(prompts.size(), is(2));
    }

    @Test
    public void shouldCallDirectionTwelveDotFourTransformMethods() throws IOException {
        //given
        final JsonObject refDataResponse =  createReader(getClass().getClassLoader().
                getResourceAsStream("direction-twelve-dot-four.json")).
                readObject();

        final Direction direction = jsonObjectConverter.convert(refDataResponse, Direction.class);

        final JsonEnvelope query = createQueryDirectionEnvelope();
        when(prompt3Transformer.transform(any(Prompt.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean())).thenReturn(Prompt.prompt().build());
        when(prompt4Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());
        //when
        List<Prompt> prompts = directionTransformService.transform(direction, new ArrayList<>(), new HashMap<>(), dummyWitness, dummyAssignees, false, "PET");

        //then
        verify(prompt3Transformer, times(1)).transform(any(Prompt.class), any(Map.class), any(Map.class),any(Map.class), anyBoolean());
        verify(prompt4Transformer, times(1)).transform(any(Prompt.class));

        assertThat(prompts.size(), is(2));
    }

    @Test
    public void shouldCallDirectionTwelveDotFiveTransformMethods() throws IOException {
        // given
        final JsonObject refDataResponse = createReader(getClass().getClassLoader()
                .getResourceAsStream("direction-twelve-dot-five.json"))
                .readObject();

        final Direction direction = jsonObjectConverter.convert(refDataResponse, Direction.class);

        when(prompt1Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());
        when(prompt3Transformer.transform(any(Prompt.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean())).thenReturn(Prompt.prompt().build());
        when(prompt5Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());
        when(prompt6Transformer.transform(any(Prompt.class), any(List.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean(), anyString())).thenReturn(Prompt.prompt().build());

        // when
        List<Prompt> prompts = directionTransformService.transform(direction, new ArrayList<>(), new HashMap<>(), dummyWitness, dummyAssignees, false, "PET");

        // then
        verify(prompt1Transformer, times(4)).transform(any(Prompt.class));
        verify(prompt3Transformer, times(1)).transform(any(Prompt.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean());
        verify(prompt5Transformer, times(1)).transform(any(Prompt.class));
        verify(prompt6Transformer, times(1)).transform(any(Prompt.class), any(List.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean(), anyString());

        assertThat(prompts.size(), is(4));
    }
    @Test
    public void shouldCallDirectionTwelveDotSixTransformMethods() throws IOException {
        //given
        final JsonObject refDataResponse =  createReader(getClass().getClassLoader().
                getResourceAsStream("direction-twelve-dot-six.json")).
                readObject();

        final Direction direction = jsonObjectConverter.convert(refDataResponse, Direction.class);

        when(prompt1Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());
        when(prompt3Transformer.transform(any(Prompt.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean())).thenReturn(Prompt.prompt().build());
        //when
        List<Prompt> prompts = directionTransformService.transform(direction, new ArrayList<>(), new HashMap<>(), dummyWitness,dummyAssignees, false, "PET");

        //then
        verify(prompt3Transformer, times(1)).transform(any(Prompt.class), any(Map.class), any(Map.class),any(Map.class), anyBoolean());
        verify(prompt1Transformer, times(12)).transform(any(Prompt.class));

        assertThat(prompts.size(), is(2));
    }

    @Test
    public void shouldCallDirectionTwelveDotElevenTransformMethods() throws IOException {
        //given
        final JsonObject refDataResponse = getJsonPayload("direction-twelve-dot-eleven.json");
        final Direction direction = jsonObjectConverter.convert(refDataResponse, Direction.class);

        final JsonEnvelope query = createQueryDirectionEnvelope();
        when(prompt3Transformer.transform(any(Prompt.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean())).thenReturn(Prompt.prompt().build());
        when(prompt6Transformer.transform(any(Prompt.class), any(List.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean(), anyString())).thenReturn(Prompt.prompt().build());

        //when
        List<Prompt> prompts = directionTransformService.transform(direction, new ArrayList<>(), new HashMap<>(), dummyWitness,dummyAssignees, false, "PET");

        //then
        verify(prompt3Transformer, times(1)).transform(any(Prompt.class), any(Map.class), any(Map.class),any(Map.class), anyBoolean());
        verify(prompt6Transformer, times(1)).transform(any(Prompt.class),any(List.class) ,any(Map.class), any(Map.class),any(Map.class), anyBoolean(), anyString());

        assertThat(prompts.size(), is(2));
    }

    @Test
    public void shouldCallTransformMethodForChildrenPrompts() throws IOException {
        //given
        final JsonObject refDataResponse = getJsonPayload("prompt_has_children.json");

        final Direction direction = jsonObjectConverter.convert(refDataResponse, Direction.class);

        final JsonEnvelope query = createQueryDirectionEnvelope();
        when(prompt1Transformer.transform(any(Prompt.class))).thenReturn(Prompt.prompt().build());
        when(prompt3Transformer.transform(any(Prompt.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean())).thenReturn(Prompt.prompt().build());
        when(prompt6Transformer.transform(any(Prompt.class), any(List.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean(), anyString())).thenReturn(Prompt.prompt().build());


        //when
        List<Prompt> prompts = directionTransformService.transform(direction, new ArrayList<>(), new HashMap<>(), dummyWitness,dummyAssignees, false, "PET");

        //then
        verify(prompt3Transformer, times(1)).transform(any(Prompt.class), any(Map.class), any(Map.class),any(Map.class), anyBoolean());
        verify(prompt6Transformer, times(1)).transform(any(Prompt.class),any(List.class), any(Map.class), any(Map.class),any(Map.class), anyBoolean(), anyString());
        verify(prompt1Transformer, times(1)).transform(any(Prompt.class));

        assertThat(prompts.size(), is(1));

    }


    private JsonEnvelope createQueryDirectionEnvelope() {
        final JsonObject jsonObject = createObjectBuilder()
                .add("directionId", directionId.toString())
                .add("caseId", caseId.toString())
                .add("orderDate", "2021-06-28")
                .build();
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(QUERY_DIRECTION),
                jsonObject);
    }
}
