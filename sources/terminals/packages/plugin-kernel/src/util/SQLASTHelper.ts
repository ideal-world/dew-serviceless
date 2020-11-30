/*
 * Copyright 2020. gudaoxuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
