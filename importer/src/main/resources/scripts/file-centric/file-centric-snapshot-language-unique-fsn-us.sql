
/******************************************************************************** 
	file-centric-snapshot-language-unique-fsn-us

	Assertion:
	Every active FSN in the US module is referenced in the US lang refset

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' does not have an FSN preferred in the US language refset.') 
	from curr_concept_s a
	where a.active = '1'
	and a.moduleid in ('731000124108') /*US module*/
	and not exists (select b.id
		from curr_description_s b, curr_langrefset_s c
		where b.id=c.referencedcomponentid
		and b.active = '1'
		and c.active = '1'
		and b.typeid = '900000000000003001' /*FSN*/
		and c.refsetid='900000000000509007' /*US lang refset*/
		and c.acceptabilityid='900000000000548007'
		and b.conceptid= a.id);

