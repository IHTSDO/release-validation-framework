
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
		concat('MEMBER: id=',a.id, ': is repeated in the language refset snapshot file.') 
	from curr_langrefset_s a, curr_description_s b
	where a.referencedcomponentid = b.id
	group by a.id
	having count(a.id) > 1;