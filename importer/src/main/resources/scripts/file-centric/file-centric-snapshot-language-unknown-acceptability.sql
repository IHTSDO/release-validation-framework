
/******************************************************************************** 
	file-centric-snapshot-language-unknown-acceptability.sql

	Assertion:
	Unknown acceptability for language reference set row

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.acceptabilityid,
		concat('Language refset entry: ' , a.id, ' has unknown acceptability id=', a.acceptabilityid),
		a.id,
		'curr_langrefset_s'
	from curr_langrefset_s a
	where a.active = 1
		and a.acceptabilityid != '900000000000548007'
		and a.acceptabilityid != '900000000000549004';
