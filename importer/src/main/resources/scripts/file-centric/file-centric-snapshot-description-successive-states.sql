
/******************************************************************************** 
	file-centric-snapshot-description-successive-states

	Assertion:	
	New inactive states must follow active states in the DESCRIPTION snapshot.
	Note: Unless there are changes in other fields since last release due to data correction in current release 

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION: id=',a.id, ' should not have a new inactive state in the current release as it was already inactive in previous snapshot.') 	
	from curr_description_s a , prev_description_s b	
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_description_s)
	and a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.moduleid =b.moduleid
	and a.typeid = b.typeid
	and a.casesignificanceid = b.casesignificanceid;
	
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION: id=',a.id, ' should not have a new inactive state in the current release as no active state found in previous snapshot.') 	
	from curr_description_s a left join prev_description_s b
	on a.id=b.id
	where a.active=0 and b.id is null;
	
	
	