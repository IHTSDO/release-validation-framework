/******************************************************************************** 
	component-centric-snapshot-definition-preferred-in-en_us-and-en_gb.sql
	Assertion:
	There is an equivalent definition in the en-us and en-gb language when provided.
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		usTestDef.conceptid,
		concat('ConceptId=', usTestDef.conceptid, ' has definition in EN-US but no equivalent found for EN-GB') 
		from (select distinct a.conceptid from curr_textdefinition_d a left join curr_langrefset_s b on b.referencedcomponentid=a.id where b.refsetid=900000000000509007 and b.active=1 and a.active=1) usTestDef
		left join (select distinct a.conceptid from curr_textdefinition_d a left join curr_langrefset_s b on b.referencedcomponentid=a.id where b.refsetid=900000000000508004 and b.active=1 and a.active=1) gbTextDef  
		on usTestDef.conceptid = gbTextDef.conceptid 
		where gbTextDef.conceptid is null;
commit;


insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		gbTextDef.conceptid,
		concat('ConceptId=', gbTextDef.conceptid, ' has definition in EN-GB but no equivalent found for EN-US') 
		from 
		(select distinct a.conceptid from curr_textdefinition_d a left join curr_langrefset_s b on b.referencedcomponentid=a.id where b.refsetid=900000000000508004 and b.active=1 and a.active=1) gbTextDef 
		left join 
		(select distinct a.conceptid from curr_textdefinition_d a left join curr_langrefset_s b on b.referencedcomponentid=a.id where b.refsetid=900000000000509007 and b.active=1 and a.active=1) usTestDef
		on usTestDef.conceptid = gbTextDef.conceptid 
		where usTestDef.conceptid is null;
commit;