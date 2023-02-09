package org.ihtsdo.rvf.executionservice;

import org.ihtsdo.rvf.executionservice.util.MySqlDataTypeConverter;
import org.ihtsdo.rvf.executionservice.util.RF2FileTableMapper;
import org.ihtsdo.snomed.util.rf2.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Load release files into DB tables.
 *
 */
public class ReleaseFileDataLoader {

	private static final int MAX_THREAD_POOL_SIZE = 100;
	public static final String TERM = "term";
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
	public void loadFilesIntoDB(final String rf2TextFileRootPath, final String[] rf2Files, List<String> rf2FilesLoaded) throws RVFExecutionException {
		final long start = System.currentTimeMillis();
		// Use fixed thread pool size to avoid reaching too many mysql connections
		ExecutorService executorService =  Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
		List<Future<String>> tasks = new ArrayList<>();
		for (final String rf2FileName : rf2Files) {
			final String rvfTableName = RF2FileTableMapper.getLegacyTableName(rf2FileName);
			if( rvfTableName == null) {
				LOGGER.warn("No matching table name found for RF2 file:" + rf2FileName);
				continue;
			}
			final Future<String> future = executorService.submit(() -> {
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
			});
			tasks.add(future);
		}
		for (Future<String> task : tasks) {
			try {
				rf2FilesLoaded.add(task.get());
			} catch (InterruptedException | ExecutionException e) {
				String errorMsg = "Thread interrupted while waiting for get rf2 file loading result.";
				LOGGER.error(errorMsg, e.fillInStackTrace());
				throw new RVFExecutionException(errorMsg, e);
			}
		}
		final long end = System.currentTimeMillis();
		LOGGER.info("Time taken to load in seconds " + (end-start)/1000);
	}

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
				indexBuilder.append(",\n");
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
			if (TERM.equals(field.getName())) {
				return false;
			}
		}
		return true;
	}
}
