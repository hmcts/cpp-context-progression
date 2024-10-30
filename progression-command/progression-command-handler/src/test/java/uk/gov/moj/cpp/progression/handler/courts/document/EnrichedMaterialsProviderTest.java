package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EnrichedMaterialsProviderTest {

    @Mock
    private MaterialEnricher materialEnricher;

    @InjectMocks
    private EnrichedMaterialsProvider enrichedMaterialsProvider;

    @Test
    public void shouldGetTheListOfMaterialsAndEnrich() throws Exception {

        final CourtDocument courtDocument = mock(CourtDocument.class);
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);

        final Material material_1 = mock(Material.class);
        final Material material_2 = mock(Material.class);
        final Material enrichedMaterial_1 = mock(Material.class);
        final Material enrichedMaterial_2 = mock(Material.class);

        when(courtDocument.getMaterials()).thenReturn(asList(material_1, material_2));
        when(materialEnricher.enrichMaterial(material_1, documentTypeAccess)).thenReturn(enrichedMaterial_1);
        when(materialEnricher.enrichMaterial(material_2, documentTypeAccess)).thenReturn(enrichedMaterial_2);

        final List<Material> enrichedMaterials = enrichedMaterialsProvider.getEnrichedMaterials(
                courtDocument,
                documentTypeAccess);

        assertThat(enrichedMaterials.size(), is(2));
        assertThat(enrichedMaterials.get(0), is(enrichedMaterial_1));
        assertThat(enrichedMaterials.get(1), is(enrichedMaterial_2));
    }
}
