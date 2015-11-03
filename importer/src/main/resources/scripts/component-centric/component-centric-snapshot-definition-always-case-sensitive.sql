
/******************************************************************************** 
	component-centric-snapshot-definition-always-case-sensitive.sql

	Assertion:
	All definitions are case sensitive.

********************************************************************************/	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('TEXTDEF: id=',a.id, ':Definitions are not case sensitive.') 	
	from curr_textdefinition_s a 
	where a.casesignificanceid != '900000000000017005';

	