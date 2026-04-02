package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MaterialIdMapping;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.QueryParam;

public class MaterialBulkRepository {

    @PersistenceContext
    private EntityManager entityManager;


    @SuppressWarnings("unchecked")
    public List<MaterialIdMapping> findMaterialIdMappingsInBulk(@QueryParam("materialIds") final List<UUID> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }

        final String uuidValues = materialIds.stream()
                .map(uuid -> "('" + uuid.toString() + "'\\:\\:uuid)")
                .collect(Collectors.joining(","));

        final String sql = "WITH inputs(material_id) AS (\n" +
                "  SELECT * FROM (VALUES " + uuidValues + ") AS t(material_id)\n" +
                "),\n" +
                "    links AS (\n" +
                "      SELECT\n" +
                "        cdm.material_id,\n" +
                "        cdm.court_document_id,\n" +
                "        cdi.prosecution_case_id AS case_id\n" +
                "      FROM inputs i\n" +
                "      JOIN court_document_material cdm ON cdm.material_id = i.material_id\n" +
                "      JOIN court_document_index    cdi ON cdi.court_document_id = cdm.court_document_id\n" +
                "    ),\n" +
                "    ranked AS (\n" +
                "      SELECT *,\n" +
                "             ROW_NUMBER() OVER (\n" +
                "               PARTITION BY material_id\n" +
                "               ORDER BY\n" +
                "                 court_document_id DESC\n" +
                "             ) AS rn\n" +
                "      FROM links\n" +
                "    )\n" +
                "    SELECT\n" +
                "      l.material_id,\n" +
                "      l.court_document_id,\n" +
                "      l.case_id,\n" +
                "      spc.reference AS caseurn\n" +
                "    FROM ranked l\n" +
                "    INNER JOIN search_prosecution_case spc ON spc.case_id = l.case_id\\:\\:text\n" +
                "    WHERE l.rn = 1\n" +
                "    ORDER BY l.material_id";

        final Query query = entityManager.createNativeQuery(sql, "MaterialIdMappingResult");
        return query.getResultList();
    }
}