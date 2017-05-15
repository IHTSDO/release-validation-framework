/*  
	The current stated relationship delta file is an accurate derivative of the current full file
*/

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Mrcm Domain Refset: id=',a.id, ' is in delta file, but not in FULL file.')
	from curr_mrcmdomainrefset_d a
	left join curr_mrcmdomainrefset_f b
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
	where b.id is null
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
		or b.guideurl is null;
		