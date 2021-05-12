
/******************************************************************************** 
	component-centric-snapshot-definition-always-preferred

	Assertion:
	All definitions are preferred.

********************************************************************************/
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('CONCEPT: Concept=', a.conceptid, ': concept has definition not "preferred".'),
		a.id,
        'curr_textdefinition_s'
	from curr_textdefinition_s a , curr_langrefset_s b
	where a.id = b.referencedcomponentid
	and a.active =1
	and b.acceptabilityid != '900000000000548007';	