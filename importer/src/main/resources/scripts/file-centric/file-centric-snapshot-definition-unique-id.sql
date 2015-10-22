
/******************************************************************************** 
	file-centric-snapshot-definition-unique-id

	Assertion:
	ID is unique in the DEFINITION snapshot.

********************************************************************************/	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TextDefinition: id=',a.id, ' is not unique in the TextDefinition Snapshot file.')
	from curr_textdefinition_s a	
	group by a.id
	having  count(a.id) > 1;