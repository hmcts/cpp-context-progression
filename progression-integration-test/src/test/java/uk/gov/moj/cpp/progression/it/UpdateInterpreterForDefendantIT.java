package uk.gov.moj.cpp.progression.it;

import com.google.common.base.Strings;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateInterpreterForDefendantHelper;

import java.util.UUID;

import static uk.gov.moj.cpp.progression.it.UpdateInterpreterForDefendantIT.PayloadBuilder.INTERPRETER_NEEDED;
import static uk.gov.moj.cpp.progression.it.UpdateInterpreterForDefendantIT.PayloadBuilder.NO_INTERPRETER_NEEDED;


public class UpdateInterpreterForDefendantIT extends BaseIntegrationTest {

    private String caseId;
    private UpdateInterpreterForDefendantHelper interpreterHelper;
    private String defendantId;

    /**
     * Creates the case and adds a defendant
     */
    @Before
    public void setUp() throws Exception {
        caseId= UUID.randomUUID().toString();
        defendantId = addDefendant();
        interpreterHelper = getUpdateInterpreterHelper(defendantId);
    }

    /**
     * Updates interpreter 'needed' to true and sets required 'language':
     *      "interpreter": {
     *        "needed": true,
     *        "language": "French"
     *      }
     * Verifies that the interpreter 'needed' is true and 'language' correctly set.
     */
    @Test
    public void updateInterpreterNeededToTrueWithRequiredLanguage() {
        // Given
        String payload = new PayloadBuilder(INTERPRETER_NEEDED).withLanguage("French").build();

        // When
        interpreterHelper.updateInterpreter(payload);

        // Then
        assertThatInterpreterUpdated();
    }

    /**
     * Updates interpreter 'needed' to false:
     *     "interpreter": {
     *        "needed": false
     *      }
     * Verifies that the interpreter 'needed' is false and no 'language' set.
     */
    @Test
    public void updateInterpreterNeededToFalse() {
        // Given
        String payload = new PayloadBuilder(NO_INTERPRETER_NEEDED).build();

        // When
        interpreterHelper.updateInterpreter(payload);

        // Then
        assertThatInterpreterUpdated();
    }

    /**
     * Updates interpreter 'needed' to true and sets required 'language':
     *      "interpreter": {
     *        "needed": true,
     *        "language": "German"
     *      }
     * Verifies that the interpreter 'needed' is true and 'language' correctly set.
     * Then updates interpreter 'needed' to false:
     *      "interpreter": {
     *        "needed": false
     *      }
     * Verifies that the interpreter 'needed' is false and that 'language' is no longer set.
     */
    @Test
    public void updateInterpreterNeededToTrueAndThenChangeToFalse() {
        // Given
        String initialPayload = new PayloadBuilder(INTERPRETER_NEEDED).withLanguage("German").build();
        String updatedPayload = new PayloadBuilder(NO_INTERPRETER_NEEDED).build();
        givenInterpreterUpdatedToInitialState(initialPayload);

        // When
        interpreterHelper.updateInterpreter(updatedPayload);

        // Then
        assertThatInterpreterUpdated();
    }

    private void givenInterpreterUpdatedToInitialState(String initialPayload) {
        interpreterHelper.updateInterpreter(initialPayload);
        assertThatInterpreterUpdated();
    }

    /*
     * Adds the defendant.
     * @return String the id of the added defendant.
     */
    private String addDefendant() {
        AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId);
        addDefendantHelper.addMinimalDefendant();
        assertThatDefendantAdded(addDefendantHelper);
        return addDefendantHelper.getDefendantId();
    }


    private UpdateInterpreterForDefendantHelper getUpdateInterpreterHelper(String defendantID) {
        return new UpdateInterpreterForDefendantHelper(caseId, defendantID);
    }

    // Assertion functions

    private void assertThatDefendantAdded(AddDefendantHelper addDefendantHelper) {
        addDefendantHelper.verifyInActiveMQ();
        addDefendantHelper.verifyMinimalDefendantAdded();
    }

    private void assertThatInterpreterUpdated() {
        interpreterHelper.verifyInActiveMQ();
        interpreterHelper.verifyInterpreterForDefendantUpdated();
    }

    static class PayloadBuilder {

        static final boolean INTERPRETER_NEEDED = true;
        static final boolean NO_INTERPRETER_NEEDED = !INTERPRETER_NEEDED;

        private static final String NEEDED_KEY = "needed";
        private static final String LANGUAGE_KEY = "language";
        private static final String INTERPRETER_KEY = "interpreter";

        private boolean needed;
        private String language;

        public PayloadBuilder(boolean needed) {
            this.needed = needed;
        }

        public PayloadBuilder withLanguage(String language) {
            this.language = language;
            return this;
        }

        public String build() {
            JSONObject payload = new JSONObject();
            JSONObject interpreter = new JSONObject();
            interpreter.put(NEEDED_KEY, needed);
            if (hasValue(language)) interpreter.put(LANGUAGE_KEY, language);
            payload.put(INTERPRETER_KEY, interpreter);
            return payload.toString();
        }

        private boolean hasValue(String input) {
            return !Strings.isNullOrEmpty(input);
        }
    }
}
