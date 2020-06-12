
/******************************************************************************** 
component-centric-snapshot-referencedComponentID-of-refeset-member-must-be-valid.sql

	Assertion:
	"All active Refset records must be valid."

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid,' in association refset member must be valid.')
	from curr_associationrefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
	where a.active = '1'
	    and a.refsetid not in ('900000000000523009','900000000000524003','900000000000525002','900000000000526001','900000000000527005','900000000000528000','900000000000530003','900000000000531004')
	    and (b.active = '0' or b.id is null) ;

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in complex map refset member must be valid.')
    from curr_complexmaprefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in expression association refset member must be valid.')
    from curr_expressionAssociationRefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in extended map refset member must be valid.')
    from curr_extendedmaprefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);


    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in map correlation origin refset member must be valid.')
    from curr_mapCorrelationOriginRefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in module dependency refset member must be valid.')
    from curr_moduledependencyrefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in owl expression refset member must be valid.')
    from curr_owlexpressionrefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in simple map refset member must be valid.')
    from curr_simplemaprefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid,' for refset id: ', a.refsetid, ' in simple refset member must be valid.')
    from curr_simplerefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1' and (b.active = '0' or b.id is null);