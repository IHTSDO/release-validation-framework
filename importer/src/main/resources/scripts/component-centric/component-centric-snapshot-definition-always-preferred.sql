
/******************************************************************************** 
	component-centric-snapshot-definition-always-preferred

	Assertion:
	All definitions are preferred.

********************************************************************************/
	
/* 	view of current snapshot made by finding Non preferred definition */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select distinct a.term, a.conceptid
	from curr_textdefinition_s a , curr_langrefset_s b 
	where a.id = b.referencedcomponentid
	and a.active =1
	and b.acceptabilityid != '900000000000548007'; 
	


	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: Concept=',a.conceptid, ': concept has definition not "preferred".') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
	