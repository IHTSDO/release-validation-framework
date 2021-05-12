/******************************************************************************** 
	component-centric-snapshot-language-referencedcomponent-unique-per-refset

	Assertion: There is only one member id per description per dialect in the language refset snapshot file.
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.conceptid,
		concat('Description: id=',b.id, ': has multiple language refset members for a given dialect.'),
		b.id,
        'curr_description_s'
	from 
	(select distinct a.refsetid, a.referencedcomponentid from curr_langrefset_d a 
	left join curr_langrefset_s b on a.refsetid =b.refsetid and a.referencedcomponentid=b.referencedcomponentid where a.id != b.id) as temp,
	curr_description_s b 
	where temp.referencedcomponentid =b.id;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.conceptid,
		concat('Text definition: id=',b.id, ': has multiple language refset members for a given dialect.'),
		b.id,
        'curr_description_s'
	from 
	(select distinct a.refsetid, a.referencedcomponentid from curr_langrefset_d a 
	left join curr_langrefset_s b on a.refsetid =b.refsetid and a.referencedcomponentid=b.referencedcomponentid where a.id != b.id) as temp,
	curr_textdefinition_s b 
	where temp.referencedcomponentid =b.id;
	