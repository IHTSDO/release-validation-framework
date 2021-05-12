
/******************************************************************************** 
	release-type-snapshot-unique-description-definition-id.sql

	Assertion:
	The ids should be unique across the description and text definition snapshot files.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Description: id=',a.id, ' is duplicated in the description and text definition snapshot.'),
		a.id,
		'curr_description_s'
	from curr_description_s a, curr_textdefinition_s b
	where a.id=b.id;