
/*  
	The current attribute-value refset delta file is an accurate derivative of the current full file
*/

/* view of current snapshot, derived from current full */
	drop table if exists temp_view;
  create table if not exists temp_view like curr_attributevaluerefset_f;
  insert into temp_view
	select a.*
	from curr_attributevaluerefset_f a
	where a.effectivetime = '<CURRENT-RELEASE-DATE>';

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ATTRIBUTE VALUE REFSET: id=',a.id, ': Member is in DELTA file, but not in FULL file.')
	from curr_attributevaluerefset_d a
	left join temp_view b 
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
		concat('ATTRIBUTE VALUE REFSET: id=',a.id, ': Member is in FULL file, but not in DELTA file.') 
	from temp_view a
	left join curr_attributevaluerefset_d b 
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
	drop table if exists temp_view;
