
/******************************************************************************** 
	component-centric-snapshot-historical-association-active-target

	Assertion:
	Active historical association refset members have active or limited status
	concepts as targets.

********************************************************************************/
	
	/* create table if not exists of limited status concepts */
	drop table if exists v_limcons;
	create table if not exists v_limcons (INDEX(referencedcomponentid)) as
	   select referencedcomponentid from curr_attributevaluerefset_s 
	   where active = '1'
	   and valueid = '900000000000486000';
   insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('AssociationRefset id=',a.id, ' is active but the target component is an inactive concept: ', b.id),
		a.id,
        'curr_associationrefset_s'
	from curr_associationrefset_s a
	inner join curr_concept_s b on a.targetcomponentid = b.id
	left join v_limcons c on c.referencedcomponentid = a.targetcomponentid
	where a.active = '1'
	and a.refsetid not in ('900000000000531004','734138000','734139008')
	and b.active = '0' 
	and c.referencedcomponentid is null;
	
	drop table if exists v_limcons;
		