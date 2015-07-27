
/******************************************************************************** 
component-centric-expression-association-call-validate-proc.sql

	Assertion:
	Calling procedure testing that all concepts referenced in the CT expression are valid.

********************************************************************************/

	call validateSnomedCTExpressionConcepts_procedure(<RUNID>,'<ASSERTIONUUID>');
	