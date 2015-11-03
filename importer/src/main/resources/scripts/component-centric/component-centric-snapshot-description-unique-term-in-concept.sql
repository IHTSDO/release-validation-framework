/******************************************************************************** 
	component-centric-snapshot-description-unique-term-in-concept

	Assertion:
	For a given concept, all active description terms are unique.

	Implementation is limited to active descriptions of active concepts edited 
	in the current prospective release.
********************************************************************************/
/*  violators have the same term twice within a concept */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.conceptid,
		concat('DESC: Id=', c.id, ': non-unique term within concept.') 
	from res_concepts_edited a
		join curr_concept_s b
			on a.conceptid = b.id
			and b.active = 1
		join curr_description_s c
			on c.conceptid = a.conceptid
			and c.active = 1
	group by c.conceptid,  binary c.term
	having count(c.conceptid) > 1
	and binary count(c.term) > 1;
			
