
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-unique-id

	Assertion:
	The current Inferred Relationship snapshot file does not contain duplicate 
	Relationship Ids

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('RELATIONSHIP : id=',id, ': Relationship Id is repeated in the Inferred Relationship snapshot file.') 

	from curr_relationship_s
	group by id
	having count(id) > 1;