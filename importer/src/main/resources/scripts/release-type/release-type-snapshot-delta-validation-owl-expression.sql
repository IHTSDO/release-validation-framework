/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-SNAPSHOT-delta-validation-OWL-Expression

  
	Assertion:
	The current data in the OWL Expression snapshot file are the same as the
	data in the current delta file. 

	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('OWL Expression: id=',a.id, ' is in delta but not in snapshot file.')
	from curr_owlexpressionrefset_d a
	left join curr_owlexpressionrefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.owlexpression = b.owlexpression
	where b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or a.refsetid is null
		or a.referencedcomponentid is null
		or a.owlexpression is null;