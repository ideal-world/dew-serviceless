(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.JVM = f()}})(function(){var define,module,exports;return (function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
module.exports={
  "ak": "ak1",
  "sk": "sk1",
  "env": {
    "dev": {
      "ak": "ak_dev",
      "sk": "sk_dev",
      "db": {
        "url": "mysqlxxxx"
      }
    },
    "test": {
    },
    "prod": {
      "db": {
        "url": "mysqlxxxx",
        "user": "",
        "pwd": ""
      }
    }
  }
}

},{}],2:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const sdk = require("@idealworld/sdk/dist/jvm");
exports.DewSDK = sdk.DewSDK;
sdk.initDefaultSDK("http://127.0.0.1:8888", "app1");
sdk.DewSDK.setting.aksk("ak1", "sk1");

const TodoAction1_test = require("./TodoAction1.test");
exports.TodoAction1_test = TodoAction1_test;
TodoAction1_test;

const TodoAction2_test = require("./TodoAction2.test");
exports.TodoAction2_test = TodoAction2_test;
TodoAction2_test;

},{"./TodoAction1.test":3,"./TodoAction2.test":4,"@idealworld/sdk/dist/jvm":7}],3:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.addItem = exports.fetchItems = exports.db = void 0;
const sdk_1 = require("@idealworld/sdk/dist/jvm");
const Enum_1 = require("@idealworld/sdk/dist/domain/Enum");
let crt = require('../../../dew.json');
const config = sdk_1.DewSDK.setting.conf(crt, 'prod');
const DB_URL = config.db.url;
const DB_USER = config.db.user;
const DB_PWD = config.db.pwd;
exports.db = sdk_1.DewSDK.reldb.subject("todoDB");
async function init() {
    await sdk_1.DewSDK.iam.resource.subject.create('todoDB', Enum_1.ResourceKind.RELDB, "ToDo数据库", DB_URL, DB_USER, DB_PWD);
    await exports.db.exec(`create table if not exists todo
(
    id bigint auto_increment primary key,
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user varchar(255) not null comment '创建者OpenId',
    content varchar(255) not null comment '内容'
)
comment '任务表'`, []);
    await exports.db.exec('insert into todo(content,create_user) values (?,?)', ['这是个示例', '']);
}
init();
async function fetchItems() {
    return doFetchItems();
}
exports.fetchItems = fetchItems;
async function doFetchItems() {
    if (sdk_1.DewSDK.iam.auth.fetch() == null) {
        return [];
    }
    if (sdk_1.DewSDK.iam.auth.fetch()?.roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return exports.db.exec('select * from todo', []);
    }
    return exports.db.exec('select * from todo where create_user = ?', [sdk_1.DewSDK.iam.auth.fetch()?.accountCode]);
}
async function addItem(content) {
    if (sdk_1.DewSDK.iam.auth.fetch() == null) {
        throw '请先登录';
    }
    await sdk_1.DewSDK.cache.set("xxx", content);
    return exports.db.exec('insert into todo(content,create_user) values (?, ?)', [content, sdk_1.DewSDK.iam.auth.fetch()?.accountCode])
        .then(() => null);
}
exports.addItem = addItem;

},{"../../../dew.json":1,"@idealworld/sdk/dist/domain/Enum":6,"@idealworld/sdk/dist/jvm":7}],4:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.ioTestDtos = exports.ioTestDto = exports.ioTestMap = exports.ioTestObj = exports.ioTestArr3 = exports.ioTestArr2 = exports.ioTestArr = exports.ioTestNum = exports.ioTestStr = exports.removeItem = void 0;
const sdk_1 = require("@idealworld/sdk/dist/jvm");
const TodoAction1_test_1 = require("./TodoAction1.test");
async function removeItem(itemId) {
    if (sdk_1.DewSDK.iam.auth.fetch()?.roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return TodoAction1_test_1.db.exec('delete from todo where id = ? ', [itemId])
            .then(() => null);
    }
    return TodoAction1_test_1.db.exec('delete from todo where id = ? and create_user = ?', [itemId, sdk_1.DewSDK.iam.auth.fetch()?.accountCode])
        .then(delRowNumber => {
        // TODO
        if (delRowNumber[0] === 1) {
            return null;
        }
        throw '权限错误';
    });
}
exports.removeItem = removeItem;
async function ioTestStr(str, num, arr, obj) {
    return str;
}
exports.ioTestStr = ioTestStr;
async function ioTestNum(str, num, arr, obj) {
    return num;
}
exports.ioTestNum = ioTestNum;
async function ioTestArr(str, num, arr, obj) {
    return arr;
}
exports.ioTestArr = ioTestArr;
async function ioTestArr2() {
    return ['xxxcxccc'];
}
exports.ioTestArr2 = ioTestArr2;
async function ioTestArr3() {
    return [{
            id: 1,
            content: 'xxxcxccc',
            createUserName: '',
            createUserId: ''
        }];
}
exports.ioTestArr3 = ioTestArr3;
async function ioTestObj(str, num, arr, obj) {
    return obj;
}
exports.ioTestObj = ioTestObj;
async function ioTestMap(map) {
    map['add'] = 'add';
    return map;
}
exports.ioTestMap = ioTestMap;
async function ioTestDto(dto) {
    dto.createUserId = '100';
    return dto;
}
exports.ioTestDto = ioTestDto;
async function ioTestDtos(dtos) {
    dtos[0].createUserId = '100';
    return dtos;
}
exports.ioTestDtos = ioTestDtos;

},{"./TodoAction1.test":3,"@idealworld/sdk/dist/jvm":7}],5:[function(require,module,exports){
"use strict";
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
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    Object.defineProperty(o, k2, { enumerable: true, get: function() { return m[k]; } });
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.loadConfig = exports.setSignature = exports.setCurrentTime = exports.setAjax = exports.SDK = void 0;
const iamSDK = __importStar(require("./module/IAMSDK"));
const cacheSDK = __importStar(require("./module/CacheSDK"));
const httpSDK = __importStar(require("./module/HttpSDK"));
const reldbSDK = __importStar(require("./module/RelDBSDK"));
const taskSDK = __importStar(require("./module/TaskSDK"));
const request = __importStar(require("./util/DewRequest"));
const DewRequest_1 = require("./util/DewRequest");
/**
 * Dew SDK.
 */
class SDK {
    /**
     * 初始化SDK.
     * @param serverUrl 服务网关地址
     * @param appId 当前应用Id
     */
    constructor(serverUrl, appId) {
        this.request = new DewRequest_1.DewRequest();
        this._setting = () => {
            const that = this;
            function aksk(ak, sk) {
                that.request.setToken("");
                that.request.setAkSk(ak, sk);
            }
            return {
                aksk: aksk,
                conf: loadConfig
            };
        };
        this.iam = new iamSDK.IAMSDK(this.request);
        this.reldb = reldbSDK.reldbSDK(this.request);
        this.cache = cacheSDK.cacheSDK(this.request);
        this.http = httpSDK.httpSDK(this.request);
        this.task = taskSDK.taskSDK(this.request);
        this.setting = this._setting();
        this.serverUrl = serverUrl;
        this.appId = appId;
        this.request.setServerUrl(serverUrl);
        this.request.setAppId(appId);
    }
}
exports.SDK = SDK;
function setAjax(impl) {
    request.setAjaxImpl(impl);
}
exports.setAjax = setAjax;
function setCurrentTime(impl) {
    request.setCurrentTime(impl);
}
exports.setCurrentTime = setCurrentTime;
function setSignature(impl) {
    request.setSignature(impl);
}
exports.setSignature = setSignature;
function loadConfig(confContext, envName) {
    const config = typeof confContext === "string" ? JSON.parse(confContext.trim()) : confContext;
    console.log("Load config " + envName + " ENV ");
    if (config.hasOwnProperty('env') && config['env'].hasOwnProperty(envName)) {
        let envConfig = config['env'][envName];
        delete config['env'];
        for (let k in config) {
            if (!envConfig.hasOwnProperty(k)) {
                envConfig[k] = config[k];
            }
        }
        return envConfig;
    }
    else {
        delete config['env'];
        return config;
    }
}
exports.loadConfig = loadConfig;

},{"./module/CacheSDK":8,"./module/HttpSDK":9,"./module/IAMSDK":10,"./module/RelDBSDK":11,"./module/TaskSDK":12,"./util/DewRequest":13}],6:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.ExposeKind = exports.AuthSubjectKind = exports.ResourceKind = exports.AccountIdentKind = exports.CommonStatus = exports.OptActionKind = void 0;
var OptActionKind;
(function (OptActionKind) {
    /**
     * 是否存在.
     */
    OptActionKind["EXISTS"] = "EXISTS";
    /**
     * 获取.
     */
    OptActionKind["FETCH"] = "FETCH";
    /**
     * 创建.
     */
    OptActionKind["CREATE"] = "CREATE";
    /**
     * 更新.
     */
    OptActionKind["MODIFY"] = "MODIFY";
    /**
     * 局部更新（仅HTTP）.
     */
    OptActionKind["PATCH"] = "PATCH";
    /**
     * 删除.
     */
    OptActionKind["DELETE"] = "DELETE";
})(OptActionKind = exports.OptActionKind || (exports.OptActionKind = {}));
var CommonStatus;
(function (CommonStatus) {
    /**
     * 禁用
     */
    CommonStatus["DISABLED"] = "DISABLED";
    /**
     * 启用
     */
    CommonStatus["ENABLED"] = "ENABLED";
})(CommonStatus = exports.CommonStatus || (exports.CommonStatus = {}));
var AccountIdentKind;
(function (AccountIdentKind) {
    /**
     * 用户名 + 密码.
     */
    AccountIdentKind["USERNAME"] = "USERNAME";
    /**
     * 租户间授权认证.
     */
    AccountIdentKind["AUTH_IDENT"] = "AUTH-IDENT";
    /**
     * 手机号 + 验证码.
     */
    AccountIdentKind["PHONE"] = "PHONE";
    /**
     * 邮箱 + 密码.
     */
    AccountIdentKind["EMAIL"] = "EMAIL";
    /**
     * 微信小程序OAuth.
     */
    AccountIdentKind["WECHAT_XCX"] = "WECHAT-XCX";
})(AccountIdentKind = exports.AccountIdentKind || (exports.AccountIdentKind = {}));
var ResourceKind;
(function (ResourceKind) {
    /**
     * 菜单.
     */
    ResourceKind["MENU"] = "MENU";
    /**
     * 页面元素.
     */
    ResourceKind["ELEMENT"] = "ELEMENT";
    /**
     * OAuth.
     */
    ResourceKind["OAUTH"] = "OAUTH";
    /**
     * 关系数据库.
     */
    ResourceKind["RELDB"] = "RELDB";
    /**
     * 缓存.
     */
    ResourceKind["CACHE"] = "CACHE";
    /**
     * MQ.
     */
    ResourceKind["MQ"] = "MQ";
    /**
     * 对象存储.
     */
    ResourceKind["OBJECT"] = "OBJECT";
    /**
     * Task.
     */
    ResourceKind["TASK"] = "TASK";
    /**
     * HTTP(s).
     */
    ResourceKind["HTTP"] = "HTTP";
})(ResourceKind = exports.ResourceKind || (exports.ResourceKind = {}));
var AuthSubjectKind;
(function (AuthSubjectKind) {
    /**
     * 租户.
     */
    AuthSubjectKind["TENANT"] = "TENANT";
    /**
     * 应用.
     */
    AuthSubjectKind["APP"] = "APP";
    /**
     * 角色.
     */
    AuthSubjectKind["ROLE"] = "ROLE";
    /**
     * 群组节点.
     */
    AuthSubjectKind["GROUP_NODE"] = "GROUP_NODE";
    /**
     * 账户.
     */
    AuthSubjectKind["ACCOUNT"] = "ACCOUNT";
})(AuthSubjectKind = exports.AuthSubjectKind || (exports.AuthSubjectKind = {}));
var ExposeKind;
(function (ExposeKind) {
    /**
     * 应用级.
     */
    ExposeKind["APP"] = "APP";
    /**
     * 租户级.
     */
    ExposeKind["TENANT"] = "TENANT";
    /**
     * 系统级.
     */
    ExposeKind["GLOBAL"] = "GLOBAL";
})(ExposeKind = exports.ExposeKind || (exports.ExposeKind = {}));

},{}],7:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.initDefaultSDK = exports.DewSDK = void 0;
const DewSDK_1 = require("./DewSDK");
/**
 * 初始化默认的SDK
 * @param serverUrl 服务网关地址
 * @param appId 当前应用Id
 */
