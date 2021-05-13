
/******************************************************************************** 
	file-centric-snapshot-language-valid-descriptionid

	Assertion:
	All members refer to a description that is referenced in either the 
	description table or the text definition table.

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Language Refset: ReferencedComponentId =', a.referencedcomponentid, ' is neither in the description nor the definition snapshot file.'),
		a.id,
		'curr_langrefset_s'
	from curr_langrefset_s a
	where 
	a.active=1
	and not exists
		(select b.id
		 from curr_description_s b
		 where b.id = a.referencedcomponentid)
	and not exists
		(select c.id
		 from curr_textdefinition_s c
		 where c.id = a.referencedcomponentid);	

