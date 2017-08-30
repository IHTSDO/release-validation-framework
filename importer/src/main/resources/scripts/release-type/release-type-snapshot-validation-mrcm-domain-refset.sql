/*  
	The current MRCM Domain Refset snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_mrcmdomainrefset_v;
  	create table if not exists temp_mrcmdomainrefset_v like curr_mrcmDomainRefset_f;
  	insert into temp_mrcmdomainrefset_v
	select a.*
	from curr_mrcmDomainRefset_f a
	where cast(a.effectivetime as datetime) =
		(select max(cast(z.effectivetime as datetime))
		 from curr_mrcmDomainRefset_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Domain Refset: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.')
	from curr_mrcmDomainRefset_s a
	left join temp_mrcmdomainrefset_v b
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

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Domain Refset: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.')
	from temp_mrcmdomainrefset_v a
	left join curr_mrcmDomainRefset_s b
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

commit;
drop table if exists temp_mrcmdomainrefset_v;