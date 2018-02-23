
/******************************************************************************** 
	file-centric-snapshot-definition-immutable

	Assertion:
	There is a 1:1 relationship between the ID and the immutable values in DEFINITION snapshot.

********************************************************************************/

/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Definition: id=',a.id, ' references a term which is duplicated.') 	
	from curr_textdefinition_s a 
	group by a.typeid, a.languagecode, a.conceptid, binary a.term
	having count(a.id) > 1;
	commit;