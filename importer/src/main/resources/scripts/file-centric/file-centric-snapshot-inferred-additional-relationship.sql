/******************************************************************************** 
	file-centric-snapshot-inferred-additional-relationship.sql

	Assertion:
	There are no active additional relationships added in the current release.
Note: RVF-249 
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Relationship: id=',a.id, ' is an active additional relationship.') 	
	from curr_relationship_d a 
	where a.characteristictypeid=900000000000227009
	and a.active=1;