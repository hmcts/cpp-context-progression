package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.service.SystemIdMapperService.APPLICATION_TARGET_TYPE;
import static uk.gov.moj.cpp.progression.service.SystemIdMapperService.CASE_TARGET_TYPE;
import static uk.gov.moj.cpp.progression.service.SystemIdMapperService.DOCUMENT_TARGET_TYPE;
import static uk.gov.moj.cpp.progression.service.SystemIdMapperService.MATERIAL_SOURCE_TYPE;
import static uk.gov.moj.cpp.progression.service.SystemIdMapperService.MATERIAL_TARGET_TYPE;
import static uk.gov.moj.cpp.progression.service.SystemIdMapperService.NOTIFICATION_SOURCE_TYPE;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.CONFLICT;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.OK;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.progression.exception.ContextSystemUserIdException;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SystemIdMapperServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @InjectMocks
    private SystemIdMapperService target;

    private final UUID userId = randomUUID();
    private final UUID notificationId = randomUUID();
    private final UUID mappedCppCaseId = randomUUID();
    private final UUID mappedApplicationId = randomUUID();
    private final UUID mappedMaterialId = randomUUID();
    private final UUID mappedDocumentId = randomUUID();

    @Before
    public void setUp() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
    }

    @Test
    public void shouldReturnCppCaseIdForNotificationId() {
        target.getCppCaseIdForNotificationId(notificationId.toString());
        verify(systemIdMapperClient).findBy(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, CASE_TARGET_TYPE, userId);
    }

    @Test
    public void shouldReturnApplicationIdForNotificationId() {
        target.getCppApplicationIdForNotificationId(notificationId.toString());
        verify(systemIdMapperClient).findBy(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, APPLICATION_TARGET_TYPE, userId);
    }

    @Test
    public void shouldReturnMaterialIdForNotificationId() {
        target.getCppMaterialIdForNotificationId(notificationId.toString());
        verify(systemIdMapperClient).findBy(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, MATERIAL_TARGET_TYPE, userId);
    }

    @Test
    public void shouldReturnDocumentIdForMaterialId() {
        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), mappedMaterialId.toString(), MATERIAL_SOURCE_TYPE, mappedDocumentId, DOCUMENT_TARGET_TYPE, now());
        when(systemIdMapperClient.findBy(mappedMaterialId.toString(), MATERIAL_SOURCE_TYPE, DOCUMENT_TARGET_TYPE, userId)).thenReturn(of(systemIdMapping));
        assertThat(systemIdMapping.getTargetId(), is(mappedDocumentId));
    }

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExists() {
        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), notificationId.toString(), NOTIFICATION_SOURCE_TYPE, mappedCppCaseId, CASE_TARGET_TYPE, now());

        when(systemIdMapperClient.findBy(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, CASE_TARGET_TYPE, userId)).thenReturn(of(systemIdMapping));
        assertThat(systemIdMapping.getTargetId(), is(mappedCppCaseId));
    }

    @Test
    public void shouldAddNotificationToCaseIdMapping() {
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));
        target.mapNotificationIdToCaseId(mappedCppCaseId, notificationId);
        verify(systemIdMapperClient).add(any(SystemIdMap.class), any(UUID.class));
    }

    @Test
    public void shouldMapNotificationIdToApplicationId() {
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));
        target.mapNotificationIdToApplicationId(mappedApplicationId, notificationId);
        verify(systemIdMapperClient).add(any(SystemIdMap.class), any(UUID.class));
    }

    @Test
    public void shouldMapNotificationIdToMaterialId() {
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));
        target.mapNotificationIdToMaterialId(mappedMaterialId, notificationId);
        verify(systemIdMapperClient).add(any(SystemIdMap.class), any(UUID.class));
    }

    @Test
    public void shouldMapDocumentIdToMaterialId() {
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any(UUID.class))).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));
        target.mapMaterialIdToDocumentId(mappedDocumentId, mappedMaterialId);
        final SystemIdMap systemIdMap = systemIdMapArgumentCaptor.getValue();

        verify(systemIdMapperClient).add(systemIdMap, userId);
        assertThat(systemIdMap.getSourceId(), is(mappedMaterialId.toString()));
        assertThat(systemIdMap.getTargetId(), is(mappedDocumentId));
    }

    @Test
    public void shouldThrowExceptionWhenMappingAlreadyExists() {
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);
        final AdditionResponse response = new AdditionResponse(randomUUID(), CONFLICT, empty());

        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(response);

        try {
            target.mapNotificationIdToCaseId(mappedCppCaseId, notificationId);
            fail();
        } catch (final IllegalStateException e) {
            assertThat(e.getMessage(), is(format("Failed to map case Id: %s to notification id %s", mappedCppCaseId, notificationId)));
        }
    }

    @Test
    public void shouldThrowExceptionWhenSystemIdNotAvailable() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(empty());

        try {
            target.mapNotificationIdToCaseId(mappedCppCaseId, notificationId);
            fail();
        } catch (final ContextSystemUserIdException expected) {
            assertThat(expected.getMessage(), is("System user id not available for progression context"));
        }
    }

}
