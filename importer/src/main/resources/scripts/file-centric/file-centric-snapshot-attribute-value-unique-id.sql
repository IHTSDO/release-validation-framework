
/******************************************************************************** 
	file-centric-snapshot-attribute-value-unique-id

	Assertion:
	ID is unique in the ATTRIBUTE VALUE snapshot.

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Attribute value refset: id=',a.id, ':Non unique id in snapshot file.'),
		a.id,
		'curr_attributevaluerefset_s'
	from curr_attributevaluerefset_s a	
	group by a.id
	having  count(a.id) > 1;
	commit;