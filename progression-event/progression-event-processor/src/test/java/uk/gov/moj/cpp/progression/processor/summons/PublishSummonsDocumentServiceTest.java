package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.notification.EmailChannel.emailChannel;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublishSummonsDocumentServiceTest {

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private CourtDocumentObjectService courtDocumentObjectService;

    @Mock
    private Sender sender;

    @Mock
    private SummonsDocumentContent summonsDocumentContent;

    @Mock
    private JsonObject summonsDocumentContentAsJsonObject;

    @Mock
    private CourtDocument courtDocument;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @InjectMocks
    private PublishSummonsDocumentService publishSummonsDocumentService;

    @BeforeEach
    public void setup() {
        when(objectToJsonObjectConverter.convert(summonsDocumentContent)).thenReturn(summonsDocumentContentAsJsonObject);
    }

    @Test
    public void generateCaseSummonsCourtDocument() {

        final UUID defendantId =  randomUUID();
        final UUID caseId =  randomUUID();
        final String templateName = randomAlphabetic(10);
        final Boolean sendForRemotePrinting = RandomGenerator.BOOLEAN.next();
        final UUID materialId = randomUUID();
        final EmailChannel emailChannel = emailChannel().build();

        when(courtDocumentObjectService.buildCaseSummonsCourtDocument(caseId, defendantId, materialId, jsonEnvelope)).thenReturn(courtDocument);

        publishSummonsDocumentService.generateCaseSummonsCourtDocument(jsonEnvelope, defendantId, caseId, summonsDocumentContent, templateName, sendForRemotePrinting, emailChannel, materialId);

        verify(documentGeneratorService).generateSummonsDocument(jsonEnvelope, summonsDocumentContentAsJsonObject, templateName, sender, caseId, null, sendForRemotePrinting, emailChannel, materialId);
        verify(progressionService).createCourtDocument(jsonEnvelope, singletonList(courtDocument));
        verify(courtDocumentObjectService).buildCaseSummonsCourtDocument(caseId, defendantId, materialId, jsonEnvelope);
        verify(systemIdMapperService).mapMaterialIdToDocumentId(courtDocument.getCourtDocumentId(), materialId);
    }

    @Test
    public void generateApplicationSummonsCourtDocument() {
        final UUID applicationId =  randomUUID();
        final String templateName = randomAlphabetic(10);
        final Boolean sendForRemotePrinting = RandomGenerator.BOOLEAN.next();
        final UUID materialId = randomUUID();
        final EmailChannel emailChannel = emailChannel().build();

        when(courtDocumentObjectService.buildApplicationSummonsCourtDocument(applicationId, materialId, jsonEnvelope)).thenReturn(courtDocument);

        publishSummonsDocumentService.generateApplicationSummonsCourtDocument(jsonEnvelope, applicationId, summonsDocumentContent, templateName, sendForRemotePrinting, emailChannel, materialId);

        verify(documentGeneratorService).generateSummonsDocument(jsonEnvelope, summonsDocumentContentAsJsonObject, templateName, sender, null, applicationId, sendForRemotePrinting, emailChannel, materialId);
        verify(progressionService).createCourtDocument(jsonEnvelope, singletonList(courtDocument));
        verify(courtDocumentObjectService).buildApplicationSummonsCourtDocument(applicationId, materialId, jsonEnvelope);
        verify(systemIdMapperService).mapMaterialIdToDocumentId(courtDocument.getCourtDocumentId(), materialId);
    }
}