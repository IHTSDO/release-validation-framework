
/******************************************************************************** 
	release-type-snapshot-description-successive-states

	Assertion:	
	New inactive states follow active states in the DEFINITION snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('TextDefinition: id=',a.id, '  should not have a new inactive state as it was inactive previously.'),
		a.id,
		'curr_textdefinition_s'
	from curr_textdefinition_s a , prev_textdefinition_s b
	where a.effectivetime != b.effectivetime
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('TextDefinition: id=',a.id, ' is inactive but no active state found in the previous snapshot.'),
		a.id,
		'curr_textdefinition_s'
	from curr_textdefinition_s a  left join prev_textdefinition_s b
	on a.id = b.id
	where a.active = 0
	and b.id is null;
	