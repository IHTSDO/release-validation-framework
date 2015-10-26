
/******************************************************************************** 
	file-centric-snapshot-concept-unique-id

	Assertion:
	The current Concept snapshot file has unique identifiers.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ':Non unique id in current release file.') 	
	from curr_concept_s a
	group by a.id
	having  count(a.id) > 1;
	commit;