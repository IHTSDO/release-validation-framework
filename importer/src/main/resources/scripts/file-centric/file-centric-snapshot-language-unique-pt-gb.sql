
/******************************************************************************** 
	file-centric-snapshot-language-unique-pt-gb

	Assertion:
	Every active concept has one active preferred term in the en-GB language refset.

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ': has no active preferred term in the en-GB language refset') 
	from curr_concept_s a
	where
	 a.active=1
	 and not exists ( select b.id from curr_description_s b, curr_langrefset_s c
			where b.id = c.referencedcomponentid
			and b.typeid = '900000000000013009'
			and b.active=1
			and c.active=1
			and c.acceptabilityid = '900000000000548007'
			and c.refsetid = '900000000000508004'
			and b.conceptid=a.id);
	