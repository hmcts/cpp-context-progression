package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateConfirmed;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;

/**
 * 
 * Compares two defendants for any updates Defendant coming in updateDefendant command is compared
 * for any updates to Person, Interpreter, BailStatus, CustodyTimeLimitDate and SolicitorsFirm
 *
 */
@SuppressWarnings("squid:S1067")
public class DefendantUpdateHelper {
    private DefendantUpdateHelper(){

    }
    public static boolean isDefendantUpdated(final UpdateDefendantCommand updateDefendantCommand,
                    final Person person, final String bailStatus, final Interpreter interpreter,
                    final LocalDate custodyTimeLimitDate, final String solicitorsFirm) {


        return isDefendantPersonUpdated(Optional.ofNullable(updateDefendantCommand.getPerson()),
                        person)
                        || isDefendantBailStatusUpdated(
                                        Optional.ofNullable(updateDefendantCommand.getBailStatus()),
                                        bailStatus)
                        || isdefendantInterpreterUpdated(
                                        Optional.ofNullable(
                                                        updateDefendantCommand.getInterpreter()),
                                        interpreter)
                        || isDefendantCustodyTimeLimitUpdated(
                                        Optional.ofNullable(updateDefendantCommand
                                                        .getCustodyTimeLimitDate()),
                                        custodyTimeLimitDate)
                        || isDefendantSolicitorFirmUpdated(
                                        Optional.ofNullable(updateDefendantCommand
                                                        .getDefenceSolicitorFirm()),
                                        solicitorsFirm);
    }

    public static DefendantUpdateConfirmed createDefendantUpdateConfirmedEvent(
                    final UpdateDefendantCommand updateDefendantCommand,
                    final Person existingDefendantPerson) {
        final UUID defendantId = updateDefendantCommand.getDefendantId();

        return new DefendantUpdateConfirmed(updateDefendantCommand.getCaseId(), defendantId,
                        createPersonForDefendant(
                                        updateDefendantCommand.getPerson() == null
                                                        ? existingDefendantPerson
                                                        : updateDefendantCommand.getPerson(),
                                        existingDefendantPerson),
                        updateDefendantCommand.getInterpreter(),
                        updateDefendantCommand.getBailStatus(),
                        updateDefendantCommand.getCustodyTimeLimitDate(),
                        updateDefendantCommand.getDefenceSolicitorFirm());
    }

    private static Person createPersonForDefendant(final Person commandPerson,
                    final Person savedPerson) {

        return new Person(commandPerson.getId(), commandPerson.getTitle(),
                        commandPerson.getFirstName(), commandPerson.getLastName(),
                        commandPerson.getDateOfBirth(), commandPerson.getNationality(),
                        commandPerson.getGender(),
                        commandPerson.getHomeTelephone() == null ? savedPerson.getHomeTelephone()
                                        : commandPerson.getHomeTelephone(),
                        commandPerson.getWorkTelephone() == null ? savedPerson.getWorkTelephone()
                                        : commandPerson.getWorkTelephone(),
                        commandPerson.getMobile() == null ? savedPerson.getMobile()
                                        : commandPerson.getMobile(),
                        commandPerson.getFax() == null ? savedPerson.getFax()
                                        : commandPerson.getFax(),
                        commandPerson.getEmail() == null ? savedPerson.getEmail()
                                        : commandPerson.getEmail(),
                        commandPerson.getAddress());
    }
    private static boolean isDefendantPersonUpdated(final Optional<Person> commandDefendantPerson,
                    final Person savedDefendantPerson) {

        if (commandDefendantPerson.isPresent()) {
            final Person updateDefendantCommandPerson = commandDefendantPerson.get();
            return !(isDefendantPersonPersonalDetailsUpdated(updateDefendantCommandPerson,
                            savedDefendantPerson)
                            || new EqualsBuilder().append(
                                            updateDefendantCommandPerson.getAddress() == null
                                                            ? savedDefendantPerson.getAddress()
                                                            : updateDefendantCommandPerson
                                                                            .getAddress(),
                                            savedDefendantPerson.getAddress()).isEquals()
                            || isDefendantPersonComsDetailsUpdated(updateDefendantCommandPerson,
                                            savedDefendantPerson));
        }
        return false;

    }

