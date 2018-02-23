
/*  
	The current full MRCM Domain Refset file consists of the previously published full file and the changes for the current release
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedcomponentid,
	concat('Mrcm Domain Refset: id=',a.id, ' is in current full file, but not in prior full or current delta file.')
	from curr_mrcmDomainRefset_f a
	left join curr_mrcmDomainRefset_d b
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
	left join prev_mrcmDomainRefset_f c
		on a.id = c.id
		and a.effectivetime = c.effectivetime
		and a.active = c.active
		and a.moduleid = c.moduleid
		and a.refsetid = c.refsetid
		and a.referencedcomponentid = c.referencedcomponentid
		and a.domainconstraint = c.domainconstraint
		and a.parentdomain = c.parentdomain
		and a.proximalprimitiveconstraint = c.proximalprimitiveconstraint
		and a.proximalprimitiverefinement = c.proximalprimitiverefinement
		and a.domaintemplateforprecoordination = c.domaintemplateforprecoordination
		and a.domaintemplateforpostcoordination = c.domaintemplateforpostcoordination
		and a.guideurl = c.guideurl
	where ( b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.refsetid is null
		or b.referencedcomponentid is null
		or b.domainconstraint is null
		or b.parentdomain is null
		or b.proximalprimitiveconstraint is null
		or b.proximalprimitiverefinement is null
		or b.domaintemplateforprecoordination is null
		or b.domaintemplateforpostcoordination is null
		or b.guideurl is null)
		and ( c.id is null
		or c.effectivetime is null
		or c.active is null
		or c.moduleid is null
		or c.refsetid is null
		or c.referencedcomponentid is null
		or c.domainconstraint is null
		or c.parentdomain is null
		or c.proximalprimitiveconstraint is null
		or c.proximalprimitiverefinement is null
		or c.domaintemplateforprecoordination is null
		or c.domaintemplateforpostcoordination is null
		or c.guideurl is null);
commit;


