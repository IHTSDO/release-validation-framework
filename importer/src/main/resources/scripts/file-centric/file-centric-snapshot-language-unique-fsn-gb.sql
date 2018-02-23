
/******************************************************************************** 
	file-centric-snapshot-language-unique-fsn-gb

	Assertion:
		
	Every active concept has a preferred FSN in the GB language refset.

********************************************************************************/
	
	
	
	
	/* TEST: Concept does not have an FSN in each possible refset */
	/*Only for core module concepts*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' does not have an FSN preferred in the en-GB language refset.') 
	from curr_concept_s a
	where a.active = '1'
	and a.moduleid in ('900000000000207008','900000000000012004')
	and not exists (select b.id
		from curr_description_s b, curr_langrefset_s c
		where b.id=c.referencedcomponentid
		and b.active = '1'
		and c.active = '1'
		and b.typeid = '900000000000003001'
		and c.refsetid='900000000000508004' 
		and c.acceptabilityid='900000000000548007'
		and b.conceptid= a.id);
	