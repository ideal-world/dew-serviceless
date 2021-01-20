/*
 * Copyright 2021. gudaoxuri
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

import gitP, {SimpleGit} from 'simple-git/promise';
import * as fileHelper from './FileHelper';
import {Response} from "simple-git/typings/simple-git";

let git: SimpleGit = gitP()

export function clone(repoPath: string, localPath: string, depth?: number): Response<string> {
    if (fileHelper.exists(localPath)) {
        throw 'The path [' + localPath + '] already exists.'
    } else {
        let option = {}
        if (depth) {
            option['--depth'] = depth
        }
        return git.clone(repoPath, localPath, option)
    }

}

