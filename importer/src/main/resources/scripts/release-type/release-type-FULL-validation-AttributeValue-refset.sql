
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
	

	drop table if exists curr;
  create table if not exists curr like curr_attributevaluerefset_f;
  insert into curr
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
	from curr a
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
	left join curr b
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
	
	drop table if exists curr;