function initDefaultSDK(serverUrl, appId) {
    exports.DewSDK = new DewSDK_1.SDK(serverUrl, appId);
}
exports.initDefaultSDK = initDefaultSDK;
DewSDK_1.setAjax((url, headers, data) => {
    return new Promise((resolve, reject) => {
        try {
            // @ts-ignore
            resolve({ data: JSON.parse($.req(url, headers, data)) });
        }
        catch (e) {
            reject({ "message": e.getMessage(), "stack": [] });
        }
    });
});
DewSDK_1.setCurrentTime(() => {
    // @ts-ignore
    return $.currentTime();
});
DewSDK_1.setSignature((text, key) => {
    // @ts-ignore
    return $.signature(text, key);
});

},{"./DewSDK":5}],8:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.CacheSDK = exports.cacheSDK = void 0;
const Enum_1 = require("../domain/Enum");
function cacheSDK(request) {
    return new CacheSDK("default", request);
}
exports.cacheSDK = cacheSDK;
class CacheSDK {
    constructor(codePostfix, request) {
        this.resourceSubjectCode = ".cache." + codePostfix;
        this.request = request;
    }
    exists(key) {
        checkKey(key);
        return cache(this.request, "existsCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.EXISTS, key);
    }
    hexists(key, fieldName) {
        checkKey(key);
        return cache(this.request, "hexistsCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.EXISTS, key + "/" + fieldName);
    }
    get(key) {
        checkKey(key);
        return cache(this.request, "getCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, key);
    }
    hgetall(key) {
        checkKey(key);
        return cache(this.request, "hgetallCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, key + '/*');
    }
    hget(key, fieldName) {
        checkKey(key);
        return cache(this.request, "hgetCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, key + '/' + fieldName);
    }
    del(key) {
        checkKey(key);
        return cache(this.request, "delCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, key);
    }
    hdel(key, fieldName) {
        checkKey(key);
        return cache(this.request, "hdelCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, key + '/' + fieldName);
    }
    incrby(key, step) {
        checkKey(key);
        return cache(this.request, "incrbyCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '?incr=true', step + '');
    }
    hincrby(key, fieldName, step) {
        checkKey(key);
        return cache(this.request, "hincrbyCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '/' + fieldName + '?incr=true', step + '');
    }
    set(key, value) {
        checkKey(key);
        return cache(this.request, "setCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key, value);
    }
    hset(key, fieldName, value) {
        checkKey(key);
        return cache(this.request, "hsetCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '/' + fieldName, value);
    }
    setex(key, value, expireSec) {
        checkKey(key);
        return cache(this.request, "setexCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '?expire=' + expireSec, value);
    }
    expire(key, expireSec) {
        checkKey(key);
        return cache(this.request, "expireCache", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '?expire=' + expireSec);
    }
    subject(codePostfix) {
        return new CacheSDK(codePostfix, this.request);
    }
}
exports.CacheSDK = CacheSDK;
function checkKey(key) {
    if (key.indexOf('/') !== -1) {
        throw 'key不能包含[/]';
    }
}
function cache(request, name, resourceSubjectCode, optActionKind, pathAndQuery, body) {
    return request.req(name, 'cache://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body);
}

},{"../domain/Enum":6}],9:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.HttpSDK = exports.httpSDK = void 0;
const Enum_1 = require("../domain/Enum");
function httpSDK(request) {
    return new HttpSDK("default", request);
}
exports.httpSDK = httpSDK;
class HttpSDK {
    constructor(codePostfix, request) {
        this.resourceSubjectCode = ".http." + codePostfix;
        this.request = request;
    }
    get(pathAndQuery, header) {
        return http(this.request, this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, pathAndQuery, header);
    }
    delete(pathAndQuery, header) {
        return http(this.request, this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, pathAndQuery, header);
    }
    post(pathAndQuery, body, header) {
        return http(this.request, this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, pathAndQuery, header, body);
    }
    put(pathAndQuery, body, header) {
        return http(this.request, this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.MODIFY, pathAndQuery, header, body);
    }
    patch(pathAndQuery, body, header) {
        return http(this.request, this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.PATCH, pathAndQuery, header, body);
    }
    subject(codePostfix) {
        return new HttpSDK(codePostfix, this.request);
    }
}
exports.HttpSDK = HttpSDK;
function http(request, resourceSubjectCode, optActionKind, pathAndQuery, header, body) {
    if (!pathAndQuery.startsWith('/')) {
        pathAndQuery = '/' + pathAndQuery;
    }
    if (!header) {
        header = {};
    }
    header["Content-Type"] = "application/json charset=utf-8";
    return request.req('http', 'http://' + resourceSubjectCode + pathAndQuery, optActionKind, body, header, true);
}

},{"../domain/Enum":6}],10:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.IAMSDK = void 0;
const Enum_1 = require("../domain/Enum");
const iamModuleName = 'iam.http.iam';
class IAMSDK {
    constructor(request) {
        this.identOptInfo = null;
        this._tenant = () => {
            const that = this;
            function registerTenant(tenantName, appName, tenantAdminUsername, tenantAdminPassword) {
                return that.request.req('createTenant', 'http://' + iamModuleName + '/common/tenant', Enum_1.OptActionKind.CREATE, {
                    tenantName: tenantName,
                    appName: appName,
                    accountUserName: tenantAdminUsername,
                    accountPassword: tenantAdminPassword,
                });
            }
            function fetchTenant() {
                return that.request.req('fetchTenant', 'http://' + iamModuleName + '/console/tenant/tenant', Enum_1.OptActionKind.FETCH);
            }
            return {
                register: registerTenant,
                fetch: fetchTenant
            };
        };
        this._app = () => {
            const that = this;
            function registerApp(appName) {
                return that.request.req('registerApp', 'http://' + iamModuleName + '/common/app', Enum_1.OptActionKind.CREATE, {
                    appName: appName,
                });
            }
            function createApp(appName) {
                return that.request.req('createApp', 'http://' + iamModuleName + '/console/tenant/app', Enum_1.OptActionKind.CREATE, {
                    name: appName
                });
            }
            function fetchApp(appId) {
                return that.request.req('fetchApp', 'http://' + iamModuleName + '/console/tenant/app/' + appId, Enum_1.OptActionKind.FETCH);
            }
            function fetchPublicKey() {
                return that.request.req('fetchPublicKey', 'http://' + iamModuleName + '/console/app/app/publicKey', Enum_1.OptActionKind.FETCH);
            }
            function listAppIdents() {
                return that.request.req('listAppIdents', 'http://' + iamModuleName + '/console/app/app/ident', Enum_1.OptActionKind.FETCH);
            }
            function fetchAppIdentSk(identId) {
                return that.request.req('fetchAppIdentSk', 'http://' + iamModuleName + '/console/app/app/ident/' + identId + '/sk', Enum_1.OptActionKind.FETCH);
            }
            return {
                register: registerApp,
                create: createApp,
                fetch: fetchApp,
                ident: {
                    list: listAppIdents,
                    fetchSk: fetchAppIdentSk,
                },
                key: {
                    fetchPublicKey: fetchPublicKey
                }
            };
        };
        this._account = () => {
            const that = this;
            function registerAccount(accountName) {
                return that.request.req('registerAccount', 'http://' + iamModuleName + '/console/tenant/account', Enum_1.OptActionKind.CREATE, {
                    name: accountName
                });
            }
            function createAccountIdent(accountId, identKind, ak, sk) {
                return that.request.req('addAccountIdent', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/ident', Enum_1.OptActionKind.CREATE, {
                    kind: identKind,
                    ak: ak,
                    sk: sk
                });
            }
            function bindApp(accountId, appId) {
                return that.request.req('bindApp', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/app/' + appId, Enum_1.OptActionKind.CREATE);
            }
            function bindRole(accountId, roleId) {
                return that.request.req('bindRole', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/role/' + roleId, Enum_1.OptActionKind.CREATE);
            }
            return {
                register: registerAccount,
                bindApp: bindApp,
                bindRole: bindRole,
                ident: {
                    create: createAccountIdent
                }
            };
        };
        this._role = () => {
            const that = this;
            function createRoleDef(roleDefCode, roleDefName) {
                return that.request.req('createRoleDef', 'http://' + iamModuleName + '/console/app/role/def', Enum_1.OptActionKind.CREATE, {
                    code: roleDefCode,
                    name: roleDefName
                });
            }
            function createRole(relRoleDefId) {
                return that.request.req('createRole', 'http://' + iamModuleName + '/console/app/role', Enum_1.OptActionKind.CREATE, {
                    relRoleDefId: relRoleDefId
                });
            }
            return {
                createRoleDef: createRoleDef,
                createRole: createRole
            };
        };
        this._resource = () => {
            const that = this;
            function fetchMenu() {
                return that.request.req('fetchMenu', 'menu://' + iamModuleName + '/common/resource?kind=menu', Enum_1.OptActionKind.FETCH);
            }
            function fetchEle() {
                return that.request.req('fetchEle', 'element://' + iamModuleName + '/common/resource?kind=element', Enum_1.OptActionKind.FETCH);
            }
            function createResourceSubject(code, kind, name, uri, ak, sk) {
                return that.request.req('createResourceSubject', 'http://' + iamModuleName + '/console/app/resource/subject', Enum_1.OptActionKind.CREATE, {
                    codePostfix: code,
                    kind: kind,
                    name: name,
                    uri: uri,
                    ak: ak,
                    sk: sk
                });
            }
            function fetchResourceSubjects(codeLike, nameLike, kind) {
                return that.request.req('fetchResourceSubjects', 'http://' + iamModuleName + '/console/app/resource/subject?code='
                    + (codeLike ? codeLike : '') + '&name=' + (nameLike ? nameLike : '') + '&kind=' + (kind ? kind : ''), Enum_1.OptActionKind.FETCH);
            }
            function createResource(name, pathAndQuery, resourceSubjectId) {
                return that.request.req('createResource', 'http://' + iamModuleName + '/console/app/resource', Enum_1.OptActionKind.CREATE, {
                    name: name,
                    pathAndQuery: pathAndQuery,
                    relResourceSubjectId: resourceSubjectId
                });
            }
            return {
                create: createResource,
                subject: {
                    create: createResourceSubject,
                    fetch: fetchResourceSubjects
                },
                menu: {
                    fetch: fetchMenu
                },
                ele: {
                    fetch: fetchEle
                },
            };
        };
        this._policy = () => {
            const that = this;
            function createAuthPolicy(subjectKind, subjectId, resourceId) {
                return that.request.req('createAuthPolicy', 'http://' + iamModuleName + '/console/app/authpolicy', Enum_1.OptActionKind.CREATE, {
                    relSubjectKind: subjectKind,
                    relSubjectIds: subjectId,
                    subjectOperator: "EQ",
                    relResourceId: resourceId,
                    resultKind: "ACCEPT"
                });
            }
            return {
                create: createAuthPolicy
            };
        };
        this._auth = () => {
            const that = this;
            function fetchLoginInfo() {
                return that.identOptInfo;
            }
            function createLoginInfo(identOptInfo) {
                if (identOptInfo === null || identOptInfo.trim() === '') {
                    setLoginInfo(null);
                }
                else {
                    setLoginInfo(JSON.parse(identOptInfo));
                }
            }
            function setLoginInfo(identOptInfo) {
                that.identOptInfo = identOptInfo;
                if (that.identOptInfo === null) {
                    that.request.setToken('');
                }
                else {
                    that.request.setToken(that.identOptInfo.token);
                }
                that.request.setAkSk('', '');
            }
            function login(userName, password) {
                that.request.setToken('');
                return that.request.req('login', 'http://' + iamModuleName + '/common/login', Enum_1.OptActionKind.CREATE, {
                    ak: userName,
                    sk: password,
                    relAppCode: that.request.getAppId()
                })
                    .then(identOptInfo => {
                    that.identOptInfo = identOptInfo;
                    that.request.setToken(identOptInfo.token);
                    return identOptInfo;
                });
            }
            function logout() {
                return that.request.req('logout', 'http://' + iamModuleName + '/common/logout', Enum_1.OptActionKind.DELETE)
                    .then(() => {
                    that.request.setToken('');
                });
            }
            return {
                fetch: fetchLoginInfo,
                create: createLoginInfo,
                set: setLoginInfo,
                login: login,
                logout: logout,
            };
        };
        this.tenant = this._tenant();
        this.app = this._app();
        this.account = this._account();
        this.role = this._role();
        this.resource = this._resource();
        this.policy = this._policy();
        this.auth = this._auth();
        this.request = request;
    }
}
exports.IAMSDK = IAMSDK;

},{"../domain/Enum":6}],11:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.RelDBSDK = exports.reldbSDK = void 0;
const Enum_1 = require("../domain/Enum");
function reldbSDK(request) {
    return new RelDBSDK("default", request);
}
exports.reldbSDK = reldbSDK;
class RelDBSDK {
    constructor(codePostfix, request) {
        this.resourceSubjectCode = ".reldb." + codePostfix;
        this.request = request;
    }
    exec(sql, parameters) {
        return doExec(this.request, this.request.getAppId() + this.resourceSubjectCode, sql, parameters);
    }
    subject(codePostfix) {
        return new RelDBSDK(codePostfix, this.request);
    }
}
exports.RelDBSDK = RelDBSDK;
function doExec(request, resourceSubjectCode, rawSql, parameters) {
    let sql = rawSql.trim().toLocaleLowerCase();
    let action = sql.startsWith('select ') && sql.indexOf(' from ') !== -1 ? 'FETCH'
        : sql.startsWith('insert ') && sql.indexOf(' into ') !== -1 && sql.indexOf(' values ') !== -1 ? 'CREATE'
            : sql.startsWith('update ') && sql.indexOf(' set ') !== -1 && sql.indexOf(' where ') !== -1 ? 'MODIFY'
                : sql.startsWith('delete ') && sql.indexOf(' where ') !== -1 ? 'DELETE'
                    : sql.startsWith('create table ') ? 'CREATE' : null;
    if (action == null) {
        throw 'SQL操作不合法:' + rawSql.trim();
    }
    return request.req('reldb', 'reldb://' + resourceSubjectCode, Enum_1.OptActionKind[action], JSON.stringify({
        sql: rawSql.trim(),
        parameters: parameters
    }));
}

},{"../domain/Enum":6}],12:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.TaskSDK = exports.taskSDK = void 0;
const Enum_1 = require("../domain/Enum");
function taskSDK(request) {
    return new TaskSDK("default", request);
}
exports.taskSDK = taskSDK;
class TaskSDK {
    constructor(codePostfix, request) {
        this.resourceSubjectCode = ".task." + codePostfix;
        this.request = request;
    }
    initTasks(funs) {
        return task(this.request, "initTasks", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, 'task', funs);
    }
    create(taskCode, fun, cron) {
        let query = '';
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron);
        }
        return task(this.request, "createTask", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, 'task/' + taskCode + query, fun);
    }
    modify(taskCode, fun, cron) {
        let query = '';
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron);
        }
        return task(this.request, "modifyTask", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.MODIFY, 'task/' + taskCode + query, fun);
    }
    delete(taskCode) {
        return task(this.request, "deleteTask", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, 'task/' + taskCode);
    }
    execute(taskCode, parameters) {
        return task(this.request, "executeTask", this.request.getAppId() + this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, 'exec/' + taskCode, parameters);
    }
    subject(codePostfix) {
        return new TaskSDK(codePostfix, this.request);
    }
}
exports.TaskSDK = TaskSDK;
function task(request, name, resourceSubjectCode, optActionKind, pathAndQuery, body) {
    return request.req(name, 'task://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body, {
        'Content-Type': 'text/plain'
    });
}

},{"../domain/Enum":6}],13:[function(require,module,exports){
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.DewRequest = exports.setSignature = exports.setCurrentTime = exports.setAjaxImpl = void 0;
const TOKEN_FLAG = 'Dew-Token';
const APP_FLAG = 'Dew-App-Id';
const REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri";
const REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action";
const AUTHENTICATION_HEAD_NAME = 'Authorization';
const DATE_HEAD_NAME = 'Dew-Date';
let ajax;
let currentTime;
let signature;
function setAjaxImpl(impl) {
    ajax = impl;
}
exports.setAjaxImpl = setAjaxImpl;
function setCurrentTime(impl) {
    currentTime = impl;
}
exports.setCurrentTime = setCurrentTime;
function setSignature(impl) {
    signature = impl;
}
exports.setSignature = setSignature;
class DewRequest {
    constructor() {
        this.appId = '';
        this.token = '';
        this.serverUrl = '';
        this.ak = '';
        this.sk = '';
    }
    setAppId(appId) {
        this.appId = appId;
    }
    getAppId() {
        return this.appId;
    }
    setToken(token) {
        this.token = token;
    }
    setAkSk(ak, sk) {
        this.ak = ak;
        this.sk = sk;
    }
    setServerUrl(serverUrl) {
        if (serverUrl.trim().endsWith("/")) {
            serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1);
        }
        this.serverUrl = serverUrl;
    }
    req(name, resourceUri, optActionKind, body, headers, rawResult) {
        headers = headers ? headers : {};
        if (this.appId) {
            headers[APP_FLAG] = this.appId;
        }
        headers[TOKEN_FLAG] = this.token ? this.token : '';
        let pathAndQuery = '/exec?' + REQUEST_RESOURCE_ACTION_FLAG + '=' + optActionKind + '&' + REQUEST_RESOURCE_URI_FLAG + '=' + encodeURIComponent(resourceUri);
        generateAuthentication('post', pathAndQuery, headers, this.ak, this.sk);
        let url = this.serverUrl + pathAndQuery;
        console.log('[Dew]Request [%s]%s , GW = [%s]', optActionKind, resourceUri, url);
        return new Promise((resolve, reject) => {
            ajax(url, headers, typeof body === "undefined" ? "" : body)
                .then(res => {
                let data = res.data;
                if (rawResult) {
                    resolve(data);
                }
                else {
                    if (data.code === '200') {
                        resolve(data.body);
                    }
                    else {
                        console.error('请求错误 : [' + data.code + ']' + data.message);
                        reject('[' + data.code + ']' + data.message);
                    }
                }
            })
                .catch(error => {
                console.error('服务错误 : [' + error.message + ']' + error.stack);
                reject(error.message);
            });
        });
    }
}
exports.DewRequest = DewRequest;
function generateAuthentication(method, pathAndQuery, headers, ak, sk) {
    if (!ak || !sk) {
        return;
    }
    let item = pathAndQuery.split('?');
    let path = item[0];
    let query = item.length === 2 ? item[1] : '';
    if (query) {
        query = query.split('&').sort((a, b) => a < b ? -1 : 1).join("&");
    }
    let date = currentTime();
    headers[AUTHENTICATION_HEAD_NAME] = ak + ':' + signature(method + '\n' + date + '\n' + path + '\n' + query, sk);
    headers[DATE_HEAD_NAME] = date;
}

},{}]},{},[2])(2)
});
