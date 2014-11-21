
/******************************************************************************** 
	file-centric-snapshot-stated-relationship-valid-sourceid

	Assertion:
	All source ids found in the Stated Relationship snapshot file exist in the Concept snapshot file

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('RELATIONSHIP: id=',a.id, ': Stated Relationship contains a source id that does not exist in the Concept snapshot file.') 	
	from curr_stated_relationship_s a
	left join curr_concept_s b on a.sourceid = b.id
	where b.id is null;