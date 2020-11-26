export type IdentOptInfo = {
    token: string,
    accountCode: string,
    roleInfo: RoleInfo[]
    groupInfo: GroupInfo[]
}

export type RoleInfo = {
    code: string,
    name: string,
}

export type GroupInfo = {
    groupCode: string,
    groupNodeCode: string,
    groupNodeBusCode: string,
    groupName: string,
    groupNodeName: string,
}
