
/******************************************************************************** 
	file-centric-snapshot-description-term-duplicated

	Assertion:
	 Same terms but using different case will be considered as duplicates.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION : ids =', GROUP_CONCAT(DISTINCT a.id ORDER BY a.id ASC SEPARATOR ', '), ' : Duplicated terms in DESCRIPTION snapshot.'),
		a.id,
		'curr_description_s'
	from curr_description_s a left join curr_concept_s b
	on a.conceptid = b.id
	where a.active = '1' and b.active = '1' and a.casesignificanceid = 900000000000448009
	group by LOWER(a.term), a.conceptid, a.typeid, a.languagecode
	having COUNT(*) > 1;
	commit;