package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.util.List;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PublicDefenceOrganisationAssociatedEventProcessorTest {

    private static final String PROGRESSION_COMMAND_FOR_DEFENCE_ORGANISATION_ASSOCIATED = "progression.command.handler.associate-defence-organisation";

    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @InjectMocks
    private PublicDefenceOrganisationAssociatedEventProcessor publicDefenceOrganisationAssociatedEventProcessor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHearing() throws IOException {

        final JsonEnvelope event = getJsonHearingResultedCaseUpdatedEnvelope();

        publicDefenceOrganisationAssociatedEventProcessor.processCommandForOrganisationAssociatedEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());

        List<JsonEnvelope> events = this.envelopeArgumentCaptor.getAllValues();

        assertThat(events.get(0).metadata().name(), is(PROGRESSION_COMMAND_FOR_DEFENCE_ORGANISATION_ASSOCIATED));

    }

    private JsonEnvelope getJsonHearingResultedCaseUpdatedEnvelope() throws IOException {
        final String hearingCasePleaAddOrUpdate = getStringFromResource("public.event.defence-organisation-associated.json");

        final Metadata metadata = metadataWithDefaults().build();
        return JsonEnvelope.envelopeFrom(metadata, new StringToJsonObjectConverter().convert(hearingCasePleaAddOrUpdate));
    }

    private String getStringFromResource(final String path) throws IOException {
        return Resources.toString(getResource(path), defaultCharset());
    }
}
