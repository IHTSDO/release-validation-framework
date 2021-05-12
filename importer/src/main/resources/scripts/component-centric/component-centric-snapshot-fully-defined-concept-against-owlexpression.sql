
/******************************************************************************** 
	component-centric-snapshot-fully-defined-concept-against-owlexpression

	Assertion:
	Active fully-defined concept must have at least one Axiom record with the words 'EquivalentClasses'

********************************************************************************/

    insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Fully defined concept: id=',a.id, ' has no Axiom record with the words EquivalentClasses.'),
		a.id,
        'curr_concept_s'
	from curr_concept_s a
       left join (select distinct referencedcomponentid from curr_owlexpressionrefset_s where active = '1' and owlexpression like '%EquivalentClasses%') b
	   on a.id = b.referencedcomponentid
	where a.active = '1'
	and a.definitionstatusid = '900000000000073002'
	and b.referencedcomponentid is null;