    private static boolean isDefendantPersonPersonalDetailsUpdated(
                    final Person updateDefendantCommandPerson, final Person savedDefendantPerson) {
        return !new EqualsBuilder()
                        .append(updateDefendantCommandPerson.getTitle() == null
                                        ? savedDefendantPerson.getTitle()
                                        : updateDefendantCommandPerson.getTitle(),
                                        savedDefendantPerson.getTitle())
                        .append(updateDefendantCommandPerson.getFirstName() == null
                                        ? savedDefendantPerson.getFirstName()
                                        : updateDefendantCommandPerson.getFirstName(),
                                        savedDefendantPerson.getFirstName())
                        .append(updateDefendantCommandPerson.getLastName() == null
                                        ? savedDefendantPerson.getLastName()
                                        : updateDefendantCommandPerson.getLastName(),
                                        savedDefendantPerson.getLastName())
                        .append(updateDefendantCommandPerson.getDateOfBirth() == null
                                        ? savedDefendantPerson.getDateOfBirth()
                                        : updateDefendantCommandPerson.getDateOfBirth(),
                                        savedDefendantPerson.getDateOfBirth())
                        .append(updateDefendantCommandPerson.getNationality() == null
                                        ? savedDefendantPerson.getNationality()
                                        : updateDefendantCommandPerson.getNationality(),
                                        savedDefendantPerson.getNationality())
                        .append(updateDefendantCommandPerson.getGender() == null
                                        ? savedDefendantPerson.getGender()
                                        : updateDefendantCommandPerson.getGender(),
                                        savedDefendantPerson.getGender())
                        .isEquals();
    }

    private static boolean isDefendantPersonComsDetailsUpdated(
                    final Person updateDefendantCommandPerson,
                    final Person savedDefendantPerson) {
        return !new EqualsBuilder()
                        .append(updateDefendantCommandPerson.getHomeTelephone() == null
                                        ? savedDefendantPerson.getHomeTelephone()
                                        : updateDefendantCommandPerson.getHomeTelephone(),
                                        savedDefendantPerson.getHomeTelephone())
                        .append(updateDefendantCommandPerson.getWorkTelephone() == null
                                        ? savedDefendantPerson.getWorkTelephone()
                                        : updateDefendantCommandPerson.getWorkTelephone(),
                                        savedDefendantPerson.getWorkTelephone())
                        .append(updateDefendantCommandPerson.getMobile() == null
                                        ? savedDefendantPerson.getMobile()
                                        : updateDefendantCommandPerson.getMobile(),
                                        savedDefendantPerson.getMobile())
                        .append(updateDefendantCommandPerson.getEmail() == null
                                        ? savedDefendantPerson.getEmail()
                                        : updateDefendantCommandPerson.getEmail(),
                                        savedDefendantPerson.getEmail())
                        .append(updateDefendantCommandPerson.getFax() == null
                                        ? savedDefendantPerson.getFax()
                                        : updateDefendantCommandPerson.getFax(),
                                        savedDefendantPerson.getFax())
                        .isEquals();

    }

    private static boolean isDefendantBailStatusUpdated(final Optional<String> commandBailStatus,
                    final String defendantBailStatus) {
        return commandBailStatus.isPresent() ? !commandBailStatus.get().equals(defendantBailStatus)
                        : false;
    }

    private static boolean isdefendantInterpreterUpdated(
                    final Optional<Interpreter> commandInterpreter,
                    final Interpreter defendantInterpreter) {
        return commandInterpreter.isPresent()
                        ? !commandInterpreter.get().equals(defendantInterpreter)
                        : false;
    }

    private static boolean isDefendantCustodyTimeLimitUpdated(
                    final Optional<LocalDate> commandCustodyTimeLimit,
                    final LocalDate defendantCustodyTimeLimit) {
        return commandCustodyTimeLimit.isPresent()
                        ? !commandCustodyTimeLimit.get().equals(defendantCustodyTimeLimit)
                        : false;
    }

    private static boolean isDefendantSolicitorFirmUpdated(
                    final Optional<String> commandDefenceSolicitorFirm,
                    final String defendantSolicitorFirm) {
        return commandDefenceSolicitorFirm.isPresent()
                        ? !commandDefenceSolicitorFirm.get().equals(defendantSolicitorFirm)
                        : false;
    }
}
