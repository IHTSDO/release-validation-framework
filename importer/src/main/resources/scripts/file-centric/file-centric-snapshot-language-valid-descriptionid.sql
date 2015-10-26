
/******************************************************************************** 
	file-centric-snapshot-language-valid-descriptionid

	Assertion:
	All members refer to a description that is referenced in either the 
	description table or the text definition table.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Language Refset: id=',a.id, ' references a description which is in neither the description nor the definition snapshot file.') 
	from curr_langrefset_s a
	where not exists
		(select b.id
		 from curr_description_s b
		 where b.id = a.referencedcomponentid)
	and not exists
		(select c.id
		 from curr_textdefinition_s c
		 where c.id = a.referencedcomponentid);	

