
/*  
	The current full description file consists of the previously published full file and the changes for the current release
*/

/*	The current full based on the current delta and the prior full */
	drop table if exists v_temp_table;
    create table if not exists temp_table(
       id bigint(20),
       effectivetime CHAR(8),
       active CHAR(1),
       moduleid bigint(20),
       conceptid bigint(20),
       languagecode VARCHAR(2),
       typeid bigint(20),
       term VARCHAR(255),
       casesignificanceid bigint(20),
		index idx_id (id),
	    index idx_effectivetime (effectivetime),
	    index idx_active (active),
	    index idx_moduleid (moduleid),
	    index idx_conceptid (conceptid),
	    index idx_languagecode (languagecode),
	    index idx_typeid (typeid),
	    index idx_term (term),
	    index idx_casesignificanceid (casesignificanceid)
    );
    
	insert into v_temp_table select * from curr_description_d;
	commit;
	insert into v_temp_table select * from prev_description_f;
	commit;
	
/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Description: id=',a.id, ' is in current full file, but not in prior full file.') 	
	from curr_description_f a
	left join v_temp_table b
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

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Description: id=',a.id, ' is in prior full file, but not in current full file.')
	from v_temp_table a
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

	commit;
	 drop table if exists v_temp_table;
