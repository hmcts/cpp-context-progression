package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.CONFLICT;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.OK;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.progression.exception.ContextSystemUserIdException;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

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
    private SystemIdMapperService systemIdMapperService;

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExists() {

        final UUID notificationId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();
        final UUID userId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), notificationId.toString(), SystemIdMapperService.SOURCE_TYPE, mappedCppCaseId, SystemIdMapperService.TARGET_TYPE, now());

        when(systemIdMapperClient.findBy(notificationId.toString(), SystemIdMapperService.SOURCE_TYPE, SystemIdMapperService.TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        assertThat(systemIdMapping.getTargetId(), is(mappedCppCaseId));
    }


    @Test
    public void shouldAddNotificationToCaseIdMapping() {

        final UUID notificationId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();
        final UUID userId = randomUUID();
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));

        systemIdMapperService.mapNotificationIdToCaseId(mappedCppCaseId, notificationId);

        verify(systemIdMapperClient).add(any(SystemIdMap.class), any(UUID.class));
    }


    @Test
    public void shouldThrowExceptionWhenMappingAlreadyExists() {
        final UUID notificationId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();
        final UUID userId = randomUUID();
        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        final AdditionResponse response = new AdditionResponse(randomUUID(), CONFLICT, empty());
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(response);

        try {
            systemIdMapperService.mapNotificationIdToCaseId(mappedCppCaseId, notificationId);
            fail();
        } catch (final IllegalStateException e) {
            assertThat(e.getMessage(), is(format("Failed to map case Id: %s to notification id %s", mappedCppCaseId, notificationId)));
        }
    }

    @Test
    public void shouldThrowExceptionWhenSystemIdNotAvailable() {
        final UUID notificationId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(empty());

        try {
            systemIdMapperService.mapNotificationIdToCaseId(mappedCppCaseId, notificationId);
            fail();
        } catch (final ContextSystemUserIdException expected) {
            assertThat(expected.getMessage(), is("System user id not available for progression context"));
        }
    }


}