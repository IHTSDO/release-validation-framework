
/******************************************************************************** 
	file-centric-snapshot-simple-refset-successive-states

	Assertion:	
	New inactive states for existing components must have active states in the previous SIMPLE REFSET snapshot

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Simple Refset: id=',a.id, ' should not have a new invalid inactive state in the SIMPLE REFSET snapshot.') 	
	from curr_simplerefset_s a , prev_simplerefset_s b
	where 
	a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.effectivetime != b.effectivetime;