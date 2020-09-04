/********************************************************************************
component-centric-snapshot-active-concept-must-have-correct-definition-status-according-to-defined-axioms.sql
Assertion:
Active concept must have correct definition status according to defined axioms
********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Active concept ', a.id, ' is having incorrect definition status according to defined axioms')
	from curr_concept_s a
       left join (select distinct referencedcomponentid from curr_owlexpressionrefset_s where active = '1' and owlexpression like '%EquivalentClasses%') b
       on a.id = b.referencedcomponentid
    where a.active = '1'
    and a.definitionstatusid = '900000000000074008' /* primitive */
    and b.referencedcomponentid is not null;