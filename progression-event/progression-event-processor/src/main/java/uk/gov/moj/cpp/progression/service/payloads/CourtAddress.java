package uk.gov.moj.cpp.progression.service.payloads;

public class CourtAddress {

    private final String room;

    private final String house;

    private final String address1;

    private final String address2;

    private final String address3;

    private final String address4;

    private final String address5;

    private final String postCode;

    public CourtAddress(String room, String house, String address1, String address2, String address3, String address4, String address5, String postCode) {
        this.room = room;
        this.house = house;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postCode = postCode;
    }

    public String getRoom() {
        return room;
    }

    public String getHouse() {
        return house;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getAddress5() {
        return address5;
    }

    public String getPostCode() {
        return postCode;
    }

    @Override
    public String toString() {
        return "CourtAddress{" +
                "room='" + room + '\'' +
                ", house='" + house + '\'' +
                ", address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", address3='" + address3 + '\'' +
                ", address4='" + address4 + '\'' +
                ", address5='" + address5 + '\'' +
                ", postCode='" + postCode + '\'' +
                '}';
    }

    public static CourtAddress.Builder builder() {
        return new CourtAddress.Builder();
    }

    public static class Builder {
        private  String room;

        private  String house;

        private  String address1;

        private  String address2;

        private  String address3;

        private  String address4;

        private  String address5;

        private  String postCode;

        public CourtAddress.Builder withRoom(final String room) {
            this.room = room;
            return this;
        }

        public CourtAddress.Builder withHouse(final String house) {
            this.house = house;
            return this;
        }

        public CourtAddress.Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }

        public CourtAddress.Builder withAddress2(final String address2) {
            this.address2 = address2;
            return this;
        }

        public CourtAddress.Builder withAddress3(final String address3) {
            this.address3 = address3;
            return this;
        }

        public CourtAddress.Builder withAddress4(final String address4) {
            this.address4 = address4;
            return this;
        }

        public CourtAddress.Builder withAddress5(final String address5) {
            this.address5 = address5;
            return this;
        }

        public CourtAddress.Builder withPostCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        public CourtAddress build() {
            return new CourtAddress(room, house, address1, address2,address3, address4, address5, postCode);
        }


    }


}
