
/******************************************************************************** 
component-centric-snapshot-referenced-concepts-in-mrcm-active.sql

	Assertion:
	"All referenced concepts in MRCM refsets must be active.
	

********************************************************************************/

    /* 	list of active concepts */
    drop table if exists tmp_active_concepts;
    create table if not exists tmp_active_concepts as
    select a.id as conceptid
    from curr_concept_s a
    where a.active = '1';
    commit;

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Reference component id:',a.referencedcomponentid, ' must be active.')
	from curr_mrcmAttributeDomainRefset_s a left join tmp_active_concepts b on a.referencedcomponentid = b.conceptid
	where a.active = '1'
        and b.conceptid is null;

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' must be active.')
    from curr_mrcmAttributeRangeRefset_s a left join tmp_active_concepts b on a.referencedcomponentid = b.conceptid
    where a.active = '1'
        and b.conceptid is null;

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' must be active.')
    from curr_mrcmDomainRefset_s a left join tmp_active_concepts b on a.referencedcomponentid = b.conceptid
    where a.active = '1'
        and b.conceptid is null;

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Reference component id:',a.referencedcomponentid, ' must be active.')
    from curr_mrcmModuleScopeRefset_s a left join tmp_active_concepts b on a.referencedcomponentid = b.conceptid
    where a.active = '1'
        and b.conceptid is null;

     /* drop temp tables */
    drop table tmp_active_concepts;