
/******************************************************************************** 
	component-centric-snapshot-ctv3-unique

	Assertion:
	CTV3 codes are unique in the CTV3 simple map refset

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.maptarget
	from curr_simplemaprefset_s a
	inner join curr_concept_s b 
		on a.referencedcomponentid = b.id
	where 1=1
	and a.refsetid = '900000000000497000'
	group by binary a.maptarget
	having count(*) > 1
	order by a.maptarget;

