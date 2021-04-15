
/******************************************************************************** 
	file-centric-snapshot-description-invalid-descriptiontype

	Assertion:
	Unknown description type on description row

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('DESCRIPTION: id=', a.id , ' has unknown description type: ', a.typeid) 	
		from description_s a
		where a.active = 1
			and a.typeid not in 
			(select b.referencedcomponentid from descriptiontyperefset_s b);