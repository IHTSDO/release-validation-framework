
/******************************************************************************** 
	component-centric-snapshot-ctv3-ratio

	Assertion:
	There is one and only one CTV3 simple map refset member per concept.

********************************************************************************/
	
	
	/* Concept maps to multiple CTV3 Refset Members */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.referencedcomponentid, ': Concept has more than one associated CTV3 refset member.') 
	from curr_simplemaprefset_s a
	where a.refsetid = '900000000000497000'
	group by a.referencedcomponentid
	having count(a.referencedcomponentid) > 1;
	commit;
	
	
/* Create view of CTV3 refset members */
	create or replace view v_ctv3 as
		select referencedcomponentid 
		from curr_simplemaprefset_s 
		where refsetid = '900000000000497000';
			
/* Concept is without a CTV3 Refset Member mapping */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept does not have an associated CTV3 refset member.') 
	from curr_concept_s a
	left join v_ctv3 b 
		on a.id = b.referencedcomponentid 
	where b.referencedcomponentid is null;

	drop view if exists v_act_ctv3;










	