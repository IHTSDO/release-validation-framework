package org.ihtsdo.rvf.importer;

import com.facebook.presto.sql.parser.StatementSplitter;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.ExecutionCommand;
import org.ihtsdo.rvf.core.data.model.Test;
import org.ihtsdo.rvf.core.data.model.TestType;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
	 * An implementation of a {@link org.ihtsdo.rvf.importer.AssertionsDatabaseImporter} that imports older
	 * Release Assertion Toolkit content via XML and SQL files. The XML file defines assertions and SQL files are
	 * used to populate the corresponding tests.
	 */
	@Service
	@Transactional
	public class AssertionsDatabaseImporter {

		private static final String CREATE_PROCEDURE = "CREATE PROCEDURE";
		private static final Logger logger = LoggerFactory.getLogger(AssertionsDatabaseImporter.class);
		private static final String RESOURCE_PATH_SEPARATOR = "/";

		@Autowired
		@Qualifier("assertionResourceManager")
		private ResourceManager assertionResourceManager;

		@Autowired
		private AssertionService assertionService;

		public boolean isAssertionImportRequired() {
			List<Assertion> assertions = assertionService.findAll();
			return assertions == null || assertions.isEmpty();
		}

		/**
		 * Imports assertions from the given manifest file input stream. The manifest file should be an XML file
		 * containing a list of assertions to import. Each assertion should contain a list of SQL scripts to execute
		 * to populate the assertion tests.
		 * @param manifestInputStream the manifest file input stream
		 * @param sqlFileDir the directory containing the SQL files
		 * @throws IOException if there is an error reading the manifest file
		 */
		public void importAssertionsFromManifest(final InputStream manifestInputStream, final String sqlFileDir) throws IOException {
			final List<Element> scriptElements = getScriptElements(manifestInputStream);
			if (scriptElements.isEmpty()) {
				logger.warn("There are no script elements to import in the XML file provided. Please note that the " +
						"XML file should contain element named script");
			} else {
				// Create Assertions and tests from script elements
				for (final Element element : scriptElements) {
					createAssertionAndTest(element, sqlFileDir);
				}
			}
		}

		 List<Element> getScriptElements(InputStream manifestInputStream) throws IOException {
			// Get JDOM document from given manifest file input stream
			final Document xmlDocument;
			try {
				final SAXBuilder sax = new SAXBuilder();
				xmlDocument = sax.build(manifestInputStream);
			} catch (JDOMException e) {
				throw new IOException("Failed to parse manifest file.", e);
			}
			final XPathFactory factory = XPathFactory.instance();
			final XPathExpression<Element> expression = factory.compile("//script", new ElementFilter("script"));
			return expression.evaluate(xmlDocument);
		}


		private void createAssertionAndTest(Element element, String sqlFileDir) {
			Assertion assertion = createAssertionFromElement(element);
			// Persist assertion
			assertionService.create(assertion);
			// Add SQL tests to assertion
			try {
				addSqlTestsToAssertion(assertion, element, sqlFileDir);
			} catch (Exception e) {
				String errorMsg = "Failed to add sql script test to assertion with uuid:" + assertion.getUuid();
				logger.error(errorMsg, e);
				throw new IllegalStateException(errorMsg, e);
			}
		}

		private void addSqlTestsToAssertion(Assertion assertion, Element element, String sqlFileDir) throws IOException {
			assert UUID.fromString(element.getAttributeValue("uuid")).equals(assertion.getUuid());
			// get Sql file name from element and use it to add SQL test
			final String sqlFileName = element.getAttributeValue("sqlFile");
			logger.debug("sqlFileName = " + sqlFileName);
			String category = element.getAttributeValue("category");
			// Category is written as file-centric-validation, component-centric-validation, etc.
			// Use this to generate the corresponding using folder name = category - validation
			final int index = category.indexOf("validation");
			if (index > -1) {
				category = category.substring(0, index-1);
			}
			logger.debug("category = {} ", category);
			String sqlFullFilename = sqlFileDir + RESOURCE_PATH_SEPARATOR + category + RESOURCE_PATH_SEPARATOR + sqlFileName;
			InputStream sqlInputStream = assertionResourceManager.readResourceStream(sqlFullFilename);
			if (sqlInputStream == null) {
				String msg = "Failed to find sql file name from source:" + sqlFullFilename + " for assertion uuid:" + assertion.getUuid();
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
			final String sqlString = IOUtils.toString(sqlInputStream, UTF_8);
			// add test to assertion
			addSqlTestToAssertion(assertion, sqlString);
		}

		private Assertion createAssertionFromElement(final Element element){
			final String category = element.getAttributeValue("category");
			final String uuid = element.getAttributeValue("uuid");
			final String text = element.getAttributeValue("text");
			final String severity = element.getAttributeValue("severity");
			final Assertion assertion = new Assertion();
			assertion.setUuid(UUID.fromString(uuid));
			assertion.setAssertionText(text);
			assertion.setKeywords(category);
			assertion.setSeverity(severity);
			return assertion;
		}

		public void addSqlTestToAssertion(final Assertion assertion, final String sql){

			final List<String> statements = new ArrayList<>();
			final StatementSplitter splitter = new StatementSplitter(sql);
			if (splitter.getCompleteStatements() == null || splitter.getCompleteStatements().isEmpty()) {
				logger.warn("SQL statements not ending with ; {}", sql);
			}
			final StringBuilder storedProcedureSql = new StringBuilder();
			boolean storedProcedureFound = false;
			for (final StatementSplitter.Statement statement : splitter.getCompleteStatements()) {
				String cleanedSql = statement.statement();
				logger.debug("cleaning sql for assertion uuid {}", assertion.getUuid());
				logger.debug("sql to be cleaned:");
				logger.debug(cleanedSql);
				if ( cleanedSql.startsWith(CREATE_PROCEDURE) || cleanedSql.startsWith(CREATE_PROCEDURE.toLowerCase())) {
					storedProcedureFound = true;
				}
				// Process SQL statement
				final StringTokenizer tokenizer = new StringTokenizer(cleanedSql);
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					// sometimes tokenizer messed up and leaves a trailing ')', so we clean this up
					if (token.endsWith(")")){
						token = token.substring(0, token.length() - 1);
					}
					if (token.length() > 2 && token.startsWith("'") && token.endsWith("'")){
						token = token.substring(1, token.length() - 1);
					}
					final Map<String, String> schemaMapping = getRvfSchemaMapping(token);
					if (!schemaMapping.keySet().isEmpty()) {
						// now replace all instances with rvf mapping
						if (schemaMapping.get(token) != null) {
							cleanedSql = cleanedSql.replaceAll(token, schemaMapping.get(token));
						}
					}
				}
				cleanedSql = cleanedSql.replaceAll("runid", "run_id");
				cleanedSql = cleanedSql.replaceAll("assertionuuid", "assertion_id");
				cleanedSql = cleanedSql.replaceAll("assertiontext,", "");
				cleanedSql = cleanedSql.replaceAll("'<ASSERTIONTEXT>',", "");
				logger.debug("cleaned sql:");
				logger.debug(cleanedSql);
				if (!storedProcedureFound) {
				   statements.add(cleanedSql);
				} else {
					storedProcedureSql.append(cleanedSql);
					storedProcedureSql.append(";");
					storedProcedureSql.append("\n");
				}
			}
			if (storedProcedureFound && !storedProcedureSql.isEmpty()) {
				statements.add(storedProcedureSql.toString());
				logger.debug("Stored procedure found {}", storedProcedureSql);
			}
			uploadTest(assertion, sql, statements);
		}

		private void uploadTest (Assertion assertion, String originalSql, List<String> sqlStatements) {
			final ExecutionCommand command = new ExecutionCommand();
			command.setTemplate(originalSql);
			command.setStatements(sqlStatements);
			final Test test = new Test();
			test.setType(TestType.SQL);
			test.setName(assertion.getAssertionText());
			test.setCommand(command);
			// we have to add as a list of tests, since api spec expects list of tests
			final List<Test> tests = new ArrayList<>();
			tests.add(test);
			logger.debug("Adding tests for assertion id {}", assertion.getAssertionId());
			assertionService.addTests(assertion, tests);
		}

		private Map<String, String> getRvfSchemaMapping(String ratSchema){
			String rvfSchema = "";
			ratSchema = ratSchema.trim();
			final String originalRatSchema = ratSchema;
			boolean currOrPrevFound = false;
			if (ratSchema.startsWith("curr_")){
				rvfSchema = "<PROSPECTIVE>";
				// we strip the prefix - note we don't include _ in length since strings are 0 indexed
				ratSchema = ratSchema.substring("curr_".length());
				currOrPrevFound = true;
			} else if (ratSchema.startsWith("prev_")){
				rvfSchema = "<PREVIOUS>";
				// we strip the prefix - note we don't include _ in length since strings are 0 indexed
				ratSchema = ratSchema.substring("prev_".length());
				currOrPrevFound = true;
			} else if (ratSchema.startsWith("dependency_")){
				rvfSchema = "<DEPENDENCY>";
				ratSchema = ratSchema.substring("dependency_".length());
			} else if (ratSchema.startsWith("v_")){
				// finally process token that represents temp tables - starts with v_
				ratSchema = ratSchema.substring("v_".length());
				rvfSchema = "<TEMP>" + "." + ratSchema;
			}
			// clean up conditions where tokenization produces schema mappings with ')' at the end
			if (currOrPrevFound && ratSchema.endsWith(")")){
				ratSchema = ratSchema.substring(0, ratSchema.lastIndexOf(")"));
			}
			// now process for release type suffix
			if (ratSchema.endsWith("_s")){
				rvfSchema = rvfSchema + "." + scripSuffix(ratSchema) + "_<SNAPSHOT>";
			} else if (ratSchema.endsWith("_d")){
				rvfSchema = rvfSchema + "." + scripSuffix(ratSchema) + "_<DELTA>";
			} else if (ratSchema.endsWith("_f")){
				rvfSchema = rvfSchema + "." + scripSuffix(ratSchema) + "_<FULL>";
			}
			if (!rvfSchema.isEmpty()) {
				final Map<String, String> map = new HashMap<>();
				map.put(originalRatSchema, rvfSchema);
				return map;
			} else {
				return Collections.emptyMap();
			}
		}

		private static String scripSuffix(String ratSchema) {
			return ratSchema.substring(0, ratSchema.length() - 2);
		}

	}

