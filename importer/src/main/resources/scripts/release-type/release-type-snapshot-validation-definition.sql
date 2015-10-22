
/*  
	The current text definition snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_textdefinition_view;
  	create table if not exists temp_textdefinition_view like curr_textdefinition_f;
  	insert into temp_textdefinition_view
	select a.*
	from curr_textdefinition_f a
	where cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_textdefinition_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Definition: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.') 	
	from curr_textdefinition_s a
	left join temp_textdefinition_view b
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

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Definition: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.') 
	from temp_textdefinition_view a
	left join curr_textdefinition_s b 
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

commit;
drop table if exists temp_textdefinition_view;
