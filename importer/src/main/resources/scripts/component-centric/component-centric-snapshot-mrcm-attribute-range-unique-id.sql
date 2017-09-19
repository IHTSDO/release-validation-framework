
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-range-unique-id

	Assertion:
	The current MRCM Attribute Range snapshot file has unique identifiers.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id,':Non unique id in current MRCM Attribute Range snapshot file.') 	
	from curr_mrcmAttributeRangeRefset_s a
	group by a.id
	having  count(a.id) > 1;
	commit;