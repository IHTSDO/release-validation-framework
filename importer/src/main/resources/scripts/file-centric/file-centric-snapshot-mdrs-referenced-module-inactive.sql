
/********************************************************************************
	file-centric-snapshot-mdrs-referenced-module-inactive.sql

	Assertion:
	Referenced module in the MDRS is inactive
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Referenced module ', a.referencedcomponentid, ' in module dependency refset is inactive for entry: ', a.id) 
	from curr_moduledependencyrefset_s a
	left join curr_concept_s c on a.referencedcomponentid = c.id
	where c.active = 0
		and a.active = 1;

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.moduleid,
		concat('Source module ', a.moduleid, ' in module dependency refset is inactive for entry: ', a.id) 
	from curr_moduledependencyrefset_s a
	left join curr_concept_s c on a.moduleid = c.id
	where c.active = 0
		and a.active = 1;
