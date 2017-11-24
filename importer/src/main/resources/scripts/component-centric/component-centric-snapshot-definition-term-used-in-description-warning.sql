/******************************************************************************** 
	component-centric-snapshot-definition-term-used-in-description-warning.sql
	Assertion:
	A description normally has less than 35 words.
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		result.conceptid,
		concat('Description: id=',result.id, ' has a term more than 35 words.') 
		from 
		 (SELECT id, conceptid, (LENGTH(term) - LENGTH(REPLACE(term, ' ', ''))+1) as total from curr_description_d  
		 where typeid='900000000000013009'
		 and active =1 
		 having total > 35) as result;
commit;