/******************************************************************************** 
component-centric-snapshot-active-description-with-concept-non-current.sql
Assertion:
Active descriptions of an inactive concept should have concept non-current indicators
********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.conceptid,
		concat('Active description ', b.id, ' is missing concept non-current indicator'),
		b.id,
		'curr_description_s'
	from curr_concept_s a join curr_description_s b 
	on a.id = b.conceptid
	where a.active=0 
	and b.active=1 
	and not exists ( select id from curr_attributevaluerefset_s c where b.id = c.referencedcomponentid and c.valueid = 900000000000495008 and active =1);
	