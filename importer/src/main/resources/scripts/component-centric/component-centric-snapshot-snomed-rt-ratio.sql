
/******************************************************************************** 
	component-centric-snapshot-snomed-rt-ratio

	Assertion:
	There is one and only one SNOMED RT simple map refset member per concept.

********************************************************************************/
	
	
	/* Concept maps to multiple SNOMED RT Refset Members */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('CONCEPT: id=',a.referencedcomponentid, ': Concept has more than one associated SNOMED RT refset member.'),
		a.id,
        'curr_simplemaprefset_s'
	from curr_simplemaprefset_s a
	where a.refsetid = '900000000000498005'
	group by (a.referencedcomponentid)
	having count(a.referencedcomponentid) > 1;


	/* create table if not exists of all active SNOMED RT refset members */
	drop table if exists v_act_srt;
	create table if not exists v_act_srt (INDEX(referencedcomponentid)) as
		select referencedcomponentid 
		from curr_simplemaprefset_s 
		where refsetid = '900000000000498005';
			
	/* Concept is without a SNOMED RT Refset Member mapping */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ': Concept does not have an associated SNOMED RT refset member.'),
		a.id,
        'curr_concept_s'
	from curr_concept_s a
	left join v_act_srt b 
		on a.id = b.referencedcomponentid 
	where b.referencedcomponentid is null
	and a.definitionstatusid != '900000000000074008';

	drop table if exists v_act_srt;

