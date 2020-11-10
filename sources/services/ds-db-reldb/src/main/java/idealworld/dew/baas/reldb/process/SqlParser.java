package idealworld.dew.baas.reldb.process;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.common.enumeration.OptActionKind;
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
        var sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (var sqlStatement : sqlStatements) {
            if (sqlStatement instanceof MySqlInsertStatement) {
                return parseInsert((MySqlInsertStatement) sqlStatement);
            } else if (sqlStatement instanceof MySqlUpdateStatement) {
                // TODO
            } else if (sqlStatement instanceof MySqlDeleteStatement) {
                return parseDelete((MySqlDeleteStatement) sqlStatement);
            } else if (sqlStatement instanceof MySqlSelectQueryBlock) {
                // TODO
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

    private static List<SqlAst> parseDelete(MySqlDeleteStatement sqlStatement) {
        var tableNames = new HashMap<String, String>();
        tableNames.put(sqlStatement.getAlias() != null ? sqlStatement.getAlias() : "", sqlStatement.getTableName().getSimpleName());
        tableNames.put(sqlStatement.getTableName().getSimpleName(), sqlStatement.getTableName().getSimpleName());
        var sqlItems = parseWhere(OptActionKind.DELETE, sqlStatement.getWhere(), tableNames, sqlStatement.getTableName().getSimpleName());
        return sqlItems.stream()
                .collect(Collectors.groupingBy(item -> item.table + "|" + item.getActionKind().toString()))
                .entrySet()
                .stream()
                .map(item -> SqlAst.builder()
                        .actionKind(OptActionKind.parse(item.getKey().split("\\|")[1]))
                        .table(item.getKey().split("\\|")[0])
                        .condFields(item.getValue().stream().filter(i -> i.condField != null).map(i -> i.condField).collect(Collectors.toList()))
                        .returnFields(item.getValue().stream().filter(i -> i.returnField != null).map(i -> i.returnField).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private static List<SqlItem> parseWhere(OptActionKind actionKind, SQLExpr sqlExpr, Map<String, String> tableNames, String defaultTableName) {
        var container = new ArrayList<SqlItem>();
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            container.addAll(parseWhere(actionKind, ((SQLBinaryOpExpr) sqlExpr).getLeft(), tableNames, defaultTableName));
            container.addAll(parseWhere(actionKind, ((SQLBinaryOpExpr) sqlExpr).getRight(), tableNames, defaultTableName));
        } else if (sqlExpr instanceof SQLInListExpr) {
            container.addAll(parseWhere(actionKind, ((SQLInListExpr) sqlExpr).getExpr(), tableNames, defaultTableName));
        } else if (sqlExpr instanceof SQLBetweenExpr) {
            container.addAll(parseWhere(actionKind, ((SQLBetweenExpr) sqlExpr).getTestExpr(), tableNames, defaultTableName));
        } else if (sqlExpr instanceof SQLQueryExpr) {
            var subQueryExpr = ((SQLQueryExpr) sqlExpr).getSubQuery();
            if (subQueryExpr.getQuery() instanceof MySqlSelectQueryBlock) {
                parseTableName(((MySqlSelectQueryBlock) subQueryExpr.getQuery()).getFrom(), tableNames);
                container.addAll(parsReturn(OptActionKind.FETCH, ((MySqlSelectQueryBlock) subQueryExpr.getQuery()).getSelectList(), tableNames, defaultTableName));
                container.addAll(parseWhere(OptActionKind.FETCH, ((MySqlSelectQueryBlock) subQueryExpr.getQuery()).getWhere(), tableNames, defaultTableName));
            }
        } else if (sqlExpr instanceof SQLPropertyExpr) {
            container.add(SqlItem.builder()
                    .actionKind(actionKind)
                    .table(tableNames.get(((SQLPropertyExpr) sqlExpr).getOwner().toString()))
                    .condField(((SQLPropertyExpr) sqlExpr).getName())
                    .build());
        } else if (sqlExpr instanceof SQLIdentifierExpr) {
            container.add(SqlItem.builder()
                    .actionKind(actionKind)
                    .table(defaultTableName)
                    .condField(((SQLIdentifierExpr) sqlExpr).getName())
                    .build());
        }
        return container;
    }

    private static List<SqlItem> parsReturn(OptActionKind actionKind, List<SQLSelectItem> sqlSelectItems, Map<String, String> tableNames, String defaultTableName) {
        var container = new ArrayList<SqlItem>();
        for (var sqlSelectItem : sqlSelectItems) {
            if (sqlSelectItem.getExpr() instanceof SQLPropertyExpr) {
                container.add(SqlItem.builder()
                        .actionKind(actionKind)
                        .table(tableNames.get(((SQLPropertyExpr) sqlSelectItem.getExpr()).getOwner().toString()))
                        .returnField(((SQLPropertyExpr) sqlSelectItem.getExpr()).getName())
                        .build());
            } else {
                // TODO 测试用
                throw new RTException("zzzz" + sqlSelectItem.toString());
            }
        }
        return container;
    }

    public static void parseTableName(SQLTableSource tableSource, Map<String, String> tableNames) {
        if (tableSource instanceof SQLExprTableSource) {
            tableNames.put(tableSource.getAlias(), parseTableName(((SQLExprTableSource) tableSource).getExpr()));
        } else {
            // TODO 测试用
            throw new RTException("yyyy" + tableSource.toString());
        }
    }

    public static String parseTableName(SQLExpr sqlExpr) {
        if (sqlExpr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) sqlExpr).getName();
        }
        // TODO 测试用
        throw new RTException("xxxx" + sqlExpr.toString());
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
    @Builder
    private static class SqlItem {

        private String table;
        private OptActionKind actionKind;
        private String returnField;
        private String condField;

    }

}
