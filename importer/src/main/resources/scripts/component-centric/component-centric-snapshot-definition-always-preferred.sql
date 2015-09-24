
/******************************************************************************** 
	component-centric-snapshot-definition-always-preferred

	Assertion:
	All definitions are preferred.

********************************************************************************/
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: Concept=', a.conceptid, ': concept has definition not "preferred".') 	
	from curr_textdefinition_s a , curr_langrefset_s b 
	where a.id = b.referencedcomponentid
	and a.active =1
	and b.acceptabilityid != '900000000000548007';	