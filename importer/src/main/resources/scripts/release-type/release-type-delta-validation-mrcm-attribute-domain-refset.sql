/*  
	The current MRCM Attribute Domain Refset delta file is an accurate derivative of the current full file
*/

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Mrcm Attribute Domain Refset: id=',a.id, ' is in delta file, but not in FULL file.')
	from curr_mrcmattributedomainrefset_d a
	left join curr_mrcmattributedomainrefset_f b
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
		