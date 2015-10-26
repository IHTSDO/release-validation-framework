
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-valid-sourceid

	Assertion:
	All source ids found in the Inferred Relationship snapshot file exist in the Concept snapshot file

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.destinationid,
		concat('RELATIONSHIP: id=',a.id, ': Inferred Relationship contains source id that does not exist in the Concept snapshot file.') 	
	from curr_relationship_s a
	left join curr_concept_s b on a.sourceid = b.id
	where b.id is null;