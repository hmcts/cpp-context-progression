package uk.gov.justice.api.resource.utils.payload;

import static java.util.Optional.empty;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PleaValueDescriptionBuilder {
    private static final String DESCRIPTION = "description";
    private static final String DEFENDANT = "defendant";
    private static final String OFFENCES = "offences";
    private static final String PLEAS = "pleas";
    private static final String PLEA_VALUE = "pleaValue";

    public JsonObject rebuildWithPleaValueDescription(final JsonObject payload) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(payload);
        jsonNode.path(DEFENDANT).path(OFFENCES).forEach(offence ->
            offence.path(PLEAS).forEach(plea ->
                ((ObjectNode)plea).put(DESCRIPTION, PleaValueDescription.descriptionFor(plea.get(PLEA_VALUE).asText()).orElse(""))
            )
        );
        return objectMapper.treeToValue(jsonNode, JsonObject.class);
    }

    enum PleaValueDescription {
        UNFIT_TO_PLEAD("UNFIT_TO_PLEAD", "Unfit to plead"),

        NO_PLEA("NO_PLEA", "No plea"),

        AUTREFOIS_ACQUIT("AUTREFOIS_ACQUIT", "Autrefois acquit"),

        CHANGE_TO_NOT_GUILTY("CHANGE_TO_NOT_GUILTY", "Change of Plea: Guilty to not Guilty"),

        AUTREFOIS_CONVICT("AUTREFOIS_CONVICT", "Autrefois convict"),

        NOT_GUILTY("NOT_GUILTY", "Not Guilty"),

        ADMITS("ADMITS","Admits"),

        DENIES("DENIES","Denies"),

        GUILTY_TO_A_LESSER_OFFENCE_NAMELY("GUILTY_TO_A_LESSER_OFFENCE_NAMELY", "Guilty to a lesser offence namely"),

        GUILTY_TO_AN_ALTERNATIVE_OFFENCE_NOT_CHARGED_NAMELY("GUILTY_TO_AN_ALTERNATIVE_OFFENCE_NOT_CHARGED_NAMELY","Guilty to an alternative offence not charged namely"),

        GUILTY("GUILTY", "Guilty"),

        CHANGE_TO_GUILTY_AFTER_SWORN_IN("CHANGE_TO_GUILTY_AFTER_SWORN_IN", "Change of Plea: Not Guilty to Guilty (After Jury sworn in)"),

        CHANGE_TO_GUILTY_MAGISTRATES_COURT("CHANGE_TO_GUILTY_MAGISTRATES_COURT", "Change of Plea: Not Guilty to Guilty  (Magistrates Court)"),

        CHANGE_TO_GUILTY_NO_SWORN_IN("CHANGE_TO_GUILTY_NO_SWORN_IN", "Change of Plea: Not Guilty to Guilty (No Jury sworn in)"),

        OPPOSES("OPPOSES", "Opposes"),

        MCA_GUILTY("MCA_GUILTY", "MCA Guilty"),

        CONSENTS("CONSENTS", "Consents"),

        PARDON("PARDON", "Pardon");

        private final String value;
        private final String description;

        PleaValueDescription(final String value, final String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public String toString() {
            return value;
        }

        public static Optional<String> descriptionFor(final String value) {
            for (final PleaValueDescription plea : PleaValueDescription.values()) {
                if (plea.value.equals(value)) {
                    return Optional.of(plea.description);
                }
            }
            return empty();
        }
    }
}
