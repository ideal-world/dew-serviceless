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

create table if not exists dew_config
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	k varchar(255) not null comment 'Key',
	v varchar(3000) not null comment 'Value',
	constraint UKov4r8jpo7opduuwck63hdl6kt
		unique (k)
)
comment '通用配置信息';

create table if not exists dew_del_record
(
	id bigint auto_increment
		primary key,
	content longtext not null comment '删除内容，Json格式',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user varchar(255) not null comment '创建者OpenId',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
    update_user varchar(255) not null comment '最后一次修改者OpenId',
	entity_name varchar(255) not null comment '对象名称',
	record_id varchar(255) not null comment '记录Id',
	constraint UKkcvl9uqv65cso9fs2dryswlkg
		unique (entity_name, record_id)
)
comment '记录删除信息';

create table if not exists iam_account
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	avatar varchar(1000) not null comment '账号头像（路径）',
	name varchar(255) not null comment '账号名称',
	open_id varchar(100) not null comment 'Open Id',
	parameters varchar(2000) not null comment '账号扩展信息，Json格式',
	parent_id bigint not null comment '父账号Id，不存在时为空',
	rel_tenant_id bigint not null comment '关联租户Id',
	status varchar(50) not null comment '账号状态',
	constraint UK8e5w1v41g6818g0f5e7qmxhmi
		unique (open_id, parent_id)
)
comment '账号';

create index IDXqoxo6ajinakf764bmxk92w7y2
	on iam_account (rel_tenant_id, status);

create table if not exists iam_account_app
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	rel_account_id bigint not null comment '关联账号Id',
	rel_app_id bigint not null comment '关联应用Id',
	constraint UKjhevyvb4pxena0dtoo3x05qdk
		unique (rel_account_id, rel_app_id)
)
comment '账号应用关联';

create index IDXi9de746blpeo08vafc3dr6ind
	on iam_account_app (rel_app_id);

create table if not exists iam_account_bind
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	from_account_id bigint not null comment '源租户账号Id',
	from_tenant_id bigint not null comment '源租户Id',
	ident_kind varchar(255) not null comment '绑定使用的账号认证类型名称',
	to_account_id bigint not null comment '目标户账号Id',
	to_tenant_id bigint not null comment '目标租户Id',
	constraint UKltlc4t2jk2ax2bn5jqbfxdklt
		unique (from_account_id, to_account_id)
)
comment '账号绑定';

create index IDXd5pbls2wrseb56lie3woxi6ak
	on iam_account_bind (to_tenant_id);

create index IDXvlkusrg2ubeq6vskcmxp08qp
	on iam_account_bind (from_tenant_id);

create table if not exists iam_account_group
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	rel_account_id bigint not null comment '关联账号Id',
	rel_group_node_id bigint not null comment '关联群组节点Id',
	constraint UKpc0hwtn4y3spjcup4x9m2b9wb
		unique (rel_account_id, rel_group_node_id)
)
comment '账号群组关联';

create index IDX3ry6uai59gurqtvuu7nqikuqe
	on iam_account_group (rel_group_node_id);

create table if not exists iam_account_ident
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	ak varchar(255) not null comment '账号认证名称',
	kind varchar(100) not null comment '账号认证类型名称',
	rel_account_id bigint not null comment '关联账号Id',
	rel_tenant_id bigint not null comment '关联租户Id',
	sk varchar(255) not null comment '账号认证密钥',
	valid_end_time bigint not null comment '账号认证有效结束时间',
	valid_start_time bigint not null comment '账号认证有效开始时间',
	constraint UKfq7m36t7wu1x64w3xhon4pf9v
		unique (rel_tenant_id, kind, ak)
)
comment '账号认证';

create index IDX7xu3sl9j1og86u266n4bp53oa
	on iam_account_ident (rel_tenant_id, rel_account_id, kind, ak, valid_start_time, valid_end_time);

create index IDX8v0ywodmhx0nrpqtkela5t714
	on iam_account_ident (rel_tenant_id, rel_account_id, kind, ak);

create index IDXncy24n4jdaknv0elvbgvvuai0
	on iam_account_ident (rel_account_id, kind, valid_start_time, valid_end_time);

create table if not exists iam_account_role
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	rel_account_id bigint not null comment '关联账号Id',
	rel_role_id bigint not null comment '关联角色Id',
	constraint UKd354dk326hn02bgr5lafrs3o9
		unique (rel_account_id, rel_role_id)
)
comment '账号角色关联';

