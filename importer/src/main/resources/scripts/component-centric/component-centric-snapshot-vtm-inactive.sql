
/******************************************************************************** 
	component-centric-snapshot-vtm-inactive

	Assertion:
	Inactive VTM simple refset members refer to inactive components.


********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ': Inactive VTM simple refset members refer to active components.') 	
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '0'
	and a.refsetid = '447565001'
	and b.active = '1';