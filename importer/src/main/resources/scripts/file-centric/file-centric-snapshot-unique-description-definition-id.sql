
/******************************************************************************** 
	file-centric-snapshot-unique-description-definition-id.sql

	Assertion:
	Descripiton and text definiiton ids should be unique.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Description: id=',a.id, ' is duplicated in the description and text definition snapshot.') 	
	from curr_description_s a, curr_textdefinition_s b
	where a.id=b.id;