/*  
 * There must be actual changes made to previously published simple map refset components in order for them to appear in the current delta.
*/

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedcomponentid,
	concat('SimpleMapRefset: id=',a.id, ' is in the detla file, but no actual changes made since the previous release.'),
	a.id,
	'curr_simplemaprefset_d'
	from curr_simplemaprefset_d a
	left join prev_simplemaprefset_s b
		on a.id = b.id
		and a.active = b.active
    	and a.moduleid = b.moduleid
    	and a.refsetid = b.refsetid
   	 	and a.referencedcomponentid = b.referencedcomponentid
    	and a.maptarget = b.maptarget
	where b.id is not null;