create table if not exists dew_del_record
(
    id bigint auto_increment
        primary key,
    content text not null comment '删除内容，Json格式',
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

