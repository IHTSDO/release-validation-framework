
/******************************************************************************** 
	file-centric-snapshot-language-successive-states

	Assertion:
	Members inactivated in current release were active in the previous release.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',a.id, ': Member inactived in current release was inactive in previous release.') 
	from curr_langrefset_s a
	join prev_langrefset_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_langrefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;


