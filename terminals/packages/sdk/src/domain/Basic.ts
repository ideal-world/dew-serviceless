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

export type JsonMap<V> = {
    [key: string]: V
}


export type Page<V> = {
    /**
     * 当前页，从1开始.
     */
    pageNumber: number
    /**
     * 每页记录数.
     */
    pageSize: number
    /**
     * 总页数.
     */
    pageTotal: number
    /**
     * 总记录数.
     */
    recordTotal: number
    /**
     * 实际对象.
     */
    objects: V[]
}
