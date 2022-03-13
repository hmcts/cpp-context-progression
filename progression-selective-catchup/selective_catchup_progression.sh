# !/bin/bash
#
#####################################################################################################################
# Author: Shyam                                                                               Date: 23/02/2022      #
# Description: This script procures the Event IDs from EventStore and restores into ViewStore for fixing duplicate  #
#              reporting restrictions.                                                                              #
#                                                                                                                   #
# V1.0    2022-02-20    Shyam     Original version                                                                  #
# v1.1    2022-02-25    Arcadius Ahouansou: Added union and delete stetements                                       #
#####################################################################################################################

# Retrieve password from Vault for Production

# Create an ARRAY with 3 elements, hostname, DBname, username

#export SELECTIVE_CATCHUP_DB_CONN="host=psf-dev-ccm02-progression.postgres.database.azure.com port=5432 dbname=progressionviewstore user=progression password=progression sslmode=require"
export SELECTIVE_CATCHUP_DB_CONN="postgresql://progression:progression@localhost/progressionviewstore"
psql -v ON_ERROR_STOP=1 ${SELECTIVE_CATCHUP_DB_CONN} <<EOF
   \echo '===Copying streamids to csv file'
   
   \COPY ((select distinct (hearingid) as stream_id from (select *, count(1) from ( select hearing_id as hearingId, offe->'id' as offenceID, jsonb_array_elements( offe->'reportingRestrictions' )->'label' as rrLabels, jsonb_array_elements(offe->'reportingRestrictions')->'judicialResultId' as rrJrids from ( select hearing_id, jsonb_array_elements( jsonb_array_elements( jsonb_array_elements( payload::jsonb->'prosecutionCases' )->'defendants' )->'offences' ) as offe from hearing ) as streamInfo ) as streamCount group by hearingId, offenceid, rrLabels, rrJrids having count(1)>1 order by count(1) ) as distinctStream) union (select distinct (caseid) as stream_id from( select *, count(1) from (select id as caseId, offe->'id' as offenceID, jsonb_array_elements(offe->'reportingRestrictions')->'label' as rrLabels, jsonb_array_elements(offe->'reportingRestrictions')->'judicialResultId' as rrJrids from (select id, jsonb_array_elements(jsonb_array_elements(payload::jsonb->'defendants')->'offences') as offe from prosecution_case ) as streamInfo ) as streamCount group by caseId, offenceid, rrLabels, rrJrids having count(1)>1 ) as distinctStream) union (select distinct (stream_id) from ( select *, count(1) from ( select id as stream_id, offe->'id' as offenceID, jsonb_array_elements(offe->'reportingRestrictions')->'label' as rrLabels, jsonb_array_elements(offe->'reportingRestrictions')->'judicialResultId' as rrJrids from (select id, jsonb_array_elements(payload::jsonb->'courtOrder'->'courtOrderOffences')->'offence' as offe from court_application) as streamInfo) as streamCount group by stream_id, offenceid, rrLabels, rrJrids having count(1)>1 ) as distinctStream) union (select distinct (stream_id) from ( select *, count(1) from ( select id as stream_id, offe->'id' as offenceID, jsonb_array_elements(offe->'reportingRestrictions')->'label' as rrLabels, jsonb_array_elements(offe->'reportingRestrictions')->'judicialResultId' as rrJrids from (select id, jsonb_array_elements(jsonb_array_elements(payload::jsonb->'courtApplicationCases')->'offences') as offe from court_application) as streamInfo) as streamCount group by stream_id, offenceid, rrLabels, rrJrids having count(1)>1 ) as distinctStream) union (select distinct  stream_id from ( ((select distinct (hearingid) as stream_id from (select *, count(1) from ( select hearing_id as hearingId, offe->'id' as offenceID  from ( select hearing_id, jsonb_array_elements( jsonb_array_elements( jsonb_array_elements( payload::jsonb->'prosecutionCases' )->'defendants' )->'offences' ) as offe from hearing ) as streamInfo ) as streamCount group by hearingId, offenceid  having count(1)>1 order by count(1) ) as distinctStream) union (select distinct (caseid) as stream_id from( select *, count(1) from (select id as caseId, offe->'id' as offenceID from (select id, jsonb_array_elements(jsonb_array_elements(payload::jsonb->'defendants')->'offences') as offe from prosecution_case ) as streamInfo ) as streamCount group by caseId, offenceid having count(1)>1 ) as distinctStream) union  (select distinct (stream_id) from ( select *, count(1) from ( select id as stream_id, offe->'id' as offenceID  from (select id, jsonb_array_elements(payload::jsonb->'courtOrder'->'courtOrderOffences')->'offence' as offe from court_application) as streamInfo) as streamCount group by stream_id, offenceid  having count(1)>1 ) as distinctStream) union (select distinct (stream_id) from ( select *, count(1) from ( select id as stream_id, offe->'id' as offenceID from (select id, jsonb_array_elements(jsonb_array_elements(payload::jsonb->'courtApplicationCases')->'offences') as offe from court_application) as streamInfo) as streamCount group by stream_id, offenceid  having count(1)>1 ) as distinctStream)) ) as bbb)) TO '/tmp/streamids.csv' DELIMITER ',' CSV HEADER;

   \echo '===Connecting to eventstore'
   \c progressioneventstore
   
   \echo '===Dropping tmp_stream_id'
   DROP TABLE IF EXISTS tmp_stream_id CASCADE;
   
   \echo '===Creating tmp_stream_id'
   CREATE TABLE tmp_stream_id( stream_id uuid primary key);

   \echo '===Loading data from streamids.csv'
   \COPY tmp_stream_id FROM '/tmp/streamids.csv' CSV header;

   \echo '===Copying data into eventids_streamid.csv'
   \COPY (SELECT el.id, el.stream_id FROM event_log el, tmp_stream_id tsid WHERE el.stream_id = tsid.stream_id) TO '/tmp/eventids_streamid.csv' DELIMITER ',' CSV HEADER;

   \echo '===Clearing tmp_stream_id'
   DROP TABLE IF EXISTS tmp_stream_id CASCADE;

   \echo '===Connecting to viewstore'
   \c progressionviewstore
   
   \echo '===Dropping tmp_selective_cu_stream'
   DROP TABLE IF EXISTS tmp_selective_cu_stream CASCADE;
   
   \echo '===Creating tmp_selective_cu_stream'
   CREATE TABLE tmp_selective_cu_stream(event_id uuid primary key, stream_id uuid not null);

   \echo '===Loading data into tmp_selective_cu_stream from eventids_streamid.csv'
   \COPY tmp_selective_cu_stream FROM '/tmp/eventids_streamid.csv' CSV header;

