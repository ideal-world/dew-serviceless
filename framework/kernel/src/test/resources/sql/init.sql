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

create table if not exists dew_del_record
(
    id bigint auto_increment
        primary key,
    content longtext not null comment '删除内容，Json格式',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user bigint not null comment '创建者Id',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
    update_user bigint not null comment '最后一次修改者Id',
    entity_name varchar(255) not null comment '对象名称',
    record_id varchar(255) not null comment '记录Id',
    constraint UKkcvl9uqv65cso9fs2dryswlkg
        unique (entity_name, record_id)
)
    comment '记录删除信息';

