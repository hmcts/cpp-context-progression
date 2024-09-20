package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialEnricherTest {

    @Mock
    private DocumentUserGroupFinder documentUserGroupFinder;

    @Mock
    private UtcClock clock;

    @InjectMocks
    private MaterialEnricher materialEnricher;

    @Test
    public void shouldEnrichMaterialWithCorrectUserGroups() throws Exception {

        final UUID materialId = randomUUID();
        final String generationStatus = "generationStatus";
        final String name = "name";
        final ZonedDateTime now = new UtcClock().now();
        final ZonedDateTime uploadTime = now.minusDays(2);
        final ZonedDateTime receivedDate = now.minusMinutes(5);

        final List<String> userGroups = asList("group_1", "group_2");

        final Material material = mock(Material.class);
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);

        when(material.getId()).thenReturn(materialId);
        when(material.getGenerationStatus()).thenReturn(generationStatus);
        when(material.getName()).thenReturn(name);
        when(material.getUploadDateTime()).thenReturn(uploadTime);
        when(material.getReceivedDateTime()).thenReturn(receivedDate);
        when(documentUserGroupFinder.getReadUserGroupsFrom(documentTypeAccess)).thenReturn(userGroups);

        final Material enrichedMaterial = materialEnricher.enrichMaterial(
                material,
                documentTypeAccess);

        assertThat(enrichedMaterial.getId(), is(materialId));
        assertThat(enrichedMaterial.getGenerationStatus(), is(generationStatus));
        assertThat(enrichedMaterial.getName(), is(name));
        assertThat(enrichedMaterial.getUploadDateTime(), is(uploadTime));
        assertThat(enrichedMaterial.getReceivedDateTime(), is(receivedDate));
        assertThat(enrichedMaterial.getUserGroups(), is(userGroups));
    }

    @Test
    public void shouldUserCurrentDateIfNoUploadDateExists() throws Exception {

        final UUID materialId = randomUUID();
        final String generationStatus = "generationStatus";
        final String name = "name";
        final ZonedDateTime now = new UtcClock().now();
        final ZonedDateTime receivedDate = now.minusMinutes(5);

        final List<String> userGroups = asList("group_1", "group_2");

        final Material material = mock(Material.class);
        final DocumentTypeAccess documentTypeRBACData = mock(DocumentTypeAccess.class);

        when(material.getId()).thenReturn(materialId);
        when(material.getGenerationStatus()).thenReturn(generationStatus);
        when(material.getName()).thenReturn(name);
        when(material.getUploadDateTime()).thenReturn(null);
        when(clock.now()).thenReturn(now);
        when(material.getReceivedDateTime()).thenReturn(receivedDate);
        when(documentUserGroupFinder.getReadUserGroupsFrom(documentTypeRBACData)).thenReturn(userGroups);

        final Material enrichedMaterial = materialEnricher.enrichMaterial(
                material,
                documentTypeRBACData);

        assertThat(enrichedMaterial.getId(), is(materialId));
        assertThat(enrichedMaterial.getGenerationStatus(), is(generationStatus));
        assertThat(enrichedMaterial.getName(), is(name));
        assertThat(enrichedMaterial.getUploadDateTime(), is(now));
        assertThat(enrichedMaterial.getReceivedDateTime(), is(receivedDate));
        assertThat(enrichedMaterial.getUserGroups(), is(userGroups));
    }
}
