/******************************************************************************** 
component-centric-snapshot-active-concept-with-inactivation-reason.sql
Assertion:
Active concepts do not have active inactivation indicators
Note:Some inactivation indicators may be applied to active descriptions. 
See https://confluence.ihtsdotools.org/display/DOCTSG/4.2.2+Component+Inactivation+Reference+Sets
900000000000486000 |Limited component (foundation metadata concept)
900000000000492006 |Pending move (foundation metadata concept)|
900000000000495008 |Concept non-current (foundation metadata concept)|

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.id,
		concat('Active concept ', a.referencedcomponentid, ' has inactivation reason ', a.valueid),
		a.id,
        'curr_attributevaluerefset_s'
	from curr_attributevaluerefset_s a join curr_concept_s b 
	on a.referencedcomponentid=b.id 
	where a.refsetid='900000000000489007'
	and a.active=1 
	and b.active=1 
	and a.valueid not in ('900000000000486000','900000000000492006','900000000000495008');