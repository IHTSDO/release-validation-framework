
/******************************************************************************** 

	release-type-full-validation-mrcm-domain-refset 

	Assertion:	The current MRCM Domain Refset full file contains all 
	previously published data unchanged.


	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.	
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM DOMAIN REFSET: id=',a.id, ' is in prior full file but not in current full file.') 	
	from prev_mrcmDomainRefset_f a
	left join curr_mrcmDomainRefset_f b
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
	