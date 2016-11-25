/******************************************************************************** 
component-centric-snapshot-active-description-with-inactivation-reason.sql
Assertion:
Active descriptions do not have active inactivation indicators
Note:Some inactivation indicators may be applied to active descriptions. 
See https://confluence.ihtsdotools.org/display/DOCTSG/4.2.2+Component+Inactivation+Reference+Sets
900000000000486000 |Limited component (foundation metadata concept)
900000000000492006 |Pending move (foundation metadata concept)|
900000000000495008 |Concept non-current (foundation metadata concept)|

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.conceptid,
		concat('Active description ', a.referencedcomponentid, ' has inactivation reason ', a.valueid) 	
	from curr_attributevaluerefset_s a join curr_description_s b 
	on a.referencedcomponentid=b.id 
	where a.refsetid='900000000000490003'
	and a.active=1 
	and b.active=1 
	and a.valueid not in ('900000000000486000','900000000000492006','900000000000495008');