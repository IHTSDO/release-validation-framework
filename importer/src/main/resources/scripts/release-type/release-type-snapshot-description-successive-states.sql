
/******************************************************************************** 
	release-type-snapshot-description-successive-states

	Assertion:	
	New inactive states must follow active states in the DESCRIPTION snapshot.
	Note: Unless there are changes in other fields since last release due to data correction in current release 

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION: id=',a.id, ' should not have a new inactive state as it was inactive previously.') 	
	from curr_description_s a , prev_description_s b	
	where a.effectivetime != b.effectivetime
	and a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.moduleid =b.moduleid
	and a.casesignificanceid = b.casesignificanceid;
	
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION: id=',a.id, ' is inactive but no active state found in the previous snapshot.') 	
	from curr_description_s a left join prev_description_s b
	on a.id=b.id
	where a.active=0 and b.id is null;
	
	
	