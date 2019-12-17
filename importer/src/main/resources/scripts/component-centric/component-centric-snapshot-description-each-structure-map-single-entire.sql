
/******************************************************************************** 
	component-centric-snapshot-description-each-structure-map-single-entire

	Assertion:
	Each referenced "Structure" should only map single "Entire".

********************************************************************************/
	drop table if exists v_act_structure_concepts;
	create table if not exists v_act_structure_concepts (INDEX(conceptId)) as
	select conceptId
		from curr_description_s
		where active = '1'
		and typeId = '900000000000003001'
		and term like 'Structure of%(body structure)';

	drop table if exists v_act_entire_concepts;
    create table if not exists v_act_entire_concepts (INDEX(conceptId)) as
    select conceptId
        from curr_description_s
        where active = '1'
        and typeId = '900000000000003001'
        and term like 'Entire%(body structure)';

    drop table if exists v_act_structure_entire_concepts_map;
    create table if not exists v_act_structure_entire_concepts_map as
    select b.conceptId as conceptId1, a.conceptId as conceptId2
    from v_act_entire_concepts a , v_act_structure_concepts b , curr_relationship_s r
            where r.active = '1'
            and r.sourceId = a.conceptId
            and r.destinationId = b.conceptId;

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        b.conceptId2,
        concat('A duplicated \'Entire\' concept with id ', b.conceptId2, ' is referencing to \'Structure\' concept ', a.conceptId)
    from (select conceptId1 as conceptId
          from v_act_structure_entire_concepts_map
          group by conceptId1
           having count(conceptId1) > 1) as a, v_act_structure_entire_concepts_map b
    where a.conceptId = b.conceptId1;
    commit;

    drop table if exists v_act_structure_entire_concepts_map;
	drop table if exists v_act_entire_concepts;
	drop table if exists v_act_structure_concepts;


