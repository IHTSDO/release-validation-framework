
/******************************************************************************** 
	component-centric-snapshot-nonhuman-active

	Assertion:
	Active Non-human refset members refer to active components.


********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ': Active Non-human simple refset members refer to inactive components.') 	
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '1'
	and a.refsetid = '447564002'
	and b.active = '0';