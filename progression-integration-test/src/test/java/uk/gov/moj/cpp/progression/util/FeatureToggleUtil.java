package uk.gov.moj.cpp.progression.util;

import static uk.gov.moj.cpp.progression.AbstractIT.PROGRESSION_CONTEXT;

import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import com.google.common.collect.ImmutableMap;

public class FeatureToggleUtil {

    public static final void enableDefenceDisclosureFeature(final boolean enabled) {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("defenceDisclosure", enabled);
        FeatureStubber.stubFeaturesFor(PROGRESSION_CONTEXT, features);
    }
}
