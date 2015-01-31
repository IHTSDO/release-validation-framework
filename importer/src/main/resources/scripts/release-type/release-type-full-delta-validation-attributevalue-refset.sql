
/*  
	The current full attributevalue refset file consists of the previously published full file and the changes for the current release
*/
drop table if exists v_temp_table;

/* view of current delta, derived from current full */
	create table if not exists v_temp_table like prev_attributevaluerefset_f;
	
	insert into v_temp_table
	select * from curr_attributevaluerefset_d;
	
	insert into v_temp_table
	select * from prev_attributevaluerefset_f;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Attribute value refset: id=',a.id, ': Attribute value refset is in current full file, but not in prior full file.') 	
	from curr_attributevaluerefset_f a
	left join v_temp_table b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.valueid = b.valueid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
 	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.valueid is null;

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Attribute value refset: id=',a.id, ': Attribute value refset is in prior full file, but not in current full file.')
	from v_temp_table a
	left join curr_attributevaluerefset_f b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.valueid = b.valueid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.valueid is null;

commit;
 drop table if exists v_temp_table;
