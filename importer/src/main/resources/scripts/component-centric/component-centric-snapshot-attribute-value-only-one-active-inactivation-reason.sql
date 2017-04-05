
/******************************************************************************** 
component-centric-snapshot-attribute-value-only-one-active-inactivation-reason.sql

	Assertion:
	There is only one active inactivation reason for a given referenced component in the attribute value snapshot.
	

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Reference component id:',a.referencedcomponentid, ' has multiple inactivation reasons') 	
	from curr_attributevaluerefset_s a
	where a.active=1 
	group by a.referencedcomponentid,a.refsetid
	having  count(distinct a.valueid) > 1;
	