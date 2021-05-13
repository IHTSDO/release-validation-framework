/******************************************************************************** 
	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE 
	Assertion:
	The current Description full file contains all previously published data 
	unchanged.
	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.
	This test identifies rows in prior, not in current, and in current, not in 
	prior.
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION: id=',a.id, ' is in previous full file but not in current full file.'),
		a.id,
		'prev_description_f'
	from prev_description_f a
	left join curr_description_f b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.conceptid = b.conceptid
		and a.languagecode = b.languagecode
		and a.typeid = b.typeid
		and a.term = b.term
		and a.casesignificanceid = b.casesignificanceid
	where b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.conceptid is null
		or b.languagecode is null
		or b.typeid is null
		or b.term is null
		or b.casesignificanceid is null;