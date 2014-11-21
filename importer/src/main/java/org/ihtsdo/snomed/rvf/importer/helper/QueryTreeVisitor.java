package org.ihtsdo.snomed.rvf.importer.helper;

import com.foundationdb.sql.StandardException;
import com.foundationdb.sql.parser.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A class that implements the visitor pattern to parse and detect SQL components
 */
public class QueryTreeVisitor implements Visitor {

    public Set<String> columnNames = new HashSet<>();
    public Set<String> tableNames = new HashSet<>();


    public Visitable visit(Visitable visitable) throws StandardException {
//        String cn = null;
        QueryTreeNode node = (QueryTreeNode)visitable;
//        if( node instanceof ColumnReference )
        if(NodeTypes.COLUMN_REFERENCE == node.getNodeType())
        {
            columnNames.add(((ColumnReference) node).getColumnName());
//            if(((ColumnReference) node).getTableName().equals("ordertable"))
//            {
//                cn =((ColumnReference) node).getColumnName();
//                columnNames.add(cn);
//            }
        }
        else if(NodeTypes.TABLE_ELEMENT_NODE == node.getNodeType()){
            tableNames.add(((TableName) node).getFullTableName());
        }

        return node;
    }
    public Set<String> getColumnNames(){
        return columnNames;
    }

    public Set<String> getTableNames() {
        return tableNames;
    }

    public boolean visitChildrenFirst(Visitable node) {
        return false;
    }

    public boolean stopTraversal() {
        return false;
    }

    public boolean skipChildren(Visitable node) throws StandardException {
        return false;
    }

}
