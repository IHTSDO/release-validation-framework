
/******************************************************************************** 
	release-type-snapshot-inferred-relationship-successive-states

	Assertion:
	All relationships inactivated in current release must have been active in the previous release

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Relationship: id=',a.id, ' should not have a new inactive state as it was inactive previously.') 
	from curr_relationship_s a
	inner join prev_relationship_s b on a.id = b.id 
	where a.active = '0' 
	and b.active = '0' 
	and a.effectivetime != b.effectivetime
	and a.moduleid = b.moduleid
	and a.sourceid = b.sourceid
	and a.relationshipgroup = b.relationshipgroup
	and a.destinationid = b.destinationid
	and a.typeid = b.typeid
	and a.characteristictypeid = b.characteristictypeid
	and a.modifierid = b.modifierid;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Relationship: id=',a.id, ' is inactive but no active state found in previous release.') 
	from curr_relationship_s a
	left join prev_relationship_s b
	on a.id=b.id
	and a.sourceid=b.sourceid
	and a.destinationid=b.destinationid
	and a.typeid=b.typeid
	where a.active=0 
	and b.id is null;