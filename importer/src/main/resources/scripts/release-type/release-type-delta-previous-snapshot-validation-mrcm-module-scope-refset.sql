/*  
 * There must be actual changes made to previously published MRCM Module Scope Refset in order for them to appear in the current delta.
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Mrcm Module Scope Refset: id=',a.id, ' is in the detla file, but no actual changes made since the previous release.')
	from curr_mrcmModuleScopeRefset_d a
	left join prev_mrcmModuleScopeRefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.mrcmrulerefsetid = b.mrcmrulerefsetid
	where b.id is not null;
