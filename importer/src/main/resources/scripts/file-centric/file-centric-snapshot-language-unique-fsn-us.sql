
/******************************************************************************** 
	file-centric-snapshot-language-unique-fsn-us

	Assertion:
	Every active concept defined in the US extension has a preferred FSN in the US language refset.
	Every active FSN in the US module is referenced in the us-en lang reference set.


********************************************************************************/
	/*
	Every active concept defined in the US extension has a preferred FSN in the US language refset.
	*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' in the US extension does not have an FSN preferred in the US language refset.')
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

    /*
    Every active FSN in the US module is referenced in the us-en lang reference set.
    */
    insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('FSN: id=',a.id, ' in the US extension is not referenced in the US language refset.')
    from curr_description_s a
    where a.active = '1' and a.moduleid = '731000124108' and a.typeid = '900000000000003001' /*FSN*/
    and not exists (select b.id
    from curr_langrefset_s b
    where b.referencedcomponentid = a.id
    and b.active = '1'
    and b.refsetid = '900000000000509007' /*US lang refset*/
    and b.acceptabilityid='900000000000548007');
