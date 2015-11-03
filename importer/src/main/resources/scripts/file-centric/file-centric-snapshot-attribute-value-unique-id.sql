
/******************************************************************************** 
	file-centric-snapshot-attribute-value-unique-id

	Assertion:
	ID is unique in the ATTRIBUTE VALUE snapshot.

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Attribute value refset: id=',a.id, ':Non unique id in snapshot file.') 	
	from curr_attributevaluerefset_s a	
	group by a.id
	having  count(a.id) > 1;
	commit;