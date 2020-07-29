
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
	left join (select r1.id as id
                    from curr_relationship_d r1 left join curr_relationship_d r2
                                                on r1.id=r2.id
                                                    and r1.moduleid<>r2.moduleid
                                                    and r1.sourceid=r2.sourceid
                                                    and r1.destinationid=r2.destinationid
                                                    and r1.typeid=r2.typeid
                    where r2.id is not null
                        and r1.active=1
                        and r2.active=0
                        and CONVERT(r1.effectivetime, SIGNED INTEGER) < CONVERT(r2.effectivetime, SIGNED INTEGER)) c
    on a.id=c.id
	where a.active=0
	and b.id is null
	and c.id is null;