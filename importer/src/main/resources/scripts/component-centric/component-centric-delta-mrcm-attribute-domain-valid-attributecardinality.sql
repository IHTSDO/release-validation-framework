
/******************************************************************************** 
	component-centric-delta-mrcm-attribute-domain-valid-attributecardinality

	Assertion:
	AttributeCardinality value is in ('0..1','0..*') in MRCM ATTRIBUTE DOMAIN delta file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' AttributeCardinality value is not in ("0..1","0..*") in MRCM ATTRIBUTE DOMAIN delta file') 	
	from curr_mrcmAttributeDomainRefset_d a	
	where a.attributecardinality NOT IN ('0..1', '0..*');
	commit;
