 /*
 * Active extended map groups with gender rules should have FEMALE rules before MALE rules (alphabetical)
 * NOTE:this check won't necessarily work for ICD10CM maps as they can have "embedded" gender rules.
 * unless you have the target terminology loaded, there's no way to answer this question
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,': Gender mapRules out of order')    
 from curr_extendedmaprefset_s a, curr_extendedmaprefset_s b
	where a.active = 1 and b.active = 1
  	and a.refSetId = b.refSetId
  	and a.referencedComponentId = b.referencedComponentId
  	and a.mapGroup = b.mapGroup
  	and a.mapRule = 'IFA 248153007 | Male (finding) |'
  	and b.mapRule = 'IFA 248152002 | Female (finding) |'
  	and a.mapPriority < b.mapPriority;
