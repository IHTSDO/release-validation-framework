
/*  
	The current delta file is an accurate derivative of the current full file
*/

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.conceptid,
	concat('DESCRIPTION: id=',a.id, ' is in DELTA file, but not in FULL file.') 	
	from curr_description_d a
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
	
