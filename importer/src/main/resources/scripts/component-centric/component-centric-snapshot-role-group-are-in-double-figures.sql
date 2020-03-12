
/********************************************************************************
	component-centric-snapshot-role-group-are-in-double-figures

	Assertion:
	Role Group numbers are in double figures..

********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
select
    <RUNID>,
    '<ASSERTIONUUID>',
    a.sourceid,
    concat('Role Group number of relationship: id=',a.id, ' is higher than 100.')
from curr_relationship_d a
where a.active = '1'
    and relationshipgroup >= 100;

insert into qa_result (runid, assertionuuid, concept_id, details)
select
    <RUNID>,
    '<ASSERTIONUUID>',
    a.sourceid,
    concat('Role Group number of stated relationship: id=',a.id, ' is higher than 100.')
from curr_stated_relationship_d a
where a.active = '1'
    and relationshipgroup >= 100;


