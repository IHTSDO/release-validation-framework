
/******************************************************************************** 
	release-type-snapshot-mrcm-domain-refset-successive-states

	Assertion:	
	New inactive states follow active states in the MRCM DOMAIN REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Mrcm Domain Refset: id=',a.id, '  should not have a new inactive state as it was inactive previously.'),
		a.id,
		'curr_mrcmdomainrefset_s'
	from curr_mrcmdomainrefset_s a , prev_mrcmdomainrefset_s b
	where a.effectivetime != b.effectivetime
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Mrcm Domain Refset: id=',a.id, ' is inactive but no active state found in the previous snapshot.'),
		a.id,
		'curr_mrcmdomainrefset_s'
	from curr_mrcmdomainrefset_s a  left join prev_mrcmdomainrefset_s b
	on a.id = b.id
	where a.active = 0
	and b.id is null;
	