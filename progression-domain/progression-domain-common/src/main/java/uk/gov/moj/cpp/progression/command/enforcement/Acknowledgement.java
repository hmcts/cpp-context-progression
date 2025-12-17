package uk.gov.moj.cpp.progression.command.enforcement;


import java.io.Serializable;

public class Acknowledgement implements Serializable {
    private static final long serialVersionUID = -2221378401462966080L;

    private String accountNumber;

    private String errorCode;

    private String errorMessage;

    public Acknowledgement(final String accountNumber, final String errorCode, final String errorMessage) {
        this.accountNumber = accountNumber;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static Builder acknowledgement() {
        return new Acknowledgement.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Acknowledgement that = (Acknowledgement) obj;

        return java.util.Objects.equals(this.accountNumber, that.accountNumber) &&
                java.util.Objects.equals(this.errorCode, that.errorCode) &&
                java.util.Objects.equals(this.errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountNumber, errorCode, errorMessage);
    }

    @Override
    public String toString() {
        return "Acknowledgement{" +
                "accountNumber='" + accountNumber + "'," +
                "errorCode='" + errorCode + "'," +
                "errorMessage='" + errorMessage + "'" +
                "}";
    }

    public Acknowledgement setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public Acknowledgement setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public Acknowledgement setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public static class Builder {
        private String accountNumber;

        private String errorCode;

        private String errorMessage;

        public Builder withAccountNumber(final String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder withErrorCode(final String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder withErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Acknowledgement build() {
            return new Acknowledgement(accountNumber, errorCode, errorMessage);
        }
    }
}
