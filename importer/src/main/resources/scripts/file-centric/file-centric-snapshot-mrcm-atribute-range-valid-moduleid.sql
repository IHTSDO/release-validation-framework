
/******************************************************************************** 
	file-centric-snapshot-mrcm-atribute-range-valid-moduleid

	Assertion:
	ModuleId value refers to valid concept identifier in MRCM ATTRIBUTE RANGE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.moduleid,
		concat('MRCM ATTRIBUTE RANGE: id=',a.id,' : moduleId=',a.moduleid,' MRCM Attribute Range Refset contains a ModuleId that does not exist in the Concept snapshot.') 	
	from curr_mrcmAttributeRangeRefset_s a
	left join curr_concept_s b
	on a.moduleid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
