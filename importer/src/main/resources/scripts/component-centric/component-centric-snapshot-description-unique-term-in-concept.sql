/******************************************************************************** 
	component-centric-snapshot-description-unique-term-in-concept

	Assertion:
	For a given concept, all active description terms are unique.

	Implementation is limited to active descriptions of active concepts edited 
	in the current prospective release.
	Note: See another assertion component-centric-snapshot-description-unique-terms
********************************************************************************/
/*  violators have the same term twice within a concept */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Duplicate term=', a.term) 
	from curr_description_s a
	where a.active =1
	and exists (select id from res_edited_active_concepts where id = a.conceptid)
	group by a.conceptid, a.languagecode, a.term
	having count(distinct a.id) > 1;