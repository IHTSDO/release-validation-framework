/******************************************************************************** 
file-centric-snapshot-mdrs-no-version-skew.sql
    Assertion:
        "Target modules in the MDRS must have the same version"
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details) 
select
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedcomponentid,
	concat('Version skew in dependency on module: ', b.referencedcomponentid, ' - ', a.targeteffectivetime, ' and ', b.targeteffectivetime)
from curr_moduledependencyrefset_s a, curr_moduledependencyrefset_s b
where a.active = '1' and b.active = '1'
	and a.moduleid = b.moduleid
	and a.sourceeffectivetime = b.sourceeffectivetime
	and a.referencedcomponentid = b.referencedcomponentid
	and a.targeteffectivetime < b.targeteffectivetime;
