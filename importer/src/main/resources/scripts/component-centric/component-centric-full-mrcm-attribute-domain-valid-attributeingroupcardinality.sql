
/******************************************************************************** 
	component-centric-full-mrcm-attribute-domain-valid-attributeingroupcardinality

	Assertion:
	AttributeInGroupCardinality value is in ('0..0', '0..1', '0..*') in MRCM ATTRIBUTE DOMAIN full file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' AttributeInGroupCardinality value is not in ("0..0", "0..1", "0..*") in MRCM ATTRIBUTE DOMAIN full file') 	
	from curr_mrcmAttributeDomainRefset_f a	
	where a.attributeingroupcardinality NOT IN ('0..0','0..1','0..*');
	commit;
