package org.ihtsdo.rvf.execution.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ihtsdo.rvf.execution.service.util.MySqlDataTypeConverter;
import org.ihtsdo.rvf.execution.service.util.RF2FileTableMapper;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load release files into DB tables.
 *
 */
public class ReleaseFileDataLoader {

	private MySqlDataTypeConverter dataTypeConverter;
	private Connection connection;
	private final Logger LOGGER = LoggerFactory.getLogger(ReleaseFileDataLoader.class);
	private RvfDynamicDataSource dataSource;
	private String schemaName;

	public ReleaseFileDataLoader(final Connection dbConnection, final MySqlDataTypeConverter typeConverter) {
		connection = dbConnection;
		dataTypeConverter = typeConverter;
	}
	
	public ReleaseFileDataLoader(RvfDynamicDataSource dataSource, String schemaName, MySqlDataTypeConverter typeConverter) {
		this.dataSource = dataSource;
		this.schemaName = schemaName;
		dataTypeConverter = typeConverter;
		
	}


	/**TODO
	 * @param rf2FileName
	 * @return
	 * @throws SQLException
	 * @throws FileRecognitionException
	 */
	public String createTableSQL(final String rf2FileName) throws SQLException, FileRecognitionException {
		final SchemaFactory schemaFactory = new SchemaFactory();
		final TableSchema tableSchema = schemaFactory.createSchemaBean(rf2FileName);
		//TODO need to have logic in place so that table name can be constructed following current convention if
		//not found for a new file
		return createTable(tableSchema);
	}

	
	/**
	 * @param rf2TextFileRootPath
	 * @param rf2Files
	 * @param rf2FilesLoaded 
	 * @throws SQLException
	 */
	public void loadFilesIntoDB(final String rf2TextFileRootPath, final String[] rf2Files, List<String> rf2FilesLoaded) throws SQLException {
		final long start = System.currentTimeMillis();
		ExecutorService executorService =  Executors.newCachedThreadPool();
		List<Future<String>> tasks = new ArrayList<>();
		for (final String rf2FileName : rf2Files) {
			final String rvfTableName = RF2FileTableMapper.getLegacyTableName(rf2FileName);
			if( rvfTableName == null) {
				LOGGER.warn("No matching table name found for RF2 file:" + rf2FileName);
				continue;
			}
			final Future<String> future = executorService.submit(new Callable<String>() {

				@Override
				public String call() throws Exception {
					final String configStr = "SET bulk_insert_buffer_size= 1024 * 1024 * 256;";
					final String disableIndex = "ALTER TABLE " + rvfTableName + " DISABLE KEYS;";
					final String enableIndex = "ALTER TABLE " + rvfTableName + " ENABLE KEYS;";
					final String loadFile = "load data local infile '" + rf2TextFileRootPath + "/" + rf2FileName + "' into table " + rvfTableName
							+ " columns terminated by '\\t' "
							+ " lines terminated by '\\r\\n' "
							+ " ignore 1 lines";
					LOGGER.info(loadFile);
					
					try (Connection connection = dataSource.getConnection(schemaName); 
						Statement statement = connection.createStatement()) {
						statement.execute(configStr);
						statement.execute(disableIndex);
						statement.execute(loadFile);
						statement.execute(enableIndex);
					}
					return rf2FileName;
				}
			});
			tasks.add(future);
		}
		for (Future<String> task : tasks) {
			try {
				rf2FilesLoaded.add(task.get());
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("Thread interrupted while waiting for get rf2 file loading result.", e.fillInStackTrace());
			}
		}
		final long end = System.currentTimeMillis();
		LOGGER.info("Time taken to load in seconds " + (end-start)/1000);
	}
	
	/* "create table concept_d(\n" + 
				"id bigint(20) not null,\n" + 
				"effectivetime char(8) not null,\n" + 
				"active char(1) not null,\n" + 
				"moduleid bigint(20) not null,\n" + 
				"definitionstatusid bigint(20) not null,\n" + 
				"key idx_id(id),\n" + 
				"key idx_effectivetime(effectivetime),\n" + 
				"key idx_active(active),\n" + 
				"key idx_moduleid(moduleid),\n" + 
				"key idx_definitionstatusid(definitionstatusid)\n" + 
				") engine=myisam default charset=utf8;"*/
	
	private String createTable(final TableSchema tableSchema) throws SQLException {
		final String rvfTableName = RF2FileTableMapper.getLegacyTableName(tableSchema.getFilename());
		if (rvfTableName == null) {
			return null;
		}
		final StringBuilder builder = new StringBuilder()
				.append("create table " + rvfTableName + "(\n");
		boolean firstField = true;
		final StringBuilder indexBuilder = new StringBuilder();
		for (final Field field : tableSchema.getFields()) {
			if (firstField) {
				firstField = false;
			} else {
				builder.append(",\n");
			}
			final DataType type = field.getType();
			final String typeString = dataTypeConverter.convert(type).toLowerCase();

			final String fieldName = field.getName().toLowerCase();
			builder.append(fieldName)
					.append(" ")
					.append(typeString)
					.append(" ")
					.append( "not null");
			
			if (isFieldRequiredIndexing(tableSchema,field)) {
				if (!firstField) {
					indexBuilder.append(",\n");
				}
				indexBuilder.append("key idx_")
				.append(fieldName)
				.append("(")
				.append(fieldName)
				.append(")");
			}
		}
		builder.append(indexBuilder.toString())
				.append("\n")
				.append(")");
		builder.append(" engine=myisam default charset=utf8;");
		return builder.toString();
	}

	private boolean isFieldRequiredIndexing(final TableSchema tableSchema, final Field field) {
		if (ComponentType.TEXT_DEFINITION == tableSchema.getComponentType()) {
			if ("term".equals(field.getName())) {
				return false;
			}
		}
		return true;
	}
}
