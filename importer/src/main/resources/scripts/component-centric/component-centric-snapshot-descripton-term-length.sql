/******************************************************************************** 
	component-centric-snapshot-descripton-term-length.sql
	Assertion:
	A description should not have more than 255 characters long.
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Description: id=',a.id, ' has a term more than 255 characters long.') 
		from curr_description_s a 
		where length(a.term) > 255;
commit;