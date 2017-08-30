/*  
 * There must be actual changes made to previously published MRCM Attribute Domain Refset in order for them to appear in the current delta.
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Mrcm Attribute Domain Refset: id=',a.id, ' is in the detla file, but no actual changes made since the previous release.')
	from curr_mrcmAttributeDomainRefset_d a
	left join prev_mrcmAttributeDomainRefset_s b
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
	where b.id is not null;
