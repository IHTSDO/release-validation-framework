package org.ihtsdo.rvf.core.service.util;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.naming.ConfigurationException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class MySqlQueryTransformerTest {
    @Mock
    MysqlExecutionConfig config;
    @Test
    public void transformToStatements() throws BusinessServiceException {
        MySqlQueryTransformer queryTransformer = new MySqlQueryTransformer();
        String sqlToTest = "DELIMITER //\n" +
                "SET default_storage_engine=MYISAM//\n" +
                "DROP TABLE IF EXISTS concept_active//\n" +
                "CREATE TABLE concept_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS\n" +
                "SELECT * FROM concept_<SNAPSHOT> AS concepts WHERE active = 1//\n" +
                "create unique index concept_active_id_ix on concept_active(id)//\n" +
                "create index concept_active_effectivetime_ix on concept_active(effectivetime)//\n" +
                "create index concept_active_definitionstatusid_ix on concept_active(definitionstatusid)//\n" +
                "create index concept_active_moduleid_ix on concept_active(moduleid)//\n" +
                "create index concept_active_active_ix on concept_active(active)//";
        List<String> result = queryTransformer.transformToStatements(sqlToTest);
        assertEquals(8, result.size());
        assertEquals("create index concept_active_active_ix on concept_active(active)", result.get(7));
        assertEquals("CREATE TABLE concept_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS\n" +
                "SELECT * FROM concept_<SNAPSHOT> AS concepts WHERE active = 1", result.get(2));
    }
    @Test
    public void transformToStatementsWithSqlFunctions() throws BusinessServiceException {
        MySqlQueryTransformer queryTransformer = new MySqlQueryTransformer();
        String sqlToTest = "DELIMITER // drop function if exists get_cr_ADRS_PT//\n" +
                "create function get_cr_ADRS_PT(candidate bigint) returns varchar(4500)\n" +
                "BEGIN RETURN (SELECT da.term FROM langrefset_active as lr join description_active as da\n" +
                "                      on da.id = lr.referencedcomponentid WHERE lr.acceptabilityid = 900000000000548007 and conceptid = candidate); END//\n" +
                "\n" +
                "drop function if exists get_cr_FSN//\n" +
                "create function get_cr_FSN(candidate bigint) returns varchar(4500)\n" +
                "BEGIN RETURN (SELECT term FROM description_active where conceptId = candidate and typeId = 900000000000003001 and languageCode = 'en'); END//\n" +
                "\n" +
                "drop function if exists get_cr_PercentDefined//\n" +
                "create function get_cr_PercentDefined(refset bigint) returns decimal(6, 4)\n" +
                "BEGIN SET @refset = refset; SET @refsetSize = (select count(1) from simplerefset_active where refsetId = @refset); " +
                "SET @definedCount = (select count(1) from concept_active where definitionStatusId = 900000000000073002 " +
                "and id in (select referencedComponentId from simplerefset_active where refsetId = @refset)); " +
                "RETURN CONVERT(@definedCount/ @refsetSize * 100,DECIMAL(6,4)); END//\n";
        List<String> result = queryTransformer.transformToStatements(sqlToTest);
        assertEquals(6, result.size());
        assertTrue(result.get(0).startsWith("drop function if exists get_cr_ADRS_PT") );
        assertTrue(result.get(5).startsWith("create function get_cr_PercentDefined(refset bigint)"));
    }
    @Test
    public void transformToStatementsUsingDefaultDelimiter() throws BusinessServiceException {
        MySqlQueryTransformer queryTransformer = new MySqlQueryTransformer();
        String sqlToTest = "SET default_storage_engine=MYISAM/;\n" +
                "DROP TABLE IF EXISTS concept_active;\n" +
                "CREATE TABLE concept_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS\n" +
                "SELECT * FROM concept_<SNAPSHOT> AS concepts WHERE active = 1;\n" +
                "create unique index concept_active_id_ix on concept_active(id);\n" +
                "create index concept_active_effectivetime_ix on concept_active(effectivetime);\n" +
                "create index concept_active_definitionstatusid_ix on concept_active(definitionstatusid);\n" +
                "create index concept_active_moduleid_ix on concept_active(moduleid);\n" +
                "create index concept_active_active_ix on concept_active(active);";
        List<String> result = queryTransformer.transformToStatements(sqlToTest);
        assertEquals(8, result.size());
        assertEquals("create index concept_active_active_ix on concept_active(active)", result.get(7));
        assertEquals("CREATE TABLE concept_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS\n" +
                "SELECT * FROM concept_<SNAPSHOT> AS concepts WHERE active = 1", result.get(2));
    }
    @Test
    public void transformToStatementInvalidDelimiter() {
        MySqlQueryTransformer queryTransformer = new MySqlQueryTransformer();
        String sqlToTest = "DELIMITER // SET default_storage_engine=MYISAM/;\n" +
                "DROP TABLE IF EXISTS concept_active;";
        BusinessServiceException exception = assertThrows(BusinessServiceException.class, () -> {
            queryTransformer.transformToStatements(sqlToTest);
        });

        String expectedMessagePattern = "SQL statements not ending with // SET default_storage_engine=MYISAM";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.startsWith(expectedMessagePattern));

    }

    @Test
    public void transformSql() throws ConfigurationException {
        MySqlQueryTransformer queryTransformer = new MySqlQueryTransformer();
        String sql = "insert into res_concepts_edited\n" +
                "\tselect distinct id\n" +
                "\tfrom <PROSPECTIVE>.concept_<SNAPSHOT>\n" +
                "\twhere id in ( \n" +
                "\t\tselect id from <PROSPECTIVE>.concept_<DELTA>\n" +
                "    union \n" +
                "        select conceptid from <PROSPECTIVE>.description_<DELTA>\n" +
                "    union \n" +
                "        select conceptid from <PROSPECTIVE>.textdefinition_<DELTA>\n" +
                "    union \n" +
                "        select sourceid from <PROSPECTIVE>.stated_relationship_<DELTA>\n" +
                "    union\n" +
                "        select referencedcomponentid from <PROSPECTIVE>.owlexpressionrefset_<DELTA>\n" +
                "    union\n" +
                "        select b.conceptid from <PROSPECTIVE>.langrefset_<DELTA> a \n" +
                "        left join <PROSPECTIVE>.description_<SNAPSHOT> b on a.referencedcomponentid=b.id\n" +
                "    union \n" +
                "        select referencedcomponentid from <PROSPECTIVE>.attributevaluerefset_<DELTA>\n" +
                "        where refsetid = '900000000000489007'\n" +
                "    union \n" +
                "        select a.conceptid from <PROSPECTIVE>.description_<DELTA> a\n" +
                "        join <PROSPECTIVE>.attributevaluerefset_<DELTA> b on a.id = b.referencedcomponentid\n" +
                "        where b.refsetid = '900000000000490003'\n" +
                "\tunion\n" +
                "        select referencedcomponentid\n" +
                "        from <PROSPECTIVE>.associationrefset_<DELTA>\n" +
                "        where refsetid in ('900000000000523009','900000000000526001','900000000000527005','900000000000530003','1186924009','1186921001')\n" +
                "\tunion\n" +
                "        select a.conceptid from <PROSPECTIVE>.description_<DELTA> a \n" +
                "        join <PROSPECTIVE>.associationrefset_<DELTA> b on a.id = b.referencedcomponentid \n" +
                "        where b.refsetid = '900000000000531004'\n" +
                "\tunion\n" +
                "        select referencedcomponentid, <ASSERTIONUUID> from <PROSPECTIVE>.simplerefset_<DELTA>\n" +
                "\tunion\n" +
                "        select referencedcomponentid from <PROSPECTIVE>.simplemaprefset_<DELTA> and RLIKE Binary '[[:<:]]hexachlorophene[[:>:]]'\n" +
                "    )";
        String[] sqlParts = {sql};
        String testAssertionId = "xyz";
        String testQaResult = "abc";
        when(config.getProspectiveVersion()).thenReturn("rvf_au_20221231_230619110027");
        when(config.getPreviousVersion()).thenReturn(String.valueOf(10));
        Map configMap = Map.of("qa_result",testQaResult,
                "<ASSERTIONUUID>", String.valueOf(testAssertionId));
        List<String> result = queryTransformer.transformSql(sqlParts,config, configMap);
        assertEquals(1,result.size());
        assertTrue(!result.get(0).contains("<PROSPECTIVE>"));
        assertTrue(!result.get(0).contains("<ASSERTIONUUID>"));
        assertTrue(result.get(0).contains("rvf_au_20221231_230619110027.description_d"));
        assertTrue(result.get(0).contains("'\\bhexachlorophene\\b'"));
        assertTrue(result.get(0).contains(testAssertionId));
    }


    @Test
    public void transformSqlMissingConfig() {
        MySqlQueryTransformer queryTransformer = new MySqlQueryTransformer();
        String sqlToTest = "SET default_storage_engine=MYISAM/;\n" +
                "DROP TABLE IF EXISTS concept_active;";
        String[] sqlParts = {sqlToTest};
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            queryTransformer.transformSql(sqlParts,config, Collections.emptyMap());
        });

        String expectedMessagePattern = "Failed to find rvf db schema for null";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.startsWith(expectedMessagePattern));

    }

}