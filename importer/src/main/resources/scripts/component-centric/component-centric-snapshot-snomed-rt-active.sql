
/******************************************************************************** 
	component-centric-snapshot-snomed-rt-active

	Assertion:
	SNOMED RT identifier simple map refset members are always active.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',a.id, ': SNOMED RT simple map refset member is not active.') 

	from curr_simplemaprefset_s a
	where a.refsetid = '900000000000498005'
	and a.active != '1'