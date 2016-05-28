
/******************************************************************************** 
	file-centric-snapshot-association-successive-states

	Assertion:	
	New inactive states follow active states in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ASSOC RS: id=',a.id, ': should not be inactive as it is inactive in the previous release already') 	
	from curr_associationrefset_s a inner join prev_associationrefset_s b
	on a.id = b.id
	where a.active = 0
	and a.active = b.active
	and a.effectivetime != b.effectivetime;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ASSOC RS: id=',a.id, ': should not be inactive as no active state found in previous release.') 	
	from curr_associationrefset_s a left join prev_associationrefset_s b
	on a.id = b.id
	where a.active=0 and b.id is null;
	commit;