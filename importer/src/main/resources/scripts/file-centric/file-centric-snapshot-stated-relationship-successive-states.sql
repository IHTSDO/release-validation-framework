
/******************************************************************************** 
	file-centric-snapshot-stated-relationship-successive-states

	Assertion:
	All relationships inactivated in current release must have been active in the previous release

	rtu 20130512: inactivation sould be the only edit. 

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.id
	from curr_stated_relationship_d a
	inner join prev_stated_relationship_s b on a.id = b.id 
	where a.active = '0' 
	and b.active = '0' 
	and a.effectivetime = '<CURRENT-RELEASE-DATE>'
	and a.moduleid = b.moduleid
	and a.sourceid = b.sourceid
	and a.relationshipgroup = b.relationshipgroup
	and a.destinationid = b.destinationid
	and a.typeid = b.typeid
	and a.characteristictypeid = b.characteristictypeid
	and a.modifierid = b.modifierid;
	
