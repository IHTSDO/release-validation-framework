
/******************************************************************************** 
	component-centric-snapshot-report.sql

	Assertion:
	Report Creation...

********************************************************************************/
	
		insert into qa_report (runid, assertionuuid, assertiontext, result ,count)
		select a.runid , b.assertionuuid , b.assertiontext , 'F' , count(b.assertionuuid)  
		from qa_run a , qa_result b 
		where a.runid = b.runid 
		and a.runid = <RUNID>
		group by b.assertionuuid
		order by 2 desc;
