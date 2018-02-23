
/******************************************************************************** 
	file-centric-snapshot-language-unique-fsn-no

	Assertion:
	Every active concept defined in the Norwegian extension has a preferred FSN in the en-US language refset.
	

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' does not have an FSN preferred in the en-US language refset.') 
	from curr_concept_s a
	where a.active = '1'
	and a.moduleid = '51000202101'
	and not exists (select b.id
		from curr_description_s b, curr_langrefset_s c
		where b.id=c.referencedcomponentid
		and b.active = '1'
		and c.active = '1'
		and b.typeid = '900000000000003001'
		and c.refsetid='900000000000509007' 
		and c.acceptabilityid='900000000000548007'
		and b.conceptid= a.id);
