
/******************************************************************************** 
	component-centric-delta-mrcm-attribute-domain-valid-attributecardinality

	Assertion:
	AttributeCardinality value is in ('0..1','0..*') in MRCM ATTRIBUTE DOMAIN delta file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' AttributeCardinality value is not in ("0..1","0..*") in MRCM ATTRIBUTE DOMAIN delta file'),
		a.id,
        'curr_mrcmattributedomainrefset_d'
	from curr_mrcmattributedomainrefset_d a
	where a.attributecardinality NOT IN ('0..1', '0..*');
	commit;
