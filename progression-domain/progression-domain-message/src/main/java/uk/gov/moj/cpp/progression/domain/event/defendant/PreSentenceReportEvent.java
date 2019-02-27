package uk.gov.moj.cpp.progression.domain.event.defendant;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class PreSentenceReportEvent {
    private final String provideGuidance;
    private final Boolean drugAssessment;
    private final Boolean psrIsRequested;

    private PreSentenceReportEvent(final Boolean psrIsRequested, final String provideGuidance, final Boolean drugAssessment) {
        this.provideGuidance = provideGuidance;
        this.drugAssessment = drugAssessment;
        this.psrIsRequested = psrIsRequested;
    }

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public Boolean getPsrIsRequested() {
        return psrIsRequested;
    }

    public static final class PreSentenceReportEventBuilder {
        private String provideGuidance;
        private Boolean drugAssessment;
        private Boolean psrIsRequested;

        private PreSentenceReportEventBuilder() {
        }

        public static PreSentenceReportEventBuilder aPreSentenceReportEvent() {
            return new PreSentenceReportEventBuilder();
        }

        public PreSentenceReportEventBuilder setProvideGuidance(final String provideGuidance) {
            this.provideGuidance = provideGuidance;
            return this;
        }

        public PreSentenceReportEventBuilder setDrugAssessment(final Boolean drugAssessment) {
            this.drugAssessment = drugAssessment;
            return this;
        }

        public PreSentenceReportEventBuilder setPsrIsRequested(final Boolean psrIsRequested) {
            this.psrIsRequested = psrIsRequested;
            return this;
        }

        public PreSentenceReportEvent build() {
            return new PreSentenceReportEvent(psrIsRequested, provideGuidance, drugAssessment);
        }

        public String getProvideGuidance() {
            return provideGuidance;
        }

        public Boolean getDrugAssessment() {
            return drugAssessment;
        }

        public Boolean getPsrIsRequested() {
            return psrIsRequested;
        }
        
    }
    
}