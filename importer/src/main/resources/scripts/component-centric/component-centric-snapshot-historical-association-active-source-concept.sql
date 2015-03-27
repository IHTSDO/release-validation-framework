
/******************************************************************************** 
	component-centric-snapshot-historical-association-active-source-concept

	Assertion:
	Active historical association refset members have inactive concepts as reference components.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',a.id, ':Historical refset member is active, but refers to an active concept.') 	
	from curr_associationrefset_s a
	inner join curr_concept_s b on a.referencedcomponentid = b.id
	where a.active = '1'
	and b.active != '0';
		