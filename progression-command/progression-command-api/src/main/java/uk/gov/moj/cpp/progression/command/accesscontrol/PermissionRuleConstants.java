package uk.gov.moj.cpp.progression.command.accesscontrol;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionRuleConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRuleConstants.class);

    private static final String BCM = "BCM";
    private static final String PTPH = "PTPH";
    private static final String CREATE = "Create";
    private static final String EDIT = "Edit";
    private static final String FINALISE = "Finalise";
    private static final String OBJECT = "object";
    private static final String ACTION = "action";

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private PermissionRuleConstants() {
    }

    public static String petFormGrantAccessPermission() {
        return createObjectBuilder().add(OBJECT, "PetForm").add(ACTION, "GrantAccess").build().toString();
    }

    public static String petFormFinaliseAccessPermission() {
        return createObjectBuilder().add(OBJECT, "PET").add(ACTION, FINALISE).build().toString();
    }

    public static String getBCMCreatePermission() {
        return buildPermissions(BCM, CREATE);
    }

    public static String getPTPHCreatePermission() {
        return buildPermissions(PTPH, CREATE);
    }

    public static String getBCMFinalisePermission() {
        return buildPermissions(BCM, FINALISE);
    }

    public static String getPTPHFinalisePermission() {
        return buildPermissions(PTPH, FINALISE);
    }

    public static String getBCMEditPermission() {
        return buildPermissions(BCM, EDIT);
    }

    public static String getPTPHEditPermission() {
        return buildPermissions(PTPH, EDIT);
    }

    private static String buildPermissions(String object, String action) {
        final ExpectedPermission expectedPermission = builder()
                .withObject(object)
                .withAction(action)
                .build();
        return convertExpectedPermissionToString(expectedPermission);
    }

    private static String convertExpectedPermissionToString(final ExpectedPermission value){
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LOGGER.error("Converting Expected Permission To String {}", e);
            return StringUtils.EMPTY;
        }
    }

    public static String adhocHearingCreatePermission() {
        return createObjectBuilder().add(OBJECT, "AdhocHearing").add(ACTION, CREATE).build().toString();
    }
}
