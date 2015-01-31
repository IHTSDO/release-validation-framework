
/******************************************************************************** 
	component-centric-snapshot-nonhuman-inactive

	Assertion:
	Inactive Non-human refset members refer to inactive components.


********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',a.id, ': Inactive Non-human simple refset members refer to active components.') 	
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '0'
	and a.refsetid = '447564002'
	and b.active = '1';