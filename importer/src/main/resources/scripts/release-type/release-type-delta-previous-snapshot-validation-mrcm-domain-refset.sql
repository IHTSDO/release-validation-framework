/*  
 * There must be actual changes made to previously published MRCM Domain Refset in order for them to appear in the current delta.
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Mrcm Domain Refset: id=',a.id, ' is in the detla file, but no actual changes made since the previous release.')
	from curr_mrcmDomainRefset_d a
	left join prev_mrcmDomainRefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.domainconstraint = b.domainconstraint
		and a.parentdomain = b.parentdomain
		and a.proximalprimitiveconstraint = b.proximalprimitiveconstraint
		and a.proximalprimitiverefinement = b.proximalprimitiverefinement
		and a.domaintemplateforprecoordination = b.domaintemplateforprecoordination
		and a.domaintemplateforpostcoordination = b.domaintemplateforpostcoordination
		and a.guideurl = b.guideurl
	where b.id is not null;
