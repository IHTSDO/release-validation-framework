
/******************************************************************************** 
	component-centric-snapshot-stated-one-isa

	Assertion:
	All concepts have at least one stated is-a relationship.

********************************************************************************/
	/* Create view of all concepts containing an active stated is_a relationship */
	create or replace view v_act_stated_isa as
	select sourceid
		from curr_stated_relationship_s
		where active = '1'
		and typeid = 116680003;
		


	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept does not have a stated is-a relationship.') 	
	
	from curr_concept_s a
	left join v_act_stated_isa b on a.id = b.sourceid
	where a.active = '1'
	and b.sourceid is null
	and a.id != 138875005;

	drop view v_act_stated_isa;
