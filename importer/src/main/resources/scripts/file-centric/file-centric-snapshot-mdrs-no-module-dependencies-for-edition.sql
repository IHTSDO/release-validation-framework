
/******************************************************************************** 
	file-centric-snapshot-mdrs-no-module-dependencies-for-edition

	Assertion:
	No module dependencies for the edition/version

********************************************************************************/
	
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select distinct
		<RUNID>,
		'<ASSERTIONUUID>',
		'<MODULEID>',
		'No module dependencies for edition module <MODULEID> with version <VERSION>',
		a.id,
		'curr_moduledependencyrefset_s'
	from curr_moduledependencyrefset_s a
	where not exists 
		(select b.moduleid from curr_moduledependencyrefset_s b
			where b.moduleid = '<MODULEID>'
				and b.effectivetime = '<VERSION>'
				and b.active = 1);
