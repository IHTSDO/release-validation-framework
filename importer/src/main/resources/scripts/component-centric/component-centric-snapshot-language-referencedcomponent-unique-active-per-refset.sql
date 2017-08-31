/******************************************************************************** 
	component-centric-snapshot-language-referencedcomponent-unique-per-refset-active

	Assertion: There is only one active member per description per dialect in the language refset snapshot file.
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.conceptid,
		concat('Description: id=',temp.referencedcomponentid, ': has multiple active language refset members for a given dialect.') 
	from 
	(select distinct a.refsetid, a.referencedcomponentid from curr_langrefset_d a 
	left join curr_langrefset_s b on a.refsetid =b.refsetid and a.referencedcomponentid=b.referencedcomponentid where a.id != b.id and a.active=1) as temp,
	curr_description_s c 
	where temp.referencedcomponentid =c.id;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.conceptid,
		concat('Text definition: id=',temp.referencedcomponentid, ': has multiple active language refset members for a given dialect.') 
	from 
	(select distinct a.refsetid, a.referencedcomponentid from curr_langrefset_d a 
	left join curr_langrefset_s b on a.refsetid =b.refsetid and a.referencedcomponentid=b.referencedcomponentid where a.id != b.id and a.active=1) as temp,
	curr_textdefinition_s c 
	where temp.referencedcomponentid =c.id;
	