
/******************************************************************************** 
component-centric-snapshot-expression-association-unique-mapTarget.sql

	Assertion:
	The mapTarget in expression association refset snapshot is a valid LOINC code

********************************************************************************/
 
 insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('MapTarget:',a.mapTarget,' is not a valid LOINC code in the expression association refset snapshot.')
 from curr_expressionAssociationRefset_s a
 	where a.contentOriginId=705117003
 	and a.referencedcomponentid = 705114005
	and a.mapTarget not like "%-%";
 commit;
