
/******************************************************************************** 
	file-centric-snapshot-language-unique-id

	Assertion:
	The current Language Refset snapshot file does not contain duplicate 
	Member Ids.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',id, ': Member Id is repeated in the language refset snapshot file.') 
	from curr_langrefset_s
	group by id
	having count(id) > 1;