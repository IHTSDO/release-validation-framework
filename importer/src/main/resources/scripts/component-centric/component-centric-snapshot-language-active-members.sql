
/******************************************************************************** 
	component-centric-snapshot-language-active-members

	Assertion:
	Members are active for active descriptions in the snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.id,
		concat('Language refset: id=',a.id, ' is inactive for an active description') 
	from curr_langrefset_s a
	inner join curr_description_s b on a.referencedComponentId = b.id 
	inner join curr_concept_s c on b.conceptid = c.id
	where a.active = '0' 
	and b.active = '1'
	and c.active='1';
	