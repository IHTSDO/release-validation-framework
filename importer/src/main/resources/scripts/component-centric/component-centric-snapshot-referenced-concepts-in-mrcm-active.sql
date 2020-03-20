
/******************************************************************************** 
component-centric-snapshot-referenced-concepts-in-mrcm-active.sql

	Assertion:
	"All referenced concepts in MRCM refsets must be active.
	

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Reference component id:',a.referencedcomponentid, ' in MRCM Attribute Domain refset must be active.')
	from curr_mrcmattributedomainrefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
	where a.active = '1'
        and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' in MRCM Attribute Range refset must be active.')
    from curr_mrcmattributerangerefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1'
        and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' in MRCM  Domain refset must be active.')
    from curr_mrcmdomainrefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1'
        and (b.active = '0' or b.id is null);

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' in MRCM module scope refset must be active.')
    from curr_mrcmmodulescoperefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1'
        and (b.active = '0' or b.id is null);
