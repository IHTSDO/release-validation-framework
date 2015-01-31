
/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
  release-type-FULL-validation-AttributeValue-refset
  
	Assertion:
  The current AttributeValue refset full file contains all previously published 
  data unchanged.

	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/
	

	drop table if exists v_curr_view;
  create table if not exists v_curr_view like curr_attributevaluerefset_f;
  insert into v_curr_view
		select *
		from curr_attributevaluerefset_f
		where cast(effectivetime as datetime) <
		(select max(cast(effectivetime as datetime))
		from curr_attributevaluerefset_f);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
    concat('ATTRIBUTE-VALUE: id=',a.id, ': refset member is in current release file, but not in prior release file.') 	        
	from v_curr_view a
	left join prev_attributevaluerefset_f b
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
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
    concat('ATTRIBUTE-VALUE: id=',a.id, ': refset member is in prior release file, but not in current release file.') 	        
	from prev_attributevaluerefset_f a
	left join v_curr_view b
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

  truncate table v_curr_view;
	drop table if exists v_curr_view;