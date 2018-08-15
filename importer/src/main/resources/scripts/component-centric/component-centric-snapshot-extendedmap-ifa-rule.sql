 /*
 * Active extended map with mapRule starting with IFA always reference a concept that is a valid active concept id
 *  NOTE the substr is parsing 'IFA 12345 | ... |' into '12345'
 *
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,' : IFA rule references an inactive concept' )  
 from curr_extendedmaprefset_s a
	where a.active = 1
 	 and a.mapRule like 'IFA%'
  	and not exists
    (select * from curr_concept_s b
     	where b.active = 1
       and b.id = substr(substring_index(a.mapRule,' ',2),5) );
 commit;
 
/*
 * Active extended maps with mapRule starting with IFA have concept id values and expressions matching active concept id and preferred name in the default language configuration
* NOTE the substr is parsing 'IFA 12345 | abc def ghi |' into 'abc def ghi'
*/ 
insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 <RUNID>,
 '<ASSERTIONUUID>',
 a.referencedcomponentid,
 concat('ExtendedMap: id=',a.id,' : IFA map rule references an inactive term')       
 from curr_extendedmaprefset_s a
where a.active = 1
  and a.mapRule like 'IFA%'
  and not exists
   (select * from curr_concept_s b, curr_description_s c
     where b.active = 1
       and c.active = 1
       and b.id = substr(substring_index(a.mapRule,' ',2),5)
       and b.id =c.conceptid
       and c.term = rtrim(ltrim(substring_index(substr(a.mapRule,instr(a.mapRule,'|')+1),'|',1))));
 commit;
 