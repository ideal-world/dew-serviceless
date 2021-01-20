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

create schema if not exists task collate utf8mb4_0900_ai_ci;

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

create table if not exists task_task_def
(
    id         bigint auto_increment
        primary key,
    code       varchar(255) not null comment '任务编码',
    cron       varchar(100) not null comment '定时配置',
    fun        longtext     not null comment '执行函数',
    rel_app_id bigint       not null comment '关联应用Id',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user bigint not null comment '创建者Id',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
    update_user bigint not null comment '最后一次修改者Id',
    constraint UKov4r8jpo7opduuwck63hdl6
        unique (rel_app_id, code)
)
    comment '任务定义';

create table if not exists task_task_inst
(
    id                bigint auto_increment
        primary key,
    start_time        bigint        not null comment '执行开始时间',
    end_time          bigint        not null comment '执行结束时间',
    success           tinyint       not null comment '执行结果',
    message           varchar(1000) not null comment '执行结果',
    rel_task_def_code varchar(255)  not null comment '关联任务定义编码',
    rel_app_id        bigint        not null comment '关联应用Id'
)
    comment '任务实例';


