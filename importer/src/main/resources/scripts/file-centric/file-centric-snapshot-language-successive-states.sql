
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
	from curr_langrefset_d a
	join prev_langrefset_s b
	on a.id = b.id
	where a.active = b.active
	and a.active = 0;


