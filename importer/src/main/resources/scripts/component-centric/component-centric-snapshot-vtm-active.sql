
/******************************************************************************** 
	component-centric-snapshot-vtm-active

	Assertion:
	Active VTM simple refset members refer to active components.


********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',a.id, ': Active VTM simple refset members refer to inactive components.') 	
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '1'
	and a.refsetid = '447565001'
	and b.active = '0';