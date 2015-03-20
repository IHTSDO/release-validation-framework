
/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-FULL-validation-Simple-refset
  
	Assertion:
	The current Simple refset full file contains all previously published data 
	unchanged.

	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/
	

	create table if not exists tmp_priorfullfromcurrent as
	select *
	from curr_simplerefset_f
	where cast(effectivetime as datetime) <=
		(select max(cast(effectivetime as datetime)) 
		 from prev_simplerefset_f);

/*  rows that are among the prior rows of the current simplerefset full file, that are not in the published prior refset*/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
    concat('Refset: id=',a.id, ': simple refset member is in current release file, but not in prior release file.') 	  
	from tmp_priorfullfromcurrent a
	left join prev_simplerefset_f b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    where b.id is null
    or b.effectivetime is null
    or b.active is null
    or b.moduleid is null
    or b.refsetid is null
    or b.referencedcomponentid is null;

/* rows that were published in the prior simple refset file, that are not among teh prior rows of the current file */    
    insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
      	concat('Refset: id=',a.id, ': simple refset member is in prior release file, but not in current release file.') 	 
	from prev_simplerefset_f a
	left join tmp_priorfullfromcurrent b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    where b.id is null
    or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null;
	
	drop table if exists tmp_priorfullfromcurrent;