
/******************************************************************************** 
	file-centric-snapshot-attribute-value-successive-states

	Assertion:	
	New inactive states follow active states in the ATTRIBUTEVALUE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('AttributeRefsetId=',a.id, ' should not have a new inactive state in the snapshot file as was inactive previously.') 	
	from curr_attributevaluerefset_s a , prev_attributevaluerefset_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_attributevaluerefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	commit;
