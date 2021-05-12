
/******************************************************************************** 
	component-centric-snapshot-stated-one-isa

	Assertion:
	All concepts have at least one stated is-a relationship.

********************************************************************************/
	/* create table if not exists of all concepts containing an active stated is_a relationship */
	drop table if exists v_act_stated_isa;
	create table if not exists v_act_stated_isa (index(sourceid)) as
	select sourceid
		from curr_stated_relationship_s
		where active = '1'
		and typeid = 116680003;
			
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ': Concept does not have a stated is-a relationship.'),
		a.id,
        'curr_concept_s'
	from curr_concept_s a
	left join v_act_stated_isa b on a.id = b.sourceid
	where a.active = '1'
	and b.sourceid is null
	and a.id != 138875005;

	drop table if exists v_act_stated_isa;
