package idealworld.dew.serviceless.reldb.process;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.parser.ParserException;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class SqlParser {


    public static List<SqlAst> parse(String sql) {
        List<SQLStatement> sqlStatements;
        try {
            sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        } catch (ParserException e) {
            log.warn("[SqlParser]Parse sql error: {}", sql, e);
            return null;
        }
        for (var sqlStatement : sqlStatements) {
            if (sqlStatement instanceof MySqlInsertStatement) {
                return parseInsert((MySqlInsertStatement) sqlStatement);
            } else if (sqlStatement instanceof MySqlUpdateStatement) {
                return parseUpdate((MySqlUpdateStatement) sqlStatement);
            } else if (sqlStatement instanceof MySqlDeleteStatement) {
                return parseDelete((MySqlDeleteStatement) sqlStatement);
            } else if (sqlStatement instanceof SQLSelectStatement) {
                return parseSelect((SQLSelectStatement) sqlStatement);
            } else {
                log.warn("[SqlParser]The operation type {} is not supported yet", sqlStatement.getClass());
                return null;
            }
        }
        return null;
    }

    private static List<SqlAst> parseInsert(MySqlInsertStatement sqlStatement) {
        return new ArrayList<>() {
            {
                add(SqlAst.builder()
                        .actionKind(OptActionKind.CREATE)
                        .table(sqlStatement.getTableName().getSimpleName())
                        .effectFields(sqlStatement.getColumns().stream()
                                .map(col -> ((SQLIdentifierExpr) col).getName())
                                .collect(Collectors.toList()))
                        .build());
            }
        };
    }

    private static List<SqlAst> parseUpdate(MySqlUpdateStatement sqlStatement) {
        var tableNames = new HashMap<String, String>();
        var sqlItems = parseFrom(OptActionKind.MODIFY, sqlStatement.getTableSource(), tableNames);
        sqlItems.addAll(parseSet(sqlStatement.getItems(), tableNames));
        sqlItems.addAll(parseExpr(OptActionKind.MODIFY, FieldKind.CONDITION, sqlStatement.getWhere(), tableNames));
        return packageSqlAst(sqlItems);
    }

    private static List<SqlAst> parseDelete(MySqlDeleteStatement sqlStatement) {
        var tableNames = new HashMap<String, String>();
        var sqlItems = parseFrom(OptActionKind.MODIFY, sqlStatement.getTableSource(), tableNames);
        sqlItems.addAll(parseExpr(OptActionKind.DELETE, FieldKind.CONDITION, sqlStatement.getWhere(), tableNames));
        return packageSqlAst(sqlItems);
    }

    private static List<SqlAst> parseSelect(SQLSelectStatement sqlStatement) {
        var tableNames = new HashMap<String, String>();
        var sqlItems = parseSelect(sqlStatement.getSelect().getQuery(), tableNames);
        return packageSqlAst(sqlItems);
    }

    private static List<SqlItem> parseSelect(SQLSelectQuery sqlSelectQuery, Map<String, String> tableNames) {
        var sqlItems = new ArrayList<SqlItem>();
        if (sqlSelectQuery instanceof MySqlSelectQueryBlock) {
            var query = (MySqlSelectQueryBlock) sqlSelectQuery;
            sqlItems.addAll(parseFrom(OptActionKind.FETCH, query.getFrom(), tableNames));
            sqlItems.addAll(parseReturn(OptActionKind.FETCH, query.getSelectList(), tableNames));
            sqlItems.addAll(parseExpr(OptActionKind.FETCH, FieldKind.CONDITION, query.getWhere(), tableNames));
        }
        return sqlItems;
    }

    private static List<SqlItem> parseSet(List<SQLUpdateSetItem> sqlUpdateSetItems, Map<String, String> tableNames) {
        var container = new ArrayList<SqlItem>();
        for (var sqlUpdateSetItem : sqlUpdateSetItems) {
            container.addAll(parseExpr(OptActionKind.MODIFY, FieldKind.EFFECT, sqlUpdateSetItem.getColumn(), tableNames));
            container.addAll(parseExpr(OptActionKind.MODIFY, FieldKind.EFFECT, sqlUpdateSetItem.getValue(), tableNames));
        }
        return container;
    }

    private static List<SqlItem> parseReturn(OptActionKind actionKind, List<SQLSelectItem> sqlSelectItems, Map<String, String> tableNames) {
        var container = new ArrayList<SqlItem>();
        for (var sqlSelectItem : sqlSelectItems) {
            container.addAll(parseExpr(actionKind, FieldKind.RETURN, sqlSelectItem.getExpr(), tableNames));
        }
        return container;
    }

    private static List<SqlItem> parseFrom(OptActionKind actionKind, SQLTableSource tableSource, Map<String, String> tableNames) {
        var container = new ArrayList<SqlItem>();
        if (tableSource instanceof SQLExprTableSource) {
            var tableName = parseTableName(((SQLExprTableSource) tableSource).getExpr());
            tableNames.put(tableSource.getAlias() != null ? tableSource.getAlias() : "", tableName);
            tableNames.put(tableName, tableName);
        } else if (tableSource instanceof SQLJoinTableSource) {
            container.addAll(parseFrom(actionKind, ((SQLJoinTableSource) tableSource).getLeft(), tableNames));
            container.addAll(parseFrom(actionKind, ((SQLJoinTableSource) tableSource).getRight(), tableNames));
            container.addAll(parseExpr(actionKind, FieldKind.CONDITION, ((SQLJoinTableSource) tableSource).getCondition(), tableNames));
        } else {
            // TODO 测试用
            throw new RTException("yyyy" + tableSource.toString());
        }
        return container;
    }

    private static String parseTableName(SQLExpr sqlExpr) {
        if (sqlExpr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) sqlExpr).getName();
        }
        // TODO 测试用
        throw new RTException("xxxx" + sqlExpr.toString());
    }

    private static List<SqlItem> parseExpr(OptActionKind actionKind, FieldKind fieldKind, SQLExpr sqlExpr, Map<String, String> tableNames) {
        var container = new ArrayList<SqlItem>();
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            container.addAll(parseExpr(actionKind, fieldKind, ((SQLBinaryOpExpr) sqlExpr).getLeft(), tableNames));
            container.addAll(parseExpr(actionKind, fieldKind, ((SQLBinaryOpExpr) sqlExpr).getRight(), tableNames));
        } else if (sqlExpr instanceof SQLInListExpr) {
            container.addAll(parseExpr(actionKind, fieldKind, ((SQLInListExpr) sqlExpr).getExpr(), tableNames));
        } else if (sqlExpr instanceof SQLBetweenExpr) {
            container.addAll(parseExpr(actionKind, fieldKind, ((SQLBetweenExpr) sqlExpr).getTestExpr(), tableNames));
        } else if (sqlExpr instanceof SQLQueryExpr) {
            container.addAll(parseSelect(((SQLQueryExpr) sqlExpr).getSubQuery().getQuery(), tableNames));
        } else if (sqlExpr instanceof SQLPropertyExpr) {
            container.add(new SqlItem(tableNames.get(tableNames.get(((SQLPropertyExpr) sqlExpr).getOwner().toString())), actionKind, fieldKind, ((SQLPropertyExpr) sqlExpr).getName()));
        } else if (sqlExpr instanceof SQLIdentifierExpr) {
            container.add(new SqlItem(tableNames.get(""), actionKind, fieldKind, ((SQLIdentifierExpr) sqlExpr).getName()));
        }
        return container;
    }

    private static List<SqlAst> packageSqlAst(List<SqlItem> sqlItems) {
        return sqlItems.stream()
                .collect(Collectors.groupingBy(item -> item.table + "|" + item.getActionKind().toString()))
                .entrySet()
                .stream()
                .map(item -> SqlAst.builder()
                        .actionKind(OptActionKind.parse(item.getKey().split("\\|")[1]))
                        .table(item.getKey().split("\\|")[0])
                        .condFields(item.getValue().stream().filter(i -> i.condField != null).map(i -> i.condField).collect(Collectors.toList()))
                        .returnFields(item.getValue().stream().filter(i -> i.returnField != null).map(i -> i.returnField).collect(Collectors.toList()))
                        .effectFields(item.getValue().stream().filter(i -> i.effectField != null).map(i -> i.effectField).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }


    @Data
    @Builder
    public static class SqlAst {

        private String table;
        private OptActionKind actionKind;
        @Builder.Default
        private List<String> effectFields = new LinkedList<>();
        @Builder.Default
        private List<String> condFields = new LinkedList<>();
        @Builder.Default
        private List<String> returnFields = new LinkedList<>();

    }


    @Data
    private static class SqlItem {

        private String table;
        private OptActionKind actionKind;
        private String returnField;
        private String condField;
        private String effectField;

        public SqlItem(String table, OptActionKind actionKind, FieldKind fieldKind, String field) {
            this.table = table;
            this.actionKind = actionKind;
            switch (fieldKind) {
                case RETURN:
                    this.returnField = field;
                    break;
                case CONDITION:
                    this.condField = field;
                    break;
                case EFFECT:
                    this.effectField = field;
                    break;
            }
        }
    }

    private enum FieldKind {
        RETURN, CONDITION, EFFECT
    }

}
