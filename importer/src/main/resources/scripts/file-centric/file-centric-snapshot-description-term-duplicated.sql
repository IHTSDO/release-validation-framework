
/******************************************************************************** 
	file-centric-snapshot-description-term-duplicated

	Assertion:
	 Same terms but using different case will be considered as duplicates.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION : ids =', GROUP_CONCAT(DISTINCT a.id ORDER BY a.id ASC SEPARATOR ', '), ' : Duplicated terms in DESCRIPTION snapshot.')
	from curr_description_s a
	where a.active = '1' and casesignificanceid = 900000000000448009
	group by LOWER(a.term), a.conceptid, a.typeid
	having COUNT(*) > 1;
	commit;