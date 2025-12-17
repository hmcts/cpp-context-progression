package uk.gov.moj.cpp.progression.aggregate.rules;

public interface RetentionRule {
    boolean apply();

    RetentionPolicy getPolicy();
}
