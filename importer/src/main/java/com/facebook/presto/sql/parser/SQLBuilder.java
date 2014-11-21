package com.facebook.presto.sql.parser;

import com.facebook.presto.sql.tree.Statement;
import org.antlr.runtime.tree.CommonTree;

/**
 * Utility class for cleaning up RAT SQL
 */
public class SQLBuilder {

    private static final SqlParser SQL_PARSER = new SqlParser();

    public static Statement printStatement(String sql)
    {
        CommonTree tree = SQL_PARSER.parseStatement(sql);
        System.out.println("tree = " + tree);
//        println(treeToString(tree));
//        println("");

        Statement statement = SQL_PARSER.createStatement(tree);

        return statement;
//        println(statement.toString());
//        println("");

//        println(SqlFormatter.formatSql(statement));
//        println("");
//        assertFormattedSql(SQL_PARSER, statement);
//
//        println(repeat("=", 60));
//        println("");
    }
}
