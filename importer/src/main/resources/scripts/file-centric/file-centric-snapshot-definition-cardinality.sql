
/******************************************************************************** 
	file-centric-snapshot-definition-cardinality.sql
	Assertion:
	There is at most one active definition per concept per language per dialect in the DEFINITION snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('CONCEPT: id=',a.conceptid, ' has more than one active definition per concept per dialect in the DEFINITION snapshot.') 	
	from curr_textdefinition_s a	
	where a.active = 1
	group by a.conceptid,a.languagecode,binary a.term
	having  count(a.conceptid) > 1;
