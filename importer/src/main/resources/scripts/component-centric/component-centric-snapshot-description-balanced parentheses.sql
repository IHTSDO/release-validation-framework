
/******************************************************************************** 
	component-centric-snapshot-description-balanced parentheses

	Assertion:
	Active Terms of active concepts contain balanced parentheses.

********************************************************************************/
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: Term=',a.term, ':Active Terms of active concept without balanced parentheses.'),
		a.id,
		'curr_description_s'
	from curr_description_s  a , curr_concept_s b
	where (LENGTH(term) - LENGTH(REPLACE(term, '(', ''))) - (LENGTH(term) - LENGTH(REPLACE(term, ')', ''))) !=0 
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id;
	commit;
	