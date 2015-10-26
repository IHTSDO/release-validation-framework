
/******************************************************************************** 
	component-centric-snapshot-icdo-active

	Assertion:
	Active ICD-O simple map refset refers to active components.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ': Active ICD-O refset member refers to an inactive concept.') 
	
	from curr_simplemaprefset_s a
	inner join curr_concept_s b on a.referencedcomponentid = b.id
	where a.refsetid = '446608001'
	and a.active = '1'
	and b.active = '0';
