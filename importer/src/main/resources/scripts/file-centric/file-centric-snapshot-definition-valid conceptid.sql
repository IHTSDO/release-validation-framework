
/******************************************************************************** 
	file-centric-snapshot-definition-valid conceptid

	Assertion:
	ConceptId value refers to valid concept identifier in DEFINITION snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DEFINITION: id=',a.id, ' refers to an invalid concept id in the Text Definition snapshot.') 	
	from curr_textdefinition_s a
	left join curr_concept_s b
	on a.conceptid = b.id
	where b.id is null;