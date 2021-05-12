
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
		a.id,
        'curr_associationrefset_s'
	from curr_associationrefset_s a left join curr_concept_s b on a.refsetid = b.id
	where a.active = '1' and (b.active = '0' or b.id is null);

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in attribute value refset member must be an existing and active concept.'),
        a.id,
        'curr_attributevaluerefset_s'
    from curr_attributevaluerefset_s a left join curr_concept_s b on a.refsetid = b.id
    	where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in complex map refset member must be an existing and active concept.'),
        a.id,
        'curr_complexmaprefset_s'
    from curr_complexmaprefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in expression association refset member must be an existing and active concept.'),
        a.id,
        'curr_expressionassociationrefset_s'
    from curr_complexmaprefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in extended map refset member must be an existing and active concept.'),
        a.id,
        'curr_extendedmaprefset_s'
    from curr_extendedmaprefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in language refset member must be an existing and active concept.'),
        a.id,
        'curr_langrefset_s'
    from curr_langrefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in map correlation origin refset member must be an existing and active concept.'),
        a.id,
        'curr_mapcorrelationoriginrefset_s'
    from curr_mapcorrelationoriginrefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in module dependency refset member must be an existing and active concept.'),
        a.id,
        'curr_moduledependencyrefset_s'
    from curr_moduledependencyrefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM attribute domain refset member must be an existing and active concept.'),
        a.id,
        'curr_mrcmattributedomainrefset_s'
    from curr_mrcmattributedomainrefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM attribute range refset member must be an existing and active concept.'),
        a.id,
        'curr_mrcmattributerangerefset_s'
    from curr_mrcmattributerangerefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM domain refset member must be an existing and active concept.'),
        a.id,
        'curr_mrcmdomainrefset_s'
    from curr_mrcmdomainrefset_s as a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in MRCM moudle scope refset member must be an existing and active concept.'),
        a.id,
        'curr_mrcmmodulescoperefset_s'
    from curr_mrcmmodulescoperefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in owl expression refset member must be an existing and active concept.'),
        a.id,
        'curr_owlexpressionrefset_s'
    from curr_owlexpressionrefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in simple map refset member must be an existing and active concept.'),
        a.id,
        'curr_simplemaprefset_s'
    from curr_simplemaprefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.refsetid,
        concat('Refset id:',a.refsetid, ' in simple refset member must be an existing and active concept.'),
        a.id,
        'curr_simplerefset_s'
    from curr_simplerefset_s a left join curr_concept_s b on a.refsetid = b.id
        where a.active = '1' and (b.active = '0' or b.id is null);










