
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-successive-states

	Assertion:
	All relationships inactivated in current release must have been active in the previous release

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.id,
		concat('RELATIONSHIP: id=',a.id, ': Inferred Relationship Id is inactived in current release, yet was already inactive in previous release.') 
	from curr_relationship_s a
	inner join prev_relationship_s b on a.id = b.id 
	where a.active = '0' 
	and b.active = '0' 
	and a.effectivetime != b.effectivetime;
	