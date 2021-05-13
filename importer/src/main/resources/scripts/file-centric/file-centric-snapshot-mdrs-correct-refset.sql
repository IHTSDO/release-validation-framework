/******************************************************************************** 
file-centric-snapshot-mdrs-correct-refset.sql
    Assertion:
        "All module dependency rows should be in the module dependency refset"
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Dependency from: ', a.moduleid, ' - ', a.sourceeffectivetime, ' to: ', a.referencedcomponentid, ' - ', a.targeteffectivetime, ' is declared in the incorrect refset ', a.refsetId),
		a.id,
		'curr_moduledependencyrefset_s'
	from curr_moduledependencyrefset_s a
		where a.active = '1' and a.refsetId != '900000000000534007';
