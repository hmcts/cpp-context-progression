package uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransformFactory {

    private final Map<String, List<ProgressionEventTransformer>> transformEventMap;


    public TransformFactory() {
        transformEventMap = new HashMap<>();

        addInstance(MasterDefendantIdEventTransformer.getEventAndJsonPaths().keySet(), new MasterDefendantIdEventTransformer());
        addInstance(CourtProceedingsInitiatedEventTransformer.getEventAndJsonPaths().keySet(), new CourtProceedingsInitiatedEventTransformer());

    }

    private void addInstance(final Set<String> keySet, final ProgressionEventTransformer eventTransformer) {
        keySet.forEach(key -> transformEventMap.compute(key, (s, progressionEventTransformers) -> {
                    if (progressionEventTransformers == null) {
                        progressionEventTransformers = new ArrayList<>();
                    }
                    progressionEventTransformers.add(eventTransformer);
                    return progressionEventTransformers;
                }
        ));
    }

    public List<ProgressionEventTransformer> getEventTransformer(final String eventName) {
        return transformEventMap.get(eventName);
    }
}
