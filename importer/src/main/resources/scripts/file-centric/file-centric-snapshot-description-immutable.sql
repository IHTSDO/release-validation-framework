
/******************************************************************************** 
	file-centric-snapshot-description-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in DESCRIPTION snapshot.
	Note: Checking for current authoring cycle only as there are some voilations in the published releases.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Concept:',a.id, ' has multiple description ids for the same term:', a.term) 	
	from curr_description_s a 
	where exists (select id from curr_description_d b where a.conceptid=b.conceptid and a.term=b.term and b.active=1)
	group by a.typeid, a.languagecode, a.conceptid, a.term
	having count(a.id) > 1;