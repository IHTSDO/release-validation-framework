/*  
	The current MRCM Attribute Domain Refset snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_mrcmattributedomainrefset_v;
  	create table if not exists temp_mrcmattributedomainrefset_v like curr_mrcmAttributeDomainRefset_f;
  	insert into temp_mrcmattributedomainrefset_v
	select a.*
	from curr_mrcmAttributeDomainRefset_f a
	where cast(a.effectivetime as datetime) =
		(select max(cast(z.effectivetime as datetime))
		 from curr_mrcmAttributeDomainRefset_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Attribute Domain Refset: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.')
	from curr_mrcmAttributeDomainRefset_s a
	left join temp_mrcmattributedomainrefset_v b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.domainid = b.domainid
		and a.grouped = b.grouped
		and a.attributecardinality = b.attributecardinality
		and a.attributeingroupcardinality = b.attributeingroupcardinality
		and a.rulestrengthid = b.rulestrengthid
		and a.contenttypeid = b.contenttypeid
	where b.id is null
        or b.effectivetime is null
        or b.active is null
        or b.moduleid is null
        or b.refsetid is null
        or b.referencedcomponentid is null
        or b.domainid is null
        or b.grouped is null
        or b.attributecardinality is null
        or b.attributeingroupcardinality is null
        or b.rulestrengthid is null
        or b.contenttypeid is null;

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Attribute Domain Refset: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.')
	from temp_mrcmattributedomainrefset_v a
	left join curr_mrcmAttributeDomainRefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.domainid = b.domainid
		and a.grouped = b.grouped
		and a.attributecardinality = b.attributecardinality
		and a.attributeingroupcardinality = b.attributeingroupcardinality
		and a.rulestrengthid = b.rulestrengthid
		and a.contenttypeid = b.contenttypeid
	where b.id is null
        or b.effectivetime is null
        or b.active is null
        or b.moduleid is null
        or b.refsetid is null
        or b.referencedcomponentid is null
        or b.domainid is null
        or b.grouped is null
        or b.attributecardinality is null
        or b.attributeingroupcardinality is null
        or b.rulestrengthid is null
        or b.contenttypeid is null;

commit;
drop table if exists temp_mrcmattributedomainrefset_v;