/*  
 * There must be actual changes made to previously published language refset components in order for them to appear in the current delta.
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Language refset id=',a.id, ' is in the detla file, but no acutal changes made since the previous release.')
	from curr_langrefset_d a
	left join prev_langrefset_s b
	on a.id = b.id
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.acceptabilityid = b.acceptabilityid
	where b.id is not null;