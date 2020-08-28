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
	from curr_concept_s a left join curr_owlexpressionrefset_s b
	on a.id = b.referencedcomponentid
	where a.active=1
	and b.active=1
	and b.refsetid='733073007'
	and ((a.definitionstatusid='900000000000074008' and (b.owlexpression not like '%SubClassOf%' or b.owlexpression like '%EquivalentClasses%'))   /* primitive */
	     or (a.definitionstatusid='900000000000073002' and b.owlexpression not like '%EquivalentClasses%') /* defined */
	     );
	