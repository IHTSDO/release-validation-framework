
/******************************************************************************** 
	file-centric-snapshot-attribute-value-valid-referencedcomponentid

	Assertion:
	Referencedcomponentid refers to valid concepts in the ATTRIBUTEVALUE snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ATT RS: id=',a.referencedcomponentid, ':Invalid Referencedcomponentid in ATTRIBUTEVALUE REFSET snapshot.') 	
	from curr_attributevaluerefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where b.id is null and not exists (select id from curr_description_s where id = a.referencedcomponentid);
	commit;