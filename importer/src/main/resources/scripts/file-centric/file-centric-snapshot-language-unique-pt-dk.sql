
/******************************************************************************** 
	file-centric-snapshot-language-unique-pt-dk

	Assertion:
	Every active concept has one active preferred term in the Danish language refset.

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ': has no active preferred term in the Danish language refset')
	from curr_concept_s a
	where
	 a.active=1
	 and a.moduleid = '554471000005108'
	 and not exists ( select b.id from curr_description_s b, curr_langrefset_s c
			where b.id = c.referencedcomponentid
			and b.typeid = '900000000000013009'
			and b.active=1
			and c.active=1
			and c.acceptabilityid = '900000000000548007'
			and c.refsetid = '554461000005103'
			and b.conceptid=a.id);
