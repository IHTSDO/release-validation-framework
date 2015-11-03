
/******************************************************************************** 
	component-centric-snapshot-snomed-rt-unique

	Assertion:
	SNOMED RT identifier simple map refset members are unique.

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ': SNOMED RT member key (referencedcomponentid, maptarget) is not unique.')
	from curr_simplemaprefset_s a
	where a.refsetid = '900000000000498005'
	group by a.referencedcomponentid, a.maptarget
	having count(*) > 1;
