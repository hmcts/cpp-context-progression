package uk.gov.moj.cpp.progression.helper;

public class EventSelector {

    public static final String EVENT_SELECTOR_BAIL_STATUS_UPDATED =
            "progression.events.bail-status-updated-for-suspect";
    public static final String EVENT_SELECTOR_BAIL_STATUS_UPDATED_FOR_DEFENDANT =
            "progression.events.bail-status-updated-for-defendant";
    public static final String EVENT_SELECTOR_CASE_COMPLETED = "progression.events.case-completed";
    public static final String EVENT_SELECTOR_CASE_CREATED = "progression.events.case-created";
    public static final String EVENT_SELECTOR_CASE_EXHIBIT_ADDED = "progression.events.exhibit-added";
    public static final String PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDED = "public.progression.defendant-added";
    public static final String PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDITION_FAILED = "public.progression.defendant-addition-failed";
    public static final String EVENT_SELECTOR_DEFENDANT_ADDED = "progression.events.defendant-added";
    public static final String EVENT_SELECTOR_DEFENDANT_UPDATED = "progression.events.defendant-updated";
    public static final String EVENT_SELECTOR_DEFENDANT_ADDITION_FAILED = "progression.events.defendant-addition-failed";
    public static final String EVENT_SELECTOR_OFFENCE_FOR_DEFENDANT_ADDED = "progression.events.offence-for-defendant-added";
    public static final String EVENT_SELECTOR_OFFENCES_FOR_DEFENDANT_UPDATED = "progression.events.offences-for-defendant-updated";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "progression.events.case-document-added";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS = "progression.events.case-document-already-exists";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS = "public.progression.case-document-already-exists";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED = "public.progression.case-document-uploaded";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "public.progression.case-document-added";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_NAME_UPDATED = "progression.events.case-document-name-updated";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_CLASSIFICATION_UPDATED = "progression.events.case-document-classification-updated";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_CLASSIFICATION_UPDATED = "public.progression.case-document-classification-updated";
    public static final String EVENT_SELECTOR_IDPC_APPROVED =
            "progression.events.idpc-approved-for-suspect";
    public static final String EVENT_SELECTOR_INDICATED_PLEA_UPDATED =
            "progression.events.indicated-plea-updated";
    public static final String EVENT_SELECTOR_PLEA_UPDATED = "progression.events.plea-updated";
    public static final String EVENT_SELECTOR_PLEA_CANCELLED = "progression.events.plea-cancelled";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_UPDATED = "public.progression.plea-updated";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED = "public.progression.plea-cancelled";

    public static final String EVENT_SELECTOR_POLICE_SUMMARY_ADDED = "progression.events.police-summary-added";
    public static final String EVENT_SELECTOR_CASE_MARKER_ADDED = "progression.events.case-markers-added";
    public static final String EVENT_SELECTOR_CASE_MARKER_REMOVED = "progression.events.case-markers-removed";
    public static final String PUBLIC_EVENT_SELECTOR_POLICE_SUMMARY_ADDED = "public.progression.police-summary-added";
    public static final String EVENT_SELECTOR_SJP_CASE_CREATED =
            "progression.events.sjp-case-created";

    public static final String PUBLIC_EVENT_SELECTOR_SJP_CASE_CREATED =
            "public.progression.sjp-case-created";

    public static final String PUBLIC_EVENT_SELECTOR_CASE_CREATED =
            "public.progression.case-created";

