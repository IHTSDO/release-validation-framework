
/******************************************************************************** 
	release-type-snapshot-simple-refset-successive-states

	Assertion:	
	New inactive states for existing components must have active states in the previous SIMPLE REFSET snapshot

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Simple Refset: id=',a.id, ' should not have a new inactive state as it was inactive previously.'),
		a.id,
		'curr_simplerefset_s'
	from curr_simplerefset_s a , prev_simplerefset_s b
	where 
	a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.effectivetime != b.effectivetime;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Simple Refset: id=',a.id, ' is inactive but no active state found in the previous snapshot.'),
		a.id,
		'curr_simplerefset_s'
	from curr_simplerefset_s a left join prev_simplerefset_s b
	on a.id=b.id
	where a.active = '0'
	and b.id is null;
	