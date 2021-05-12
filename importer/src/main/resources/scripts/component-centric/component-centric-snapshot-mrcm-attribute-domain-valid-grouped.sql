
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-valid-grouped

	Assertion:
	Grouped value is either 0 or 1 in MRCM ATTRIBUTE DOMAIN snapshot file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' has an invalid value ', a.grouped, ' for the grouped field.'),
		a.id,
        'curr_mrcmattributedomainrefset_s'
	from curr_mrcmattributedomainrefset_s a
	where a.grouped != '0' and a.grouped != '1';
	commit;
