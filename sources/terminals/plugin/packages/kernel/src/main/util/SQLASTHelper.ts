import {Parser} from "node-sql-parser";
import {AST, TableColumnAst} from "node-sql-parser/types";

export class SQLParser {

    parser = new Parser()
    database: string = 'MySQL'

    option(database: string): void {
        this.database = database
    }

    astify(sql: string): AST[] | AST {
        return this.parser.astify(sql, {
            database: this.database
        })
    }

    sqlify(ast: AST): string {
        return this.parser.sqlify(ast, {
            database: this.database
        })
    }

    parse(sql: string): TableColumnAst {
        return this.parser.parse(sql, {
            database: this.database
        })
    }

}
