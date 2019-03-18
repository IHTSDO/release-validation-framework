
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
	from curr_textdefinition_s a,
	(select b.term from curr_textdefinition_s b group by b.typeid, b.conceptid, b.languagecode, binary b.term having count(b.id) > 1) duplicate
	where a.term = duplicate.term
	and a.active = 1;
	commit;