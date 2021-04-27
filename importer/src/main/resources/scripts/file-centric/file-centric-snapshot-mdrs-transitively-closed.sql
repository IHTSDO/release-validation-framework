/******************************************************************************** 
file-centric-snapshot-mdrs-transitively-closed.sql
	Assertion:
        "The dependencies must be transitively closed."
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details) 
select
  <RUNID>,
  '<ASSERTIONUUID>',
  a.referencedcomponentid,
  concat('Missing dependency from: ', a.moduleid, ' - ', a.sourceeffectivetime, ' to: ', b.referencedcomponentid, ' - ', b.targeteffectivetime, ' in module dependency refset.')
from curr_moduledependencyrefset_s a, curr_moduledependencyrefset_s b
where a.active = '1' and b.active = '1'
  and a.referencedcomponentid = b.moduleid and a.targeteffectivetime = b.sourceeffectivetime
  and not (a.moduleid = b.referencedcomponentid and a.sourceeffectivetime = b.targeteffectivetime)
  and not exists (
    select c.referencedcomponentid from curr_moduledependencyrefset_s c
    where c.active = '1' and a.moduleid = c.moduleid and a.sourceeffectivetime = c.sourceeffectivetime
      and b.referencedcomponentid = c.referencedcomponentid and b.targeteffectivetime = c.targeteffectivetime);

