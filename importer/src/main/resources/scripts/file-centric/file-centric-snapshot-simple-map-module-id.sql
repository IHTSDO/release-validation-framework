
/******************************************************************************** 
	file-centric-snapshot-simple-map-module-id.sql

	Assertion:
	The module id of all SimpleMap members should be the core module id

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Simple map refset: id=',a.id, ' has a wrong module id:', a.moduleid) 	
	from curr_simplemaprefset_s a 
	where a.moduleid != '900000000000207008';
	