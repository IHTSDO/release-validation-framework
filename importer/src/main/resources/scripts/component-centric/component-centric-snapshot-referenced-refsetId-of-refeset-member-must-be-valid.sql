
/******************************************************************************** 
component-centric-snapshot-referenced-refsetId-of-refeset-member-must-be-valid.sql

	Assertion:
	"The refsetId of a refset member must be an existing and active concept."

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.refsetid,
		concat('Refset id:',a.refsetid, ' in association refset member must be an existing and active concept.'),
		null,
        'curr_associationrefset_s'
	from (select distinct refsetid from curr_associationrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
	where (b.active = '0' or b.id is null);

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in attribute value refset member must be an existing and active concept.'),
        null,
        'curr_attributevaluerefset_s'
    from (select distinct refsetid from curr_attributevaluerefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
    	where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in complex map refset member must be an existing and active concept.'),
        null,
        'curr_complexmaprefset_s'
    from (select distinct refsetid from curr_complexmaprefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in expression association refset member must be an existing and active concept.'),
        null,
        'curr_expressionassociationrefset_s'
    from (select distinct refsetid from curr_expressionassociationrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in extended map refset member must be an existing and active concept.'),
        null,
        'curr_extendedmaprefset_s'
    from (select distinct refsetid from curr_extendedmaprefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in language refset member must be an existing and active concept.'),
        null,
        'curr_langrefset_s'
    from (select distinct refsetid from curr_langrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in map correlation origin refset member must be an existing and active concept.'),
        null,
        'curr_mapcorrelationoriginrefset_s'
    from (select distinct refsetid from curr_mapcorrelationoriginrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in module dependency refset member must be an existing and active concept.'),
        null,
        'curr_moduledependencyrefset_s'
    from (select distinct refsetid from curr_moduledependencyrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM attribute domain refset member must be an existing and active concept.'),
        null,
        'curr_mrcmattributedomainrefset_s'
    from (select distinct refsetid from curr_mrcmattributedomainrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM attribute range refset member must be an existing and active concept.'),
        null,
        'curr_mrcmattributerangerefset_s'
    from (select distinct refsetid from curr_mrcmattributerangerefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM domain refset member must be an existing and active concept.'),
        null,
        'curr_mrcmdomainrefset_s'
    from (select distinct refsetid from curr_mrcmdomainrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM moudle scope refset member must be an existing and active concept.'),
        null,
        'curr_mrcmmodulescoperefset_s'
    from (select distinct refsetid from curr_mrcmmodulescoperefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in owl expression refset member must be an existing and active concept.'),
        null,
        'curr_owlexpressionrefset_s'
    from (select distinct refsetid from curr_owlexpressionrefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in simple map refset member must be an existing and active concept.'),
        null,
        'curr_simplemaprefset_s'
    from (select distinct refsetid from curr_simplemaprefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in simple refset member must be an existing and active concept.'),
        null,
        'curr_simplerefset_s'
    from (select distinct refsetid from curr_simplerefset_s where active = '1') as a left join curr_concept_s b on a.refsetid = b.id
        where (b.active = '0' or b.id is null);










