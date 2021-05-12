
/******************************************************************************** 
	component-centric-snapshot-ctv3-unique

	Assertion:
	CTV3 codes are unique in the CTV3 simple map refset

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('CTV3 Code = ', a.maptarget, 'is not unique'),
		null,
        'curr_simplemaprefset_s'
	from curr_simplemaprefset_s a
	inner join curr_concept_s b 
		on a.referencedcomponentid = b.id
	where a.maptarget is not null
	and a.refsetid = '900000000000497000'
	group by binary a.maptarget
	having count(*) > 1
	order by a.maptarget;

