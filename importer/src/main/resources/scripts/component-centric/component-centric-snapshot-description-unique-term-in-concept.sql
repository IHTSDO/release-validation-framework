
/******************************************************************************** 
	component-centric-snapshot-description-unique-term-in-concept

	Assertion:
	For a given concept, all active description terms are unique.

	Implementation is limited to active descriptions of active concepts edited 
	in the current prospective release.
********************************************************************************/
/*	list of active descriptions of active concepts edited in the current 
	prospective release 
*/	
	drop table if exists tmp_active_desc;
	create table if not exists tmp_active_desc as
	select c.conceptid, c.term
	from res_concepts_edited a
		join curr_concept_s b
			on a.conceptid = b.id
			and b.active = 1
		join curr_description_s c
			on c.conceptid = a.conceptid
			and c.active = 1;
	commit;

/*  violators have the same term twice within a concept */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: Id=', conceptid, ': non-unique term within concept.') 
	from tmp_active_desc
	group by conceptid,  binary term
	having count(conceptid) > 1
	and binary count(term) > 1;
	commit;
	
	drop table if exists tmp_active_desc;
	
			
