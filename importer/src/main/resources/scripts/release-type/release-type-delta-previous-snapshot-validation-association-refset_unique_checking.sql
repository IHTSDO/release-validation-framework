/*  
 * Previously published historical association components should not be modified.
*/
insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Association refset id=',a.id, ' has historical association components changed since the previous release.')
	from curr_associationrefset_d a,
	prev_associationrefset_s b
	where a.id = b.id
	and (a.refsetid != b.refsetid
    or a.referencedcomponentid != b.referencedcomponentid
    or a.targetcomponentid != b.targetcomponentid);