    public static final String EVENT_SELECTOR_SUSPECT_ADDED = "progression.events.suspect-added";
    public static final String PUBLIC_EVENT_SELECTOR_SUSPECT_ADDED = "public.progression.suspect-added";
    public static final String EVENT_SELECTOR_SUSPECT_UPDATED = "progression.events.suspect-updated";
    public static final String PUBLIC_EVENT_SELECTOR_SUSPECT_UPDATED = "public.progression.suspect-updated";
    public static final String PUBLIC_EVENT_SELECTOR_SUSPECT_ALREADY_EXISTS = "public.progression.suspect-already-exists";
    public static final String EVENT_SELECTOR_VICTIM_ADDED = "progression.events.victim-added";
    public static final String PUBLIC_EVENT_SELECTOR_VICTIM_ADDED = "public.progression.victim-added";
    public static final String PUBLIC_EVENT_SELECTOR_WITNESS_ADDED = "public.progression.witness-added";
    public static final String EVENT_SELECTOR_WITNESS_CLASSIFICATIONS_REPLACED = "progression.events.witness-classifications-replaced";
    public static final String PUBLIC_EVENT_SELECTOR_WITNESS_CLASSIFICATIONS_REPLACED = "public.progression.witness-classifications-replaced";
    public static final String EVENT_SELECTOR_WITNESS_CLASSIFICATION_ADDED =
            "progression.events.witness-classification-added";
    public static final String EVENT_SELECTOR_STATEMENT_ADDED = "progression.events.statement-added";
    public static final String PUBLIC_EVENT_SELECTOR_STATEMENT_ADDED = "public.progression.statement-added";

    public static final String EVENT_SELECTOR_PERSON_TYPE_UPDATED = "progression.events.person-type-updated";
    public static final String PUBLIC_EVENT_SELECTOR_PERSON_TYPE_UPDATED = "public.progression.person-type-updated";
    public static final String EVENT_SELECTOR_WITNESS_NOT_FOUND = "progression.events.witness-not-found";
    public static final String EVENT_SELECTOR_VICTIM_NOT_FOUND = "progression.events.victim-not-found";

    public static final String EVENT_SELECTOR_MARK_CASE_SENSITIVE =
            "progression.events.sensitive-marked";
    public static final String EVENT_SELECTOR_OWNERSHIP_TRANSFERRED_TO_CMS =
            "progression.events.ownership-transferred";
    public static final String EVENT_SELECTOR_DEFENCE_SOLICITOR_FIRM_UPDATED_FOR_DEFENDANT =
            "progression.events.defence-solicitor-firm-for-defendant-updated";
    public static final String EVENT_SELECTOR_INTERPRETER_UPDATED_FOR_DEFENDANT =
            "progression.events.interpreter-for-defendant-updated";
    public static final String EVENT_SELECTOR_VICTIM_DELETED = "progression.events.victim-deleted";
    public static final String PUBLIC_EVENT_SELECTOR_VICTIM_DELETED = "public.progression.victim-deleted";
    public static final String EVENT_SELECTOR_VICTIM_CLASSIFICATIONS_REPLACED = "progression.events.victim-classifications-replaced";
    public static final String PUBLIC_EVENT_SELECTOR_VICTIM_CLASSIFICATIONS_REPLACED = "public.progression.victim-classifications-replaced";
    public static final String EVENT_SELECTOR_WITNESS_DELETED = "progression.events.witness-deleted";
    public static final String PUBLIC_EVENT_SELECTOR_WITNESS_DELETED = "public.progression.witness-deleted";

    public static final String EVENT_SELECTOR_ADD_ACTION_ITEM =
            "progression.events.action-item-added";
    public static final String EVENT_SELECTOR_UPDATE_ACTION_ITEM =
            "progression.events.action-item-updated";
    public static final String EVENT_SELECTOR_DELETE_ACTION_ITEM =
            "progression.events.action-item-deleted";

    public static final String EVENT_SELECTOR_MATERIAL_READ_RECORDED = "progression.events.material-read-recorded";
    public static final String PUBLIC_EVENT_SELECTOR_MATERIAL_READ_RECORDED = "public.progression.material-read-recorded";


