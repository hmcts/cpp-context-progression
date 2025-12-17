package uk.gov.moj.cpp.progression.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationDetailsEnrichmentServiceTest {

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private ApplicationDetailsEnrichmentService service;

    private static final UUID SYSTEM_USER_ID = UUID.randomUUID();
    private static final String TARGET_TYPE_APPLICATION = "APPLICATION_ID_LAA";
    private static final String SOURCE_TYPE_APPLICATION = "LAA_APP_SHORT_ID";

    @BeforeEach
    void setUp() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
    }

    @Test
    void shouldCreateNewLaaApplicationShortId() {
        UUID applicationId = UUID.randomUUID();
        
        String result = service.createAndStoreLaaApplicationShortIdWithSystemIdMapper(applicationId);
        
        assertNotNull(result);
        assertTrue(result.matches("A\\d{2}[A-Z0-9]{9}"));
        
        ArgumentCaptor<SystemIdMap> mapCaptor = ArgumentCaptor.forClass(SystemIdMap.class);
        verify(systemIdMapperClient).add(mapCaptor.capture(), eq(SYSTEM_USER_ID));
        
        SystemIdMap capturedMap = mapCaptor.getValue();
        assertEquals(result, capturedMap.getSourceId());
        assertEquals(applicationId, capturedMap.getTargetId());
        assertEquals(SOURCE_TYPE_APPLICATION, capturedMap.getSourceType());
        assertEquals(TARGET_TYPE_APPLICATION, capturedMap.getTargetType());
    }

    @Test
    void shouldThrowExceptionWhenMappingCreationFails() {
        UUID applicationId = UUID.randomUUID();
        
        doThrow(new RuntimeException("Mapping failed"))
                .when(systemIdMapperClient).add(any(SystemIdMap.class), eq(SYSTEM_USER_ID));

        assertThrows(IllegalStateException.class, 
                () -> service.createAndStoreLaaApplicationShortIdWithSystemIdMapper(applicationId));
    }

    @Test
    void shouldThrowExceptionWhenSystemUserIdNotFound() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, 
                () -> service.createAndStoreLaaApplicationShortIdWithSystemIdMapper(UUID.randomUUID()));
    }

    @Test
    void shouldGenerateNewIdWhenFirstGeneratedIdExists() {
        UUID applicationId = UUID.randomUUID();
        String firstGeneratedId = "A23ABCDEFGH";
        String secondGeneratedId = "A23ZYXWVUTS";
        
        // Mock first generated ID already exists
        SystemIdMapping existingMapping = new SystemIdMapping(
                SYSTEM_USER_ID,
                firstGeneratedId,
                SOURCE_TYPE_APPLICATION,
                UUID.randomUUID(),  // different application ID
                TARGET_TYPE_APPLICATION,
                null
        );
        
        // Mock service to generate IDs in sequence
        ApplicationDetailsEnrichmentService spyService = spy(service);
        when(spyService.generateLaaApplicationShortId())
                .thenReturn(firstGeneratedId)
                .thenReturn(secondGeneratedId);
        
        // Mock first ID exists, second doesn't - use lenient() to avoid strict stubbing issues
        lenient().when(systemIdMapperClient.findBy(eq(SYSTEM_USER_ID), eq(firstGeneratedId), eq(SOURCE_TYPE_APPLICATION)))
                .thenReturn(Optional.of(existingMapping));
        lenient().when(systemIdMapperClient.findBy(eq(SYSTEM_USER_ID), eq(firstGeneratedId), eq(TARGET_TYPE_APPLICATION)))
                .thenReturn(Optional.empty());
        lenient().when(systemIdMapperClient.findBy(eq(SYSTEM_USER_ID), eq(secondGeneratedId), eq(SOURCE_TYPE_APPLICATION)))
                .thenReturn(Optional.empty());
        lenient().when(systemIdMapperClient.findBy(eq(SYSTEM_USER_ID), eq(secondGeneratedId), eq(TARGET_TYPE_APPLICATION)))
                .thenReturn(Optional.empty());
        
        String result = spyService.createAndStoreLaaApplicationShortIdWithSystemIdMapper(applicationId);
        
        assertEquals(firstGeneratedId, result);
        verify(systemIdMapperClient).add(any(SystemIdMap.class), eq(SYSTEM_USER_ID));
        
        ArgumentCaptor<SystemIdMap> mapCaptor = ArgumentCaptor.forClass(SystemIdMap.class);
        verify(systemIdMapperClient).add(mapCaptor.capture(), eq(SYSTEM_USER_ID));
        assertEquals(firstGeneratedId, mapCaptor.getValue().getSourceId());
        assertEquals(applicationId, mapCaptor.getValue().getTargetId());
    }

    @Test
    void shouldThrowExceptionAfterMultipleExistingIds() {
        UUID applicationId = UUID.randomUUID();
        String alwaysExistingId = "A23ABCDEFGH";
        
        // Mock that every generated ID already exists
        SystemIdMapping existingMapping = new SystemIdMapping(
                SYSTEM_USER_ID,
                alwaysExistingId,
                SOURCE_TYPE_APPLICATION,
                UUID.randomUUID(),
                TARGET_TYPE_APPLICATION,
                null
        );
        
        ApplicationDetailsEnrichmentService spyService = spy(service);
        when(spyService.generateLaaApplicationShortId()).thenReturn(alwaysExistingId);
        when(systemIdMapperClient.findBy(any(), eq(SOURCE_TYPE_APPLICATION), eq(TARGET_TYPE_APPLICATION), any()))
                .thenReturn(Optional.of(existingMapping));

        assertThrows(IllegalStateException.class,
                () -> spyService.createAndStoreLaaApplicationShortIdWithSystemIdMapper(applicationId));
    }
} 