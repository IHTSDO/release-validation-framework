
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-valid-attributeingroupcardinality

	Assertion:
	The attributeInGroupCardinality value must be '0..0' for ungrouped MRCM attribute domains.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=', a.id,' AttributeInGroupCardinality value is not 0..0')
	from curr_mrcmAttributeDomainRefset_s a	
	where a.grouped = '0' and a.attributeingroupcardinality != '0..0';
	commit;
