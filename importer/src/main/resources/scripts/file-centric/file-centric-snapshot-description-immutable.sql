
/******************************************************************************** 
	file-centric-snapshot-description-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in the description snapshot.
	Note: Checking for current authoring cycle only as there are some voilations in the published releases.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Concept:',a.id, ' has multiple description ids for the same term:', a.term) 	
	from curr_description_d a 
	group by a.conceptid, binary (a.term)
	having count(a.id) > 1;
	commit;
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Description:', a.id , ' has changes in the immutable fields either type id, language code or concept id since previous release.') 	
	from curr_description_s a
	join prev_description_s b
 	on a.id=b.id
	where
	a.typeid != b.typeid
	or a.languagecode != b.languagecode
	or a.conceptid != b.conceptid;
