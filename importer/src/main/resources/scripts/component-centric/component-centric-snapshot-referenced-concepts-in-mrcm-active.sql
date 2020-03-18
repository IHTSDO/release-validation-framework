
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
	from curr_mrcmAttributeDomainRefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
	where a.active = '1'
        and b.active = '0';

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' in MRCM Attribute Range refset must be active.')
    from curr_mrcmAttributeRangeRefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1'
        and b.active = '0';

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' in MRCM  Domain refset must be active.')
    from curr_mrcmDomainRefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1'
        and b.active = '0';

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' in MRCM module scope refset must be active.')
    from curr_mrcmModuleScopeRefset_s a left join curr_concept_s b on a.referencedcomponentid = b.id
    where a.active = '1'
        and b.active = '0';
