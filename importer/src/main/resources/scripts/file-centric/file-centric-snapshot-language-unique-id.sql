
/******************************************************************************** 
	file-centric-snapshot-language-unique-id

	Assertion:
	The current Language Refset snapshot file does not contain duplicate 
	Member Ids.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.conceptid,
		concat('MEMBER: id=',c.id, ': is repeated in the language refset snapshot file.') 
	from (select a.id from curr_langrefset_s a group by a.id having count(a.id) > 1) as duplicate,
	 curr_description_s b,
	 curr_langrefset_s c
	where duplicate.id=c.id and c.referencedcomponentid = b.id;