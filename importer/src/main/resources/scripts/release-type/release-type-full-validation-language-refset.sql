/******************************************************************************** 
	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-FULL-validation-Language-refset
  
	Assertion:
	The current Language refset full file contains all previously published 
	data unchanged.
	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.
	This test identifies rows in prior, not in current, and in current, not in 
	prior.
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
    concat('LANGUAGE-REFSET: id=',a.id, ' is in previous full file, but not in current full file.'),
    a.id,
    'prev_langrefset_f'
	from prev_langrefset_f a
	left join curr_langrefset_f b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
    	and a.refsetid = b.refsetid
   	 	and a.referencedcomponentid = b.referencedcomponentid
    	and a.acceptabilityid = b.acceptabilityid
    where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.acceptabilityid is null;	