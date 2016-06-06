/******************************************************************************** 
	file-centric-snapshot-stated-relationship-characteristic-type-id.sql

	Assertion:
	The characteristic type id must be Stated in the stated relationship snapshot file
Note: RVF-249
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Stated relationship: id=',a.id, ' contains invalid characteristic type id:', a.characteristictypeid) 	
	from curr_stated_relationship_s a 
	where a.characteristictypeid!=900000000000010007;
