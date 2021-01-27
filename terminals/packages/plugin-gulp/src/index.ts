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

import * as DewPlugin from "@idealworld/plugin-kernel";

/**
 * Dew构建核心方法.
 *
 * @param relativeBasePath 要构建到后台的执行目录的相对路径
 * @param isProd 是否是生产环境
 * @param toDist 是否生成文件，不为空时会将编译的文件发送到后台，为空时仅测用将编译的文件写入到指定的目录
 */
export async function dewBuild(relativeBasePath: string, isProd: Boolean, toDist?: string): Promise<void> {
    // @ts-ignore
    return DewPlugin.dewBuild(relativeBasePath, process.env.NODE_ENV ? process.env.NODE_ENV : '', isProd, toDist)
}
