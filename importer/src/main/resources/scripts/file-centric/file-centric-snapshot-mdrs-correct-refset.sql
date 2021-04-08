/******************************************************************************** 
file-centric-snapshot-mdrs-correct-refset.sql
    Assertion:
        "All module dependency rows should be in the module dependency refset"
********************************************************************************/

    insert into qa_result (runid, assertionuuid, concept_id, details)
    select
        <RUNID>,
        '<ASSERTIONUUID>',
        a.referencedcomponentid,
        concat('Dependency from: ', a.moduleid, ' - ', a.sourceeffectivetime, ' to: ', b.referencedcomponentid, ' - ', b.targeteffectivetime, ' is declared in the incorrect refset ', a.refsetId)
    from curr_moduledependencyrefset_s a
        where a.refsetId != '900000000000534007' 
