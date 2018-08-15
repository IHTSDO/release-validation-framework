/*
 * Active extended map with mapAdvice containing "MAP IS CONTEXT DEPENDENT FOR GENDER" use gender mapRule values
 *
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id, ' : Gender advice but without gender rule')   
 from curr_extendedmaprefset_s a
	where a.active = 1
  	and mapAdvice like '%MAP IS CONTEXT DEPENDENT FOR GENDER%'
 	and a.mapRule not like '%IFA 248153007 | Male (finding) |%'
  	and a.mapRule not like '%IFA 248152002 | Female (finding) |%';
 commit;
 
/*
 * Also the inverse:
 */
 
insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,' : Gender rule without gender advice')   
 from curr_extendedmaprefset_s a
where a.active = 1
  and mapAdvice not like '%MAP IS CONTEXT DEPENDENT FOR GENDER%'
  and (a.mapRule like '%IFA 248153007 | Male (finding) |%'
       or a.mapRule like '%IFA 248152002 | Female (finding) |%');
 commit;

 