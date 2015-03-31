
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
		concat('Simple refset: id=',a.id, ' is an active VTM but refers to an inactive component.') 	
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '1'
	and a.refsetid = '447565001'
	and b.active = '0';