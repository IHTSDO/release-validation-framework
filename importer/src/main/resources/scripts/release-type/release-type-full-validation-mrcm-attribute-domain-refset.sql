
/******************************************************************************** 

	release-type-full-validation-mrcm-attribute-domain-refset 

	Assertion:	The current MRCM Attribute Domain Refset full file contains all 
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
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id, ' is in prior full file but not in current full file.') 	
	from prev_mrcmAttributeDomainRefset_f a
	left join curr_mrcmAttributeDomainRefset_f b
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
	