    public static final String EVENT_SELECTOR_STORAGE_REFERENCE_FOR_EXHIBIT_SET = "progression.events.storage-reference-set";
    public static final String EVENT_SELECTOR_EXHIBIT_NAME_CHANGED = "progression.events.exhibit-name-changed";
    public static final String EVENT_SELECTOR_EXHIBIT_NOT_FOUND = "progression.events.exhibit-not-found";
    public static final String PUBLIC_SELECTOR_STRUCTURE_STORAGE_REFERENCE_UPDATED = "public.progression.storage-reference-updated";
    public static final String PUBLIC_SELECTOR_STRUCTURE_EXHIBIT_NOT_FOUND = "public.progression.exhibit-not-found";

    public static final String PUBLIC_SELECTOR_STRUCTURE_NAME_CHANGED = "public.progression.exhibit-name-changed";
    public static final String PUBLIC_SELECTOR_STRUCTURE_EXHIBIT_ADDED = "public.progression.exhibit-added";
    public static final String PUBLIC_SELECTOR_STRUCTURE_CASE_DOCUMENT_NAME_UPDATED = "public.progression.case-document-name-updated";

    public static final String PUBLIC_STRUCTURE_ALL_OFFENCES_WITHDRAWAL_REQUESTED = "public.progression.all-offences-withdrawal-requested";
    public static final String STRUCTURE_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED = "progression.events.all-offences-withdrawal-requested";

    public static final String PUBLIC_STRUCTURE_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = "public.progression.all-offences-withdrawal-request-cancelled";
    public static final String STRUCTURE_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = "progression.events.all-offences-withdrawal-request-cancelled";
    public static final String PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED = "public.progression.case-update-rejected";
    public static final String STRUCTURE_EVENTS_CASE_UPDATE_REJECTED = "progression.events.case-update-rejected";


    public static final String PUBLIC_SELECTOR_STRUCTURE_ACTION_ITEM_ADDED = "public.progression.action-item-added";
    public static final String PUBLIC_SELECTOR_STRUCTURE_ACTION_ITEM_UPDATED = "public.progression.action-item-updated";
    public static final String PUBLIC_SELECTOR_STRUCTURE_ACTION_ITEM_DELETED = "public.progression.action-item-deleted";
    public static final String PUBLIC_SELECTOR_STRUCTURE_ACTION_PLAN_DELETED = "public.progression.action-plan-deleted";

    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "progression.events.case-reopened-in-libra";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "public.progression.case-reopened-in-libra";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "progression.events.case-reopened-in-libra-updated";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "public.progression.case-reopened-in-libra-updated";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "progression.events.case-reopened-in-libra-undone";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "public.progression.case-reopened-in-libra-undone";

    public static final String EVENT_SELECTOR_CASE_STARTED = "progression.events.case-started";

    public static final String EVENT_SELECTOR_INVALID_CASE_MARKER_RECEIVED = "progression.events.case-markers-rejected";
    public static final String PUBLIC_SELECTOR_STRUCTURE_UPDATE_CASE_MARKERS_OUTCOMES = "public.progression.update-case-markers-outcomes";

    public static final String EVENT_SELECTOR_STRUCTURE_EVENTS_EXHIBIT_ALREADY_EXISTS = "progression.events.exhibit-already-exists";

    public static final String PUBLIC_SELECTOR_STRUCTURE_EXHIBIT_ALREADY_EXISTS = "public.progression.exhibit-already-exists";
    public static final String EVENT_SELECTOR_STRUCTURE_EVENTS_STATEMENT_ALREADY_EXISTS = "progression.events.statement-already-exists";
    public static final String PUBLIC_SELECTOR_STRUCTURE_STATEMENT_ALREADY_EXISTS = "public.progression.statement-already-exists";
    public static final String EVENT_SELECTOR_STRUCTURE_EVENTS_ACTION_NOT_PERMITTED = "progression.events.action-not-permitted";
    public static final String PUBLIC_STRUCTURE_POLICE_SUMMARY_ALREADY_EXISTS =  "public.progression.police-summary-already-exists";

}