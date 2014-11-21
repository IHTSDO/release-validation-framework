
/*  
	The current full description file consists of the previously published full file and the changes for the current release
*/

/*	The current full based on the current delta and the prior full */
	drop temporary table if exists temp_table;
    create temporary table if not exists temp_table(
       id VARCHAR(18),
       effectivetime CHAR(8),
       active CHAR(1),
       moduleid VARCHAR(18),
       conceptid VARCHAR(18),
       languagecode VARCHAR(2),
       typeid VARCHAR(18),
       term VARCHAR(255),
       casesignificanceid VARCHAR(18),
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
    
	insert into temp_table select * from curr_description_d;
	commit;
	insert into temp_table select * from prev_description_f;
	commit;
	
/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Description: id=',a.id, ': Description is in current full file, but not in prior full file.') 	
	from curr_description_f a
	left join temp_table b 
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
	concat('Description: id=',a.id, ': Description is in prior full file, but not in current full file.')
	from temp_table a
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
	drop temporary table temp_table;
