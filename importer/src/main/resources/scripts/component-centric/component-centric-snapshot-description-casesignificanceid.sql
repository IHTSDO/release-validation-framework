
/******************************************************************************** 
	component-centric-snapshot-description-casesignificanceid

	Assertion:
	Case-sensitive terms have appropriate caseSignificanceId.

********************************************************************************/

	alter table curr_description_d add FULLTEXT index idx_desc_ft_idx(term);
	
	call caseSignificance_procedure(<RUNID>,'<ASSERTIONUUID>');

	