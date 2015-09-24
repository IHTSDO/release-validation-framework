
/******************************************************************************** 
	file-centric-snapshot-definition-unique-terms

	Assertion:
	There are no active duplicate Definition terms in the DEFINITION snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Definition id =', a.id,': Term=[',a.term, '] is duplicate in the DEFINITION snapshot file.') 	
	from curr_textdefinition_s a
	where active = 1
	group by BINARY  a.term
	having count(a.term) > 1 ;