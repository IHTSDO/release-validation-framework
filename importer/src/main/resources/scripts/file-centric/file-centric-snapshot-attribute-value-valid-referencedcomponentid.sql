
/******************************************************************************** 
	file-centric-snapshot-attribute-value-valid-referencedcomponentid

	Assertion:
	Referencedcomponentid refers to valid concepts in the ATTRIBUTEVALUE snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ATT RS: id=',a.referencedcomponentid, ':Invalid Referencedcomponentid in ATTRIBUTEVALUE REFSET snapshot.'),
		a.id,
		'curr_attributevaluerefset_s'
	from curr_attributevaluerefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where a.refsetid='900000000000489007'
	and b.id is null;
		
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ATT RS: id=',a.referencedcomponentid, ':Invalid Referencedcomponentid in ATTRIBUTEVALUE REFSET snapshot.'),
		a.id,
		'curr_attributevaluerefset_s'
	from curr_attributevaluerefset_s a
	where a.refsetid='900000000000490003'
 	and not exists (select b.id from curr_description_s b where b.id = a.referencedcomponentid)
 	and not exists ( select c.id from curr_textdefinition_s c where c.id = a.referencedcomponentid);
	commit;