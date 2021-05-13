/******************************************************************************** 
file-centric-snapshot-mdrs-valid-effectiveTime.sql
	Assertion:
        "The effectiveTime should always equal the sourceEffectiveTime."
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
select
  <RUNID>,
  '<ASSERTIONUUID>',
  a.referencedcomponentid,
  concat('Source effective time: ', a.sourceeffectivetime, ' must equal effective time: ', a.effectivetime, ' in module dependency refset.'),
  a.id,
  'curr_moduledependencyrefset_s'
from curr_moduledependencyrefset_s a
where a.active = '1' and (a.sourceeffectivetime <> a.effectivetime);

