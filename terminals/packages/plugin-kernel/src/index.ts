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

import {Node} from "acorn";
import * as JSASTHelper from "./util/JSASTHelper";

export function checkAndReplace(fileContent: string): string {
    let ast = JSASTHelper.parse(fileContent)
    let items = replaceAndExtractInitFun(fileContent, ast)
    let initFun = items[1]
    return items[0]
}

function replaceAndExtractInitFun(fileContent: string, ast: Node): string[] {
    console.log(fileContent)
    return []
}
