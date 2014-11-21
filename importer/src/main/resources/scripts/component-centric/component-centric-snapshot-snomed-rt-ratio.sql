
/******************************************************************************** 
	component-centric-snapshot-snomed-rt-ratio

	Assertion:
	There is one and only one SNOMED RT simple map refset member per concept.

********************************************************************************/
	
	
	/* Concept maps to multiple SNOMED RT Refset Members */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.referencedcomponentid, ': Concept has more than one associated SNOMED RT refset member.') 
	from curr_simplemaprefset_s a
	where a.refsetid = '900000000000498005'
	group by (a.referencedcomponentid)
	having count(a.referencedcomponentid) > 1;


	/* Create view of all active SNOMED RT refset members */
	create or replace view v_act_srt as
		select referencedcomponentid 
		from curr_simplemaprefset_s 
		where refsetid = '900000000000498005';
			
	/* Concept is without a SNOMED RT Refset Member mapping */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept does not have an associated SNOMED RT refset member.') 
	from curr_concept_s a
	left join v_act_srt b 
		on a.id = b.referencedcomponentid 
	where b.referencedcomponentid is null;

	drop view if exists v_act_srt;

