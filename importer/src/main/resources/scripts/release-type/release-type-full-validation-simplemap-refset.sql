
/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-FULL-validation-SimpleMap-refset
  
	Assertion:
	The current SimpleMap refset full file contains all previously published 
	data unchanged.

	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/
	

	create or replace view curr as
		select *
		from curr_simplemaprefset_f
		where cast(effectivetime as datetime) <
			(select max(cast(effectivetime as datetime)) 
			 from curr_simplemaprefset_f);
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
    concat('SIMPLE-MAP: id=',a.id, ': refset member is in current release file, but not in prior release file.') 	        
	from curr a
	left join prev_simplemaprefset_f b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.maptarget = b.maptarget
    where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.maptarget is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
    concat('SIMPLE-MAP: id=',a.id, ': refset member is in prior release file, but not in current release file.') 	        
	from prev_simplemaprefset_f a
	left join curr b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
	and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.maptarget = b.maptarget
    where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.maptarget is null;


	drop view curr;
