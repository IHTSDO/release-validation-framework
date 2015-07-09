package org.ihtsdo.rvf.execution.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RvfDbScheduledEventGenerator implements InitializingBean{
	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;
	@Resource(name = "dataSource")
	private BasicDataSource dataSource;
	private int maxKeepTimeInHour;
	
	public RvfDbScheduledEventGenerator(int maxKeepTimeInHour) {
		this.maxKeepTimeInHour = maxKeepTimeInHour;
		
	}
	
	public void createDropReleaseSchemaEvent(String prospectiveSchema) throws SQLException {
		try (Connection connection = rvfDynamicDataSource.getConnection(prospectiveSchema)) {
			String dropSchemaSQL = "drop database "  + prospectiveSchema;
			String createDropEvent = "create event if not exists " + "drop_" + prospectiveSchema + " on schedule at now() + INTERVAL " + maxKeepTimeInHour + " HOUR" + " DO " + dropSchemaSQL + ";";
			PreparedStatement statement = connection.prepareStatement(createDropEvent);
			statement.execute();
		}
	}

	public void createQaResultDeleteEvent(Long runId) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			String deleteQaResultSQL = " delete from qa_result where run_id = " + runId;
			String createDeleteEvent = "create event if not exists " + "delete_qaResultForRunId_" + runId  + " on schedule at now() + INTERVAL " + maxKeepTimeInHour + " HOUR" + " DO " + deleteQaResultSQL + ";";
			PreparedStatement statement = connection.prepareStatement(createDeleteEvent);
			statement.execute();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();) {
			String turnOnSchedulerSql = "SET GLOBAL event_scheduler = ON";
			statement.execute(turnOnSchedulerSql);
			String createQaResultTruncateEvent = "create event if not exists truncateQaResultTableWeeklyOnSunday " + "on schedule every 1 week starts CONCAT(DATE(NOW() + INTERVAL 6 - WEEKDAY(CURRENT_DATE) DAY ), ' 05:00:00') " +
			" on completion preserve " + " DO truncate qa_result;";
			statement.execute(createQaResultTruncateEvent);
		}
	}
}