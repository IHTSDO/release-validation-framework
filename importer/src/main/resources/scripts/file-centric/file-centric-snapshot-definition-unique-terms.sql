
/******************************************************************************** 
	file-centric-snapshot-definition-unique-terms

	Assertion:
	There are no active duplicate Definition terms in the DEFINITION snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Definition id =', a.id,': Term=[',a.term, '] is duplicate in the DEFINITION snapshot file.') 	
	from curr_textdefinition_s a, curr_concept_s b
	where a.active = 1 and b.active =1
	and a.conceptid = b.id
	group by BINARY  a.term
	having count(a.term) > 1 ;