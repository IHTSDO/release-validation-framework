
/******************************************************************************** 
	component-centric-full-mrcm-domain-valid-guideurl

	Assertion:
	GuideURL is a valid URL in MRCM DOMAIN full

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM DOMAIN REFSET: id=',a.id,' GuideURL is not a valid URL in MRCM DOMAIN REFSET full file') 	
	from curr_mrcmDomainRefset_f a	
	where a.guideurl NOT REGEXP "^(https?://|www\\.)[\.A-Za-z0-9\-]+\\.[a-zA-Z]{2,4}";
	commit;
