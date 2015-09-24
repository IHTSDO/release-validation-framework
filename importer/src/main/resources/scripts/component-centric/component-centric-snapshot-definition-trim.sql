
/******************************************************************************** 
	file-centric-snapshot-definition-trim

	Assertion:
	No active definitions contain leading or trailing spaces.

********************************************************************************/	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEF: id=',a.id, ':Active Terms with leading or trailing spaces.') 	
	from curr_textdefinition_s a 
	where a.active = 1
	and ( a.term != LTRIM(term) or a.term != RTRIM(term)); 
	commit;
	