create index IDX4haa22xu0379nl99a9qvywh7c
	on iam_account_role (rel_role_id);

create table if not exists iam_app
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	icon varchar(1000) not null comment '应用图标（路径）',
	name varchar(255) not null comment '应用名称',
	parameters varchar(2000) not null comment '应用扩展信息，Json格式',
	pri_key varchar(1000) not null comment '私钥',
	pub_key varchar(255) not null comment '公钥',
	rel_tenant_id bigint not null comment '关联租户Id',
	status varchar(50) not null comment '应用状态',
	constraint UKiy2dnjxng6jb88g7r7v0o6rgk
		unique (rel_tenant_id, name)
)
comment '应用';

create index IDX1jjwc4sh23jovpthkvapym5ix
	on iam_app (status);

create table if not exists iam_app_ident
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	ak varchar(255) not null comment '应用认证名称（Access Key Id）',
	note varchar(1000) not null comment '应用认证用途',
	rel_app_id bigint not null comment '关联应用Id',
	sk varchar(1000) not null comment '应用认证密钥（Secret Access Key）',
	valid_time bigint not null comment '应用认证有效时间',
	constraint UKljeojtvy3u0scc3mp39hr0874
		unique (ak)
)
comment '应用认证';

create index IDXi4gnko54gfoijmtxfe9p2nfl4
	on iam_app_ident (rel_app_id, valid_time);

create table if not exists iam_auth_policy
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	action_kind varchar(100) not null comment '操作类型名称',
	effective_time bigint not null comment '生效时间',
	exclusive tinyint(1) not null comment '是否排他',
	expired_time bigint not null comment '失效时间',
	rel_app_id bigint not null comment '关联应用Id',
	rel_resource_id bigint not null comment '关联资源Id',
	rel_subject_ids varchar(10000) not null comment '关联权限主体Ids,有多个时逗号分隔,注意必须存在最后一个逗号',
	rel_subject_kind varchar(20) not null comment '关联权限主体类型名称',
	rel_tenant_id bigint not null comment '关联租户Id',
	result_kind varchar(100) not null comment '操作结果名称',
	subject_operator varchar(20) not null comment '关联权限主体运算类型名称'
)
comment '权限策略';

create table if not exists iam_group
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	expose_kind varchar(100) not null comment '开放等级类型名称',
	rel_app_id bigint not null comment '关联应用Id',
	rel_tenant_id bigint not null comment '关联租户Id',
	code varchar(255) not null comment '群组编码',
	icon varchar(1000) not null comment '群组图标（路径）',
	kind varchar(100) not null comment '群组类型名称',
	name varchar(255) not null comment '群组名称',
	rel_group_id bigint not null comment '关联群组Id，用于多树合成',
	rel_group_node_id bigint not null comment '关联群起始组节点Id，用于多树合成',
	sort int not null comment '显示排序，asc',
	constraint UKoipgouysoa4a57klvpxede9gu
		unique (rel_tenant_id, rel_app_id, code)
)
comment '群组';

create index IDXfs1qyakrurqoi32v30xxbp1ne
	on iam_group (expose_kind);

create table if not exists iam_group_node
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	bus_code varchar(1000) not null comment '业务编码',
	code varchar(1000) not null comment '节点编码',
	name varchar(255) not null comment '节点名称',
	parameters varchar(2000) not null comment '节点扩展信息，Json格式',
	rel_group_id bigint not null comment '关联群组Id'
)
comment '群组节点';

create table if not exists iam_resource
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	expose_kind varchar(100) not null comment '开放等级类型名称',
	rel_app_id bigint not null comment '关联应用Id',
	rel_tenant_id bigint not null comment '关联租户Id',
	action varchar(1000) not null comment '触发后的操作，多用于菜单链接',
	icon varchar(1000) not null comment '资源图标（路径）',
	name varchar(255) not null comment '资源名称',
	parent_id bigint not null comment '资源所属组Id',
	rel_resource_subject_id bigint not null comment '关联资源主体Id',
	res_group tinyint(1) not null comment '是否是资源组',
	sort int not null comment '资源显示排序，asc',
	uri varchar(5000) not null comment 'URI'
)
comment '资源';

create index IDX2tig999ttn9s7d67l6wifr3bw
	on iam_resource (expose_kind);

