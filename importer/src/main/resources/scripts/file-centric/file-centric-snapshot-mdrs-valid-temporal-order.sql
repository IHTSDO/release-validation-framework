/******************************************************************************** 
file-centric-snapshot-mdrs-valid-temporal-order.sql
	Assertion:
        "The sourceEffectiveTime should always be equal or greater than the targetEffectiveTime."
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details) 
select
  <RUNID>,
  '<ASSERTIONUUID>',
  a.referencedcomponentid,
  concat('Source effective time: ', a.sourceeffectivetime, ' must be equal or greater than target effective time: ', a.targeteffectivetime, ' in module dependency refset.')
from curr_moduledependencyrefset_s a
where a.active = '1' and (a.sourceeffectivetime < a.targeteffectivetime);

