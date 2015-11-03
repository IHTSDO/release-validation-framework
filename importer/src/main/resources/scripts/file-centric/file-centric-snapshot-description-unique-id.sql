
/******************************************************************************** 
	file-centric-snapshot-description-unique-id

	Assertion:
	The current Description snapshot file has unique ids.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: id=',a.id, ':Non unique id in description release file.') 	
	from curr_description_s a	
	group by a.id
	having  count(a.id) > 1;