create index IDXqtt1cl5bpsru88k4ut4x54gim
	on iam_resource (parent_id);

create table if not exists iam_resource_subject
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	ak varchar(1000) not null comment 'AK，部分类型支持写到URI中',
	code varchar(255) not null comment '资源主体编码',
	kind varchar(100) not null comment '资源类型名称',
	name varchar(255) not null comment '资源主体名称',
	platform_account varchar(1000) not null comment '第三方平台账号名',
	platform_project_id varchar(1000) not null comment '第三方平台项目名，如华为云的ProjectId',
	rel_app_id bigint not null comment '关联应用Id',
	rel_tenant_id bigint not null comment '关联租户Id',
	sk varchar(1000) not null comment 'SK，部分类型支持写到URI中',
	sort int not null comment '资源主体显示排序，asc',
	timeout_ms int not null comment '执行超时',
	uri varchar(5000) not null comment '资源主体连接URI',
	constraint UKqha1627a3vy6hscmfnw28viwg
		unique (code)
)
comment '资源主体';

create index IDXei74af3xufobhvh61mujbngqt
	on iam_resource_subject (rel_tenant_id, rel_app_id, kind);

create table if not exists iam_role
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	expose_kind varchar(100) not null comment '开放等级类型名称',
	rel_app_id bigint not null comment '关联应用Id',
	rel_tenant_id bigint not null comment '关联租户Id',
	name varchar(255) not null comment '角色名称',
	rel_group_node_id bigint not null comment '关联群组节点Id',
	rel_role_def_id bigint not null comment '关联角色定义Id',
	sort int not null comment '显示排序，asc',
	constraint UKfm6o97ipma326nnxsdw5xaja8
		unique (rel_role_def_id, rel_group_node_id)
)
comment '角色';

create index IDXnaxipcsq8ps9d86qvrg5a5e3k
	on iam_role (expose_kind);

create table if not exists iam_role_def
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	code varchar(255) not null comment '角色定义编码',
	name varchar(255) not null comment '角色定义名称',
	rel_app_id bigint not null comment '关联应用Id',
	rel_tenant_id bigint not null comment '关联租户Id',
	sort int not null comment '显示排序，asc',
	constraint UKq61oa4wbx1ggd9tqp4a7juw33
		unique (rel_tenant_id, rel_app_id, code)
)
comment '角色定义';

create table if not exists iam_tenant
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	allow_account_register tinyint(1) not null comment '是否开放账号注册',
	icon varchar(1000) not null comment '租户图标（路径）',
	name varchar(255) not null comment '租户名称',
	parameters varchar(5000) not null comment '租户扩展信息，Json格式',
	status varchar(50) not null comment '租户状态'
)
comment '租户';

create index IDX3x7h6khiqtlwf1uewxh78xfx9
	on iam_tenant (status);

create table if not exists iam_tenant_ident
(
	id bigint auto_increment
		primary key,
	create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
	create_user varchar(255) not null comment '创建者OpenId',
	update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
	update_user varchar(255) not null comment '最后一次修改者OpenId',
	kind varchar(100) not null comment '租户认证类型名称',
	rel_tenant_id bigint not null comment '关联租户Id',
	valid_ak_rule varchar(2000) not null comment '认证AK校验正则规则',
	valid_ak_rule_note varchar(2000) not null comment '认证AK校验正则规则说明',
	valid_sk_rule varchar(2000) not null comment '认证SK校验正则规则',
	valid_sk_rule_note varchar(2000) not null comment '认证SK校验正则规则说明',
	valid_time_sec bigint not null comment '认证有效时间（秒）',
	constraint UKcuj2mhpkb8x6brgrk61psetvl
		unique (rel_tenant_id, kind)
)
comment '租户认证配置';

create table if not exists iam_tenant_cert
(
    id bigint auto_increment
    primary key,
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user varchar(255) not null comment '创建者OpenId',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后一次修改时间',
    update_user varchar(255) not null comment '最后一次修改者OpenId',
    category varchar(255) not null comment '凭证类型名称',
    version tinyint not null comment '凭证保留的版本数量',
    rel_tenant_id bigint not null comment '关联租户Id',
    constraint UKkxsomgbfhpvdtpo5w39sy6oyw
    unique (rel_tenant_id, category)
    )
    comment '租户凭证配置';
