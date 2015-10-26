
/******************************************************************************** 
component-centric-snapshot-map-correlation-origin-valid-loinc-parts.sql

	Assertion:
	The LOINC parts in MapCorrelationOrigin refset snapshot are in correct format

********************************************************************************/
 
 insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('LOINC part:',a.mapTarget,' is not correctly formated in the MapCorrelationOrigin refset snapshot.')
 from curr_mapCorrelationOriginRefset_s a
 	where a.contentOriginId=705117003
 	and a.refsetId = 705112009
	and a.mapTarget not like "LP%-%";
 commit;
