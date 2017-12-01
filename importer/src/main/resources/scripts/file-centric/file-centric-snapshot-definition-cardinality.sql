
/******************************************************************************** 
	file-centric-snapshot-definition-cardinality.sql
	Assertion:
	There is at most one active definition per concept per dialect.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Concept id=',a.conceptid, ' has more than one active definitions in dialect:', b.refsetid) 	
	from curr_textdefinition_s a,
	curr_langrefset_s b
	where a.active = 1 
	and b.active=1 
	and b.referencedcomponentid =a.id
	group by a.conceptid,b.refsetid
	having count(b.referencedcomponentid) > 1;