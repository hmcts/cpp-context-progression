package uk.gov.moj.cpp.progression.service.payloads;

public class StatDecAppointmentLetterDefendant {

    private final String name;

    private final  StatDecAppointmentLetterDefendantAddress  address;

    public StatDecAppointmentLetterDefendant(String name, StatDecAppointmentLetterDefendantAddress address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public StatDecAppointmentLetterDefendantAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "StatDecAppointmentLetterDefendant{" +
                "name='" + name + '\'' +
                ", address=" + address +
                '}';
    }

    public static StatDecAppointmentLetterDefendant.Builder builder() {
        return new StatDecAppointmentLetterDefendant.Builder();
    }

    public static class Builder {

        private String name;

        private  StatDecAppointmentLetterDefendantAddress  address;


        public StatDecAppointmentLetterDefendant.Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public StatDecAppointmentLetterDefendant.Builder withAddress(final StatDecAppointmentLetterDefendantAddress address) {
            this.address = address;
            return this;
        }

        public StatDecAppointmentLetterDefendant build() {
            return new StatDecAppointmentLetterDefendant(name, address);
        }
    }


}
