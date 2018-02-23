
/*  
	The current full MRCM Attribute Domain Refset file consists of the previously published full file and the changes for the current release
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedcomponentid,
	concat('Mrcm Attribute Domain Refset: id=',a.id, ' is in current full file, but not in prior full or current delta file.')
	from curr_mrcmAttributeDomainRefset_f a
	left join curr_mrcmAttributeDomainRefset_d b
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
	left join prev_mrcmAttributeDomainRefset_f c
		on a.id = c.id
		and a.effectivetime = c.effectivetime
		and a.active = c.active
		and a.moduleid = c.moduleid
		and a.refsetid = c.refsetid
		and a.referencedcomponentid = c.referencedcomponentid
		and a.domainid = c.domainid
		and a.grouped = c.grouped
		and a.attributecardinality = c.attributecardinality
		and a.attributeingroupcardinality = c.attributeingroupcardinality
		and a.rulestrengthid = c.rulestrengthid
		and a.contenttypeid = c.contenttypeid
	where ( b.id is null
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
		or b.contenttypeid is null)
		and ( c.id is null
		or c.effectivetime is null
		or c.active is null
		or c.moduleid is null
		or c.refsetid is null
		or c.referencedcomponentid is null
		or c.domainid is null
		or c.grouped is null
		or c.attributecardinality is null
		or c.attributeingroupcardinality is null
		or c.rulestrengthid is null
		or c.contenttypeid is null);
commit;


