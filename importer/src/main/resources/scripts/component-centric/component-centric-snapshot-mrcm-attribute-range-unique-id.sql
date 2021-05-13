
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-range-unique-id

	Assertion:
	The current MRCM Attribute Range snapshot file has unique identifiers.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id,':Non unique id in current MRCM Attribute Range snapshot file.'),
		a.id,
        'curr_mrcmattributerangerefset_s'
	from curr_mrcmattributerangerefset_s a
	group by a.id
	having  count(a.id) > 1;
	commit;