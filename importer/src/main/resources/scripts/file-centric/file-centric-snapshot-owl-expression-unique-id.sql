
/******************************************************************************** 
	file-centric-snapshot-owl-expression-unique-id

	Assertion:
	The current OWL Expression snapshot file does not contain duplicate
	OWL Expression Ids

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		referencedcomponentid,
		concat('OWL Expression: id=',id, ' is repeated in the OWL Expression snapshot file.')
	from curr_owlexpressionrefset_s
	group by id
	having count(id) > 1;