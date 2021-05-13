
/******************************************************************************** 
	file-centric-snapshot-attribute-value-valid-valueid

	Assertion:
	ValueId refers to valid concepts in the ATTRIBUTE VALUE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ATT RF: id=',a.valueid, ':Invalid valueId.'),
		a.id,
		'curr_attributevaluerefset_s'
	from curr_attributevaluerefset_s a
	left join curr_concept_s b
	on a.valueid = b.id
	where b.id is null;
	commit;