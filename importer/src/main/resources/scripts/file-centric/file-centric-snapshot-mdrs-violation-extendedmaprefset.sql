/******************************************************************************** 
file-centric-snapshot-mdrs-violation-extendedmaprefset.sql
	Assertion:
        "The moduleId,effectiveTime pair should always respect the MDRS."
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details) 
select
  <RUNID>,
  '<ASSERTIONUUID>',
  a.referencedcomponentid,
  concat('extendedmaprefset: Component effective time: ', a.effectivetime, ' must not be later than effective time: ', m.targeteffectivetime, ' of included module ', a.moduleid, ' as per MDRS')
from curr_extendedmaprefset_s a, curr_moduledependencyrefset_s m
where m.moduleid = '<MODULEID>' and m.effectivetime = '<VERSION>' and m.active = 1 and a.moduleid = m.referencedcomponentid and (a.effectivetime > m.targeteffectivetime);

insert into qa_result (runid, assertionuuid, concept_id, details) 
select
  <RUNID>,
  '<ASSERTIONUUID>',
  a.referencedcomponentid,
  concat('extendedmaprefset: Component effective time: ', a.effectivetime, ' must not be later than effective time: ', m.sourceeffectivetime, ' of Edition module ', a.moduleid, ' as per MDRS')
from curr_extendedmaprefset_s a, curr_moduledependencyrefset_s m
where m.moduleid = '<MODULEID>' and m.effectivetime = '<VERSION>' and m.active = 1 and a.moduleid = m.moduleid and (a.effectivetime > m.sourceeffectivetime);

insert into qa_result (runid, assertionuuid, concept_id, details)
select
  <RUNID>,
  '<ASSERTIONUUID>',
  a.moduleid,
  concat('extendedmaprefset: Component moduleId: ', a.moduleid, ' is not a dependency of the Edition <MODULEID> as per MDRS')
from curr_extendedmaprefset_s a
where a.moduleid != '<MODULEID>' and a.moduleid not in (
  select m.referencedcomponentid from curr_moduledependencyrefset_s m
  where m.moduleid = '<MODULEID>' and m.effectivetime = '<VERSION>' and m.active = 1);
