
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-valid-attributecardinality

	Assertion:
	AttributeCardinality value is in ('0..1','0..*') in MRCM ATTRIBUTE DOMAIN snapshot file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' AttributeCardinality value is not in ("0..1","0..*") in MRCM ATTRIBUTE DOMAIN snapshot file') 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.active = 1
	and a.attributecardinality NOT IN ('0..1', '0..*');
	commit;
