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

import {pathsToModuleNameMapper} from "ts-jest/utils";

const {compilerOptions} = require("../../tsconfig.json");

module.exports = {
    preset: 'ts-jest',
    testRegex: "/test/.*.test.ts$",
    moduleNameMapper: pathsToModuleNameMapper(compilerOptions.paths, {
        // This has to match the baseUrl defined in tsconfig.json.
        prefix: "<rootDir>/../../",
    }),
}
