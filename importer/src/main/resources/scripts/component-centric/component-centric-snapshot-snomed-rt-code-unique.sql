
/******************************************************************************** 
	component-centric-snapshot-snomed-rt-code-unique

	Assertion:
	SNOMED RT codes are unique in the SNOMED RT simple map refset

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		a.maptarget
	from curr_simplemaprefset_s a
	inner join curr_concept_s b 
		on a.referencedcomponentid = b.id
	where a.maptarget is not null
	and a.refsetid = '900000000000498005'
	group by binary a.maptarget
	having count(*) > 1
	order by a.maptarget;

