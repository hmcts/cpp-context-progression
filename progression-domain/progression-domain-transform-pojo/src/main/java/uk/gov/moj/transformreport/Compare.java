package uk.gov.moj.transformreport;

import uk.gov.justice.v24.progression.events.listener.CaseAddedToCrownCourt;
import uk.gov.justice.v24.progression.events.listener.ConvictionDateAdded;
import uk.gov.justice.v24.progression.events.listener.ConvictionDateRemoved;
import uk.gov.justice.v24.progression.events.listener.CourtsDocumentCreated;
import uk.gov.justice.v24.progression.events.listener.CourtsDocumentRemoved;
import uk.gov.justice.v24.progression.events.listener.DefendantRequestCreated;
import uk.gov.justice.v24.progression.events.listener.HearingResulted;
import uk.gov.justice.v24.progression.events.listener.NowsMaterialStatusUpdated;
import uk.gov.justice.v24.progression.events.listener.PrintRequestAccepted;
import uk.gov.justice.v24.progression.events.listener.PrintRequestFailed;
import uk.gov.justice.v24.progression.events.listener.PrintRequestSucceeded;
import uk.gov.justice.v24.progression.events.listener.PrintRequested;
import uk.gov.justice.v24.progression.events.listener.ProsecutionCaseCreated;
import uk.gov.justice.v24.progression.events.listener.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.v24.progression.events.listener.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.v24.progression.events.listener.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.v24.progression.events.listener.ProsecutionCaseOffencesUpdated;
import uk.gov.moj.cpp.coredomain.tools.transform.SchemaExploreKt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */

public class Compare {
    public static void main(String args[]) {
        final Map<Class<?>, Class<?>> roots = new LinkedHashMap<>();

        //No change required
        roots.put(ConvictionDateAdded.class, uk.gov.justice.core.courts.ConvictionDateAdded.class);
        roots.put(ConvictionDateRemoved.class, uk.gov.justice.core.courts.ConvictionDateRemoved.class);
        roots.put(CourtsDocumentRemoved.class, uk.gov.justice.core.courts.CourtsDocumentRemoved.class);
        roots.put(DefendantRequestCreated.class, uk.gov.justice.core.courts.DefendantRequestCreated.class);
        roots.put(PrintRequestAccepted.class, uk.gov.justice.core.courts.PrintRequestAccepted.class);
        roots.put(PrintRequested.class, uk.gov.justice.core.courts.PrintRequested.class);
        roots.put(PrintRequestFailed.class, uk.gov.justice.core.courts.PrintRequestFailed.class);
        roots.put(PrintRequestSucceeded.class, uk.gov.justice.core.courts.PrintRequestSucceeded.class);
        roots.put(ProsecutionCaseDefendantUpdated.class, uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated.class);
        roots.put(NowsMaterialStatusUpdated.class, uk.gov.justice.core.courts.NowsMaterialStatusUpdated.class);
        roots.put(CaseAddedToCrownCourt.class, uk.gov.justice.core.courts.CaseAddedToCrownCourt.class);
        roots.put(CourtsDocumentCreated.class, uk.gov.justice.core.courts.CourtsDocumentCreated.class);
        roots.put(ProsecutionCaseDefendantHearingResultUpdated.class, uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated.class);

        // need transformation
        roots.put(HearingResulted.class, uk.gov.justice.hearing.courts.HearingResulted.class); //done
        roots.put(ProsecutionCaseCreated.class, uk.gov.justice.core.courts.ProsecutionCaseCreated.class); //done
        roots.put(ProsecutionCaseDefendantListingStatusChanged.class, uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged.class); //done
        roots.put(ProsecutionCaseOffencesUpdated.class, uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated.class); //done


        SchemaExploreKt.exploreParralel(roots,
                c -> c.getName().contains("uk.gov.justice")
        );
    }
}
