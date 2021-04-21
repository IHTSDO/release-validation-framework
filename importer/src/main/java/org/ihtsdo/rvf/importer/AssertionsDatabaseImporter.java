	package org.ihtsdo.rvf.importer;

	import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.SimpleAssertion;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.service.AssertionService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facebook.presto.sql.parser.StatementSplitter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

	/**
	 * An implementation of a {@link org.ihtsdo.rvf.importer.AssertionsDatabaseImporter} that imports older
	 * Release Assertion Toolkit content via XML and SQL files. The XML file defines the assertions and the SQL files are
	 * used to populate the corresponding tests.
	 */
	@Service
	@Transactional
	public class AssertionsDatabaseImporter {

		private static final String CREATE_PROCEDURE = "CREATE PROCEDURE";
		private static final String JSON_EXTENSION = ".json";
		private static final Logger logger = LoggerFactory.getLogger(AssertionsDatabaseImporter.class);
		private static final String RESOURCE_PATH_SEPARATOR = "/";

		@Value("${rvf.assertion.import.required:false}")
		private boolean assertionImportRequired;

		protected ObjectMapper objectMapper = new ObjectMapper();
		private Map<String, String> lookupMap = new HashMap<>();
		@Autowired
		AssertionService assertionService;

		public boolean isAssertionImportRequired() {
			List<Assertion> assertions = assertionService.findAll();
			if (assertions == null || assertions.isEmpty() || assertionImportRequired) {
				return true;
			}
			return false;
		}

		public void importAssertionsFromFile(final InputStream manifestInputStream, final String sqlResourcesFolderLocation){
			// get JDOM document from given manifest file
			final Document xmlDocument = getJDomDocumentFromFile(manifestInputStream);
			if(xmlDocument != null)
			{
				final XPathFactory factory = XPathFactory.instance();
				final XPathExpression expression = factory.compile("//script");
				final List<Element> scriptElements = expression.evaluate(xmlDocument);
				if(scriptElements.size() > 0)
				{
					// get various values from script element
					for(final Element element : scriptElements)
					{
						final Assertion assertion = createAssertionFromElement(element);
						if(assertion != null){
							try
							{
								logger.info("Created assertion id : " + assertion.getAssertionId());
								assert UUID.fromString(element.getAttributeValue("uuid")).equals(assertion.getUuid());
								// get Sql file name from element and use it to add SQL test
								final String sqlFileName = element.getAttributeValue("sqlFile");
								logger.info("sqlFileName = " + sqlFileName);
								String category = element.getAttributeValue("category");
								/*
									We know category is written as file-centric-validation, component-centric-validation, etc
									We use this to generate the corresponding using folder name = category - validation
								  */
								final int index = category.indexOf("validation");
								if(index > -1)
								{
									category = category.substring(0, index-1);
								}
								logger.info("category = " + category);

								String sqlResourceFileName = sqlResourcesFolderLocation+ RESOURCE_PATH_SEPARATOR + category + RESOURCE_PATH_SEPARATOR + sqlFileName;
								InputStream sqlInputStream = getClass().getResourceAsStream( sqlResourceFileName);
								if (sqlInputStream != null) {
									final String sqlString = readStream(sqlInputStream);
									// add test to assertion
									addSqlTestToAssertion(assertion, sqlString);
								} else {
									String msg = "Failed to find sql file name from source:" + sqlResourceFileName + " for assertion uuid:" + assertion.getUuid();
									logger.error(msg);
									throw new IllegalStateException(msg);
								}
							}
							catch (final Exception e) {
								logger.warn("Error reading sql from input stream. Nested exception is : " + e.getMessage());
								throw new IllegalStateException("Failed to add sql script test to assertion with uuid:" + assertion.getUuid(), e);
							}
						}
						else{
							throw new IllegalStateException("Error creating assertion");
						}
					}

					// finally print all lookup map contents for debugging - //todo save somewhere?
				logger.debug("lookupMap = " + lookupMap);
				}
				else{
					logger.error("There are no script elements to import in the XML file provided. Please note that the " +
							"XML file should contain element named script");
				}
			}
			else{
				logger.warn("Error generating document from xml file passed : " + manifestInputStream);
			}
		}

		protected Assertion createAssertionFromElement(final Element element){

			final String category = element.getAttributeValue("category");
			logger.info("category = " + category);
			final String uuid = element.getAttributeValue("uuid");
			logger.info("uuid = " + uuid);
			final String text = element.getAttributeValue("text");
			logger.info("text = " + text);
			final String sqlFileName = element.getAttributeValue("sqlFile");
			logger.info("sqlFileName = " + sqlFileName);
			final String severity = element.getAttributeValue("severity");
			logger.info("severity = " + severity);

			// add entities using rest client
			final UUID key = UUID.fromString(uuid);
			final Assertion assertionFromDb = assertionService.findAssertionByUUID(key);
			final Assertion assertion;
			if (assertionFromDb != null) {
			    assertion = assertionFromDb;
			} else {
			    assertion = new Assertion();
			    assertion.setUuid(key);
			}
			assertion.setAssertionText(text);
			assertion.setKeywords(category);
			assertion.setSeverity(severity);
			if (assertionFromDb == null) {
			    return assertionService.create(assertion);
			} else {

			    assertionService.deleteTests(assertionFromDb, assertionService.getTests(assertionFromDb));
			    return assertionService.save(assertion);
			}
		}



		public void addSqlTestToAssertion(final Assertion assertion, final String sql){

			final List<String> statements = new ArrayList<>();
			final StatementSplitter splitter = new StatementSplitter(sql);
			if (splitter.getCompleteStatements() == null || splitter.getCompleteStatements().isEmpty()) {
				logger.warn("SQL statements not ending with ;" + sql );
			}
			final StringBuilder storedProcedureSql = new StringBuilder();
			boolean storedProcedureFound = false;
			for(final StatementSplitter.Statement statement : splitter.getCompleteStatements())
			{
				String cleanedSql = statement.statement();
				logger.debug("sql to be cleaned:" + cleanedSql);
				if ( cleanedSql.startsWith(CREATE_PROCEDURE) || cleanedSql.startsWith(CREATE_PROCEDURE.toLowerCase())) {
					storedProcedureFound = true;
				}
				// tokenise and process statement
				final StringTokenizer tokenizer = new StringTokenizer(cleanedSql);
				while(tokenizer.hasMoreTokens())
				{
					String token = tokenizer.nextToken();
					// we know sometimes tokenizer messed up and leaves a trailing ), so we clena this up
					if(token.endsWith(")")){
						token = token.substring(0, token.length() - 1);
					}
					final Map<String, String> schemaMapping = getRvfSchemaMapping(token);
					if(schemaMapping.keySet().size() > 0){

						lookupMap.put(token, schemaMapping.get(token));
						// now replace all instances with rvf mapping
						if (schemaMapping.get(token) != null)
						{
							cleanedSql = cleanedSql.replaceAll(token, schemaMapping.get(token));
						}

					}
				}
				cleanedSql = cleanedSql.replaceAll("runid", "run_id");
				cleanedSql = cleanedSql.replaceAll("assertionuuid", "assertion_id");
				cleanedSql = cleanedSql.replaceAll("assertiontext,", "");
				cleanedSql = cleanedSql.replaceAll("'<ASSERTIONTEXT>',", "");
				logger.debug("cleaned sql:" + cleanedSql);
				if (!storedProcedureFound) {
				   statements.add(cleanedSql);
				} else {
					storedProcedureSql.append(cleanedSql + ";\n");
				}
			}
			if (storedProcedureFound && storedProcedureSql.length() > 0) {
				statements.add(storedProcedureSql.toString());
				logger.debug("Stored proecure found:" + storedProcedureSql.toString());
			}

			uploadTest(assertion, sql, statements);
		}

		private void uploadTest (Assertion assertion, String orignalSql, List<String> sqlStatements) {
			final ExecutionCommand command = new ExecutionCommand();
			command.setTemplate(orignalSql);
			command.setStatements(sqlStatements);
			final Test test = new Test();
			test.setType(TestType.SQL);
			test.setName(assertion.getAssertionText());
			test.setCommand(command);
			// we have to add as a list of tests, since api spec expects list of tests
			final List<Test> tests = new ArrayList<>();
			tests.add(test);

			//import via assertion service
//			if( assertionService.findAssertionByUUID(assertion.getUuid()) == null ) {
//				assertion = assertionService.create(assertion);
//			}
			logger.debug("Adding tests for assertion id" + assertion.getAssertionId());
			assertionService.addTests(assertion, tests);

		}

		protected static Document getJDomDocumentFromFile(final InputStream manifestInputStream){

			try {
				final SAXBuilder sax = new SAXBuilder();
				return sax.build(manifestInputStream);
			}
			catch (JDOMException | IOException e) {
				logger.warn("Nested exception is : " + e.fillInStackTrace());
				return null;
			}
		}

		protected static String readStream(final InputStream is) throws Exception {
			final InputStreamReader reader = new InputStreamReader(is);
			final StringBuilder builder = new StringBuilder();
			final char buffer[] = new char[1024];
			// Wait for at least 1 byte (e.g. stdin)
			int n = reader.read(buffer);
			builder.append(buffer, 0, n);
			while(reader.ready()) {
				n = reader.read(buffer);
				builder.append(buffer, 0, n);
			}
			return builder.toString();
		}

		protected Map<String, String> getRvfSchemaMapping(String ratSchema){
			String rvfSchema = "";
			ratSchema = ratSchema.trim();
			final String originalRatSchema = ratSchema;
			boolean currOrPrevFound = false;
			if(ratSchema.startsWith("curr_")){
				rvfSchema = "<PROSPECTIVE>";
				// we strip the prefix - note we don't include _ in length since strings are 0 indexed
				ratSchema = ratSchema.substring("curr_".length());
				currOrPrevFound = true;
			}
			else if(ratSchema.startsWith("prev_")){
				rvfSchema = "<PREVIOUS>";
				// we strip the prefix - note we don't include _ in length since strings are 0 indexed
				ratSchema = ratSchema.substring("prev_".length());
				currOrPrevFound = true;
			}
			else if(ratSchema.startsWith("v_")){
				// finally process token that represents temp tables - starts with v_
				ratSchema = ratSchema.substring("v_".length());
				rvfSchema = "<TEMP>" + "." + ratSchema;
			}

			// hack to clean up conditions where tokenisation produces schema mappings with ) at the end
			if(currOrPrevFound && ratSchema.endsWith(")")){
				ratSchema = ratSchema.substring(0, ratSchema.lastIndexOf(")"));
			}

			// now process for release type suffix
			if(ratSchema.endsWith("_s")){
				// we strip the suffix
				ratSchema = ratSchema.substring(0, ratSchema.length() - 2);
				rvfSchema = rvfSchema + "." + ratSchema + "_<SNAPSHOT>";
			}
			else if(ratSchema.endsWith("_d")){
				// we strip the suffix
				ratSchema = ratSchema.substring(0, ratSchema.length() - 2);
				rvfSchema = rvfSchema + "." + ratSchema + "_<DELTA>";
			}
			else if(ratSchema.endsWith("_f")){
				// we strip the suffix
				ratSchema = ratSchema.substring(0, ratSchema.length() - 2);
				rvfSchema = rvfSchema + "." + ratSchema + "_<FULL>";
			}

			if (rvfSchema.length() > 0) {
				final Map<String, String> map = new HashMap<>();
				map.put(originalRatSchema, rvfSchema);

				return map;
			}
			else{
				return Collections.EMPTY_MAP;
			}

		}


		/**
		 * Attempts to import any .json file in taretDir and child directories
		 * @param targetDir
		 * @param keywords
		 * @throws JsonProcessingException
		 */
		public void importAssertionsFromDirectory(File targetDir, String keywords) throws JsonProcessingException {
			logger.info("Loading json files from {}", targetDir);
			ObjectMapper mapper = new ObjectMapper();
			SimpleAssertion assertion = null;
			for (File file : targetDir.listFiles()) {
				if (file.isDirectory()) {
					importAssertionsFromDirectory(file, keywords + "," + file.getName());
				} else {
					if (file.getName().endsWith(JSON_EXTENSION)) {
						try {
							assertion = mapper.readValue(file, SimpleAssertion.class);
							//If keywords are not specified in the file, take them from the directory name
							if (assertion.getKeywords() == null || assertion.getKeywords().isEmpty()) {
								assertion.setKeywords(keywords);
							}
						} catch (Exception e) {
							logger.error("Failed to parse {} ", file.getName(), e);
						}
						createOrUpdateAssertion(assertion);
					} else {
						logger.info ("Skipping non-json file {}", file.getAbsolutePath());
					}
				}
			}

		}

		@SuppressWarnings("rawtypes")
		private void createOrUpdateAssertion(SimpleAssertion simpleAssertion) throws JsonProcessingException {
			Assertion assertion = simpleAssertion.toAssertion();
			//Do we need to create that assertion or does it already exist?
			if ( assertionService.find(assertion.getAssertionId()) != null) {
				assertionService.delete(assertion);
			}
			assertion = assertionService.create(assertion);
			List<String> tests = simpleAssertion.getTestsAsList();
			uploadTest(assertion, null, tests);
		}
	}

