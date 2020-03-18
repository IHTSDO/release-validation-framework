/******************************************************************************** 
	component-centric-snapshot-no-duplicate-association-of-SEP

	Assertion: There is no duplicate association of "S and E" or "S and P".
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Active structure concept: id=',a.referencedcomponentid, ' is being referenced by multiple ', if(a.refsetid = '734138000', 'Entire', 'Part') , ' concepts id=', group_concat(a.targetcomponentid), '.')
	from curr_associationrefset_s a left join curr_concept_s b on a.targetcomponentid = b.id
	where a.refsetid in ('734138000','734139008')
	    and a.active = '1'
	    and b.active = '1'
    group by a.referencedcomponentid, a.refsetid
    having count(a.targetcomponentid) > 1;
	