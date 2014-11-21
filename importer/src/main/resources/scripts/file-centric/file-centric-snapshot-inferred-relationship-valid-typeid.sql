
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-valid-typeid

	Assertion:
	All type ids found in the Inferred Relationship snapshot file exist in the Concept snapshot file

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('RELATIONSHIP: id=',a.id, ': Inferred Relationship contains type id that does not exist in the Concept snapshot file.') 	
	from curr_relationship_s a
	left join curr_concept_s b on a.typeid = b.id
	where b.id is null;