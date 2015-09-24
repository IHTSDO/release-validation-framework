
/******************************************************************************** 
	file-centric-snapshot-association-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOC RF: id=',a.id, ':Invalid keys in ASSOCIATION REFSET snapshot file.') 	
	from curr_associationrefset_s a 
	group by a.id , a.refsetid , a.referencedcomponentid , a.targetcomponentid
	having count(a.id) > 1 and count(a.refsetid) > 1 and count(a.referencedcomponentid ) > 1 and count(a.targetcomponentid) > 1;
	commit;
	