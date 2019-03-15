/********************************************************************************
	release-type-snapshot-owl-expression-successive-states

	Assertion:
	All OWL Expressions inactivated in current release must have been active in the previous release

********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('OWL Expression Refset: id=',a.id, '  is inactive but no active state found in previous release.')
	from curr_owlexpressionrefset_s a
	left join prev_owlexpressionrefset_s b
	on a.id = b.id
	where
	a.active = 0
	and b.id is null;
	commit;

insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('OWL Expression Refset: id=',a.id, '  is inactive but the OWL Expression was changed from previous release')
	from curr_owlexpressionrefset_s a
	left join prev_owlexpressionrefset_s b
	on a.id = b.id
	where
	a.active = 0
	and a.owlexpression <> b.owlexpression;
	commit;
