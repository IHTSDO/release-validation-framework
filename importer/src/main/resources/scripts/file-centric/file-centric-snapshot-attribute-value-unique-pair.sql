
/******************************************************************************** 
	file-centric-snapshot-attribute-value-unique-pair

	Assertion:
	Reference componentId and valueId pair is unique in the ATTRIBUTE VALUE snapshot.

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Reference component id:',a.referencedcomponentid, ' valueid=', a.valueid, ' pair is not unique in the Attribute Value snapshot') 	
	from curr_attributevaluerefset_s a	
	group by a.referencedcomponentid,a.valueid
	having  count(a.id) > 1;
	commit;
	