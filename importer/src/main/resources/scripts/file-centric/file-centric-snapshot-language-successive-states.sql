
/******************************************************************************** 
	file-centric-snapshot-language-successive-states

	Assertion:
	Members inactivated in current release were active in the previous release.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Language Refset: id=',a.id, ' should not have a new inactive state in the current release as it was inactive already in the previvous SNAPSHOT.') 
	from curr_langrefset_s a
	join prev_langrefset_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_langrefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	commit;
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Language Refset: id=',a.id, ' should not have a new inactive state in the current snapshot as no active stated found in previous release') 
	from curr_langrefset_s a
	left join prev_langrefset_s b
	on a.id = b.id
	where
	a.active = 0
	and b.id is null;
	commit;

