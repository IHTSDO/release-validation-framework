
/******************************************************************************** 
	component-centric-snapshot-description-casesignificanceid

	Assertion:
	Case-sensitive terms have appropriate caseSignificanceId.

********************************************************************************/

	alter table curr_description_d add FULLTEXT index (term);
	
	call caseSignificance_procedure(<RUNID>,'<ASSERTIONUUID>');

	