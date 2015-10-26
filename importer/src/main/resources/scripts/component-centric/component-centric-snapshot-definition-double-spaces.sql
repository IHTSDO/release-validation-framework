
/******************************************************************************** 
	file-centric-snapshot-definition-double-spaces

	Assertion:
	No active definiitons associated with active concept contain double spaces.

********************************************************************************/
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('TEXTDEF : id=',a.id, ':Active Terms of active concept containing double spaces.') 	
	from curr_textdefinition_s a , curr_concept_s b
	where a.active = 1
	and a.conceptid = b.id
	and b.active = 1
	and a.term like '%  %'; 
	commit;



	