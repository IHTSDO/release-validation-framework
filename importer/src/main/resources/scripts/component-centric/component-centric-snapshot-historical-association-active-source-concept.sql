
/******************************************************************************** 
	component-centric-snapshot-historical-association-active-source-concept

	Assertion:
	Active historical association refset members have inactive concepts as reference components.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ':Historical refset member is active, but refers to an active concept.'),
		a.id,
		'curr_associationrefset_s'
	from curr_associationrefset_s a
	inner join curr_concept_s b on a.referencedcomponentid = b.id
	where a.active = '1'
	and b.active != '0'
	and a.refsetid in 
	(900000000000523009,900000000000524003,900000000000525002,900000000000526001,900000000000527005,900000000000528000,900000000000530003,900000000000531004);