package uk.gov.moj.cpp.progression.query.view.service.transformer;

public enum DirectionPrompts {
    PROMPT1("3b81321f-c98b-473a-a3c0-ff5b55654138"),

    PROMPT2("2cd85752-8329-48f5-8708-a396c8e8835f"),

    PROMPT3("2062d38f-9801-4a91-8664-c48a5fd44006"),

    PROMPT4("3acdfec5-514b-475f-b935-238972a7ab37"),

    PROMPT5("9b1b362e-edc0-4e40-b6d9-31fe24d1047c"),

    PROMPT6("602ff24c-eb1b-4a60-9d10-225ae3b82d63"),

    PROMPT7("c80d3164-015e-44ae-ab5b-1627447f9b38"),

    PROMPT8("ae5f5def-4f45-4e34-89a6-4c1deaf49e17"),

    INVALID("INVALID");

    private final String value;

    DirectionPrompts(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static DirectionPrompts valueFor(final String value) {
        if (PROMPT1.value.equals(value)) {
            return PROMPT1;
        }
        if (PROMPT2.value.equals(value)) {
            return PROMPT2;
        }
        if (PROMPT3.value.equals(value)) {
            return PROMPT3;
        }
        if (PROMPT4.value.equals(value)) {
            return PROMPT4;
        }
        if (PROMPT5.value.equals(value)) {
            return PROMPT5;
        }
        if (PROMPT6.value.equals(value)) {
            return PROMPT6;
        }
        if (PROMPT7.value.equals(value)) {
            return PROMPT7;
        }

        if (PROMPT8.value.equals(value)) {
            return PROMPT8;
        }

        return INVALID;
    }
}
