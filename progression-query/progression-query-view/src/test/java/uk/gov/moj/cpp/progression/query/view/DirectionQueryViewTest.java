package uk.gov.moj.cpp.progression.query.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;
import uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection;
import uk.gov.moj.cpp.progression.domain.pojo.ReferenceDataDirectionManagementType;
import uk.gov.moj.cpp.progression.query.view.service.DirectionTransformService;
import uk.gov.moj.cpp.progression.query.view.utils.FileUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DirectionQueryViewTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    final UUID directionId = UUID.fromString("0a18eadf-0970-42ff-b980-b7f383391391");

    @Mock
    private DirectionTransformService directionTransformService;

    @InjectMocks
    private DirectionQueryView directionQueryView;


    @Test
    public void shouldGetTransformedDirections() throws IOException {

        final Direction refDataDirectionFromRefDB = jsonObjectConverter.convert(FileUtil.getJsonPayload("refdata-direction-one-prompt1-response.json"), Direction.class);
        final Prompt transformedPrompt1 = jsonObjectConverter.convert(FileUtil.getJsonPayload("dm-transformed-dir-one.json"), Prompt.class);
        final Prompt transformedPrompt2 = jsonObjectConverter.convert(FileUtil.getJsonPayload("dm-transformed-dir-one-prompt-one.json"), Prompt.class);

        ReferenceDataDirectionManagementType referenceDataDirectionManagementType =  new ReferenceDataDirectionManagementType(randomUUID(), "name1", 1, "PET", "", "test1", "test");

        when(directionTransformService.transform(any(Direction.class), any(List.class), any(Map.class), any(Map.class), any(Map.class), anyBoolean(), anyString())).thenReturn(asList(transformedPrompt1, transformedPrompt2));

        final RefDataDirection transformedDirections = directionQueryView.getTransformedDirections(refDataDirectionFromRefDB, referenceDataDirectionManagementType, asList(), new HashMap<>(), new HashMap<>(), false, "");

        assertThat(transformedDirections.getRefData().getDirectionRefDataId(), is(directionId));
        assertThat(transformedDirections.getPrompts().get(0).getHeader(), is(true));
        assertThat(transformedDirections.getPrompts().get(1).getValue(), is("2020-07-15"));

    }

}
