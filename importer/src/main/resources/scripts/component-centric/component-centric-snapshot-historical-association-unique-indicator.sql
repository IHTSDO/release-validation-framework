
/******************************************************************************** 
	component-centric-snapshot-historical-association-unique-indicator

	Assertion:
	Duplicate historical association indicators.

********************************************************************************/
insert into qa_result (runid, assertionuuid, concept_id, details)
select distinct
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedComponentId	,
	concat('MEMBER: id=',a.id, ' in historical association refset member has a duplicated indicator with MEMBER id= ', b.id)
from curr_associationrefset_s a inner join (
											select min(id) id, referencedComponentId, targetcomponentId, refsetid
											from curr_associationrefset_s 
											group by referencedComponentId, targetcomponentId, refsetid
											having count(*) >= 2
											) b
										on a.referencedComponentId = b.referencedComponentId 
											and a.targetcomponentId = b.targetcomponentId 
											and a.refsetid = b.refsetid 
											and a.id != b.id;