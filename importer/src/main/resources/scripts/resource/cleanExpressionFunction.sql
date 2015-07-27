DROP FUNCTION IF EXISTS cleanExpression;
CREATE FUNCTION cleanExpression(str VARCHAR(500))
RETURNS VARCHAR(300) DETERMINISTIC
	RETURN
    	replace(replace(replace(replace((str),'(',''),')',''),'=',','),':',',') ;