-- EXECUTE DELETE STATEMENT HERE


\echo '=== delete stream data from stream_status'

 delete
from
	stream_status
where
	component = 'EVENT_LISTENER'
	and stream_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);

\echo '=== delete stream data from stream_buffer'
delete
from
	stream_buffer
where
	component = 'EVENT_LISTENER'
	and stream_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);
	
\echo '=== delete from processed_event using event id'
 delete
from
	processed_event
where
	component = 'EVENT_LISTENER'
	and event_id in (
	select
		event_id
	from
		tmp_selective_cu_stream);


	
\echo '=== delete stream data from case_cps_prosecutor'
 delete
from
	case_cps_prosecutor
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);

		
\echo '=== delete stream data from case_defendant_hearing by caseid'	
 delete
from
	case_defendant_hearing
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);
		
\echo '=== delete stream data from case_defendant_hearing by hearingid'	
 delete
from
	case_defendant_hearing
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);


				
\echo '=== delete from case_link_split_merge'			
 delete
from
	case_link_split_merge
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);


				
\echo '=== delete from case_note'			
 delete
from
	case_note
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);


\echo '=== delete from caseprogressiondetail'			
 delete
from
	caseprogressiondetail
where
	caseid in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);





\echo '=== delete from court_application_case by case_id'			
 delete
from
	court_application_case
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from court_application_case by application_id'			
 delete
from
	court_application_case
where
	application_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from court_document_index by hearing_id'			
 delete
from
	court_document_index
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from court_document_index by prosecution_case_id'			
 delete
from
	court_document_index
where
	prosecution_case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);


\echo '=== delete from court_document_index by application_id'			
 delete
from
	court_document_index
where
	application_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from defendant by caseid'			
 delete
from
	defendant
where
	caseid in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from defendant_partial_match by '			
 delete
from
	defendant_partial_match
where
	prosecution_case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from hearing_application by hearing_id'			
 delete
from
	hearing_application
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from hearing_application by application_id'			
 delete
from
	hearing_application
where
	application_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from hearing_result_line '			
 delete
from
	hearing_result_line
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from informant_register '			
 delete
from
	informant_register
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from initiate_court_application '			
 delete
from
	initiate_court_application
where
	id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from match_defendant_case_hearing by hearing_id'			
 delete
from
	match_defendant_case_hearing
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from match_defendant_case_hearing by prosecution_case_id'			
 delete
from
	match_defendant_case_hearing
where
	prosecution_case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);




\echo '=== delete from notification_status by case_id'			
 delete
from
	notification_status
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from notification_status by application_id'			
 delete
from
	notification_status
where
	application_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from now_document_request'			
 delete
from
	now_document_request
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);



\echo '=== delete from pet_case_defendant_offence'			
 delete
from
	pet_case_defendant_offence
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);






\echo '=== delete from court_application'			
 delete
from
	court_application
where
	id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);






\echo '=== delete from prosecution_case'	
 delete
from
	prosecution_case
where
	id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);	



\echo '=== delete from search_prosecution_case'	
 delete
from
	search_prosecution_case
where
	case_id::uuid in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);	



\echo '=== delete from shared_court_document by hearing_id'	
 delete
from
	shared_court_document
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);	



\echo '=== delete from shared_court_document by case_id'	
 delete
from
	shared_court_document
where
	case_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);	





\echo '=== delete from hearing'			
 delete
from
	hearing
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);


-- Clear temporary table, once process completes
--   DROP TABLE IF EXISTS tmp_selective_cu_stream CASCADE;


EOF

rm -fr /tmp/streamids.csv
rm -fr /tmp/eventids_streamid.csv
