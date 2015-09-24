
/******************************************************************************** 
	file-centric-snapshot-association-successive-states

	Assertion:	
	New inactive states follow active states in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOC RS: id=',a.id, ':Invalid inactive states in the ASSOCIATION REFSET snapshot file.') 	
	from curr_associationrefset_s a , prev_associationrefset_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_associationrefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	commit;