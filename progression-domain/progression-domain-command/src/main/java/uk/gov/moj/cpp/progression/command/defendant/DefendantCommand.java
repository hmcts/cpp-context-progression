package uk.gov.moj.cpp.progression.command.defendant;

import java.time.LocalDate;
import java.util.UUID;

public class DefendantCommand {
    private final UUID defendantProgressionId;
    private final UUID defendantId;
    private final AdditionalInformationCommand additionalInformation;


    public DefendantCommand(UUID defendantProgressionId, UUID defendantId,
                            AdditionalInformationCommand additionalInformation) {
        this.defendantProgressionId = defendantProgressionId;
        this.defendantId = defendantId;
        this.additionalInformation = additionalInformation;
    }

    public UUID getDefendantProgressionId() {
        return defendantProgressionId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public AdditionalInformationCommand getAdditionalInformationCommand() {
        return additionalInformation;
    }

    public static final class DefendantCommandBuilder {
        private UUID defendantProgressionId;
        private UUID defendantId;
        private AdditionalInformationCommand additionalInformation;

        private DefendantCommandBuilder() {
        }

        public static DefendantCommandBuilder aDefendantCommand() {
            return new DefendantCommandBuilder();
        }

        public DefendantCommandBuilder defendantProgressionId(UUID defendantProgressionId) {
            this.defendantProgressionId = defendantProgressionId;
            return this;
        }

        public DefendantCommandBuilder defendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantCommandBuilder additionalInformation(AdditionalInformationCommand additionalInformation) {
            this.additionalInformation = additionalInformation;
            return this;
        }

        public DefendantCommand build() {
            DefendantCommand defendant = new DefendantCommand(defendantProgressionId, defendantId, additionalInformation);
            return defendant;
        }
    }
}

















