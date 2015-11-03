
/******************************************************************************** 
	component-centric-snapshot-vmp-active

	Assertion:
	Active VMP refset members refer to active components.


********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ': Active VMP refset members refer to inactive components.') 	
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '1'
	and a.refsetid = '447566000'
	and b.active = '0';