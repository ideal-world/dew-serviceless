!function(e){if("object"==typeof exports&&"undefined"!=typeof module)module.exports=e();else if("function"==typeof define&&define.amd)define([],e);else{("undefined"!=typeof window?window:"undefined"!=typeof global?global:"undefined"!=typeof self?self:this).JVM=e()}}((function(){return function e(t,n,o){function i(c,u){if(!n[c]){if(!t[c]){var s="function"==typeof require&&require;if(!u&&s)return s(c,!0);if(r)return r(c,!0);var a=new Error("Cannot find module '"+c+"'");throw a.code="MODULE_NOT_FOUND",a}var d=n[c]={exports:{}};t[c][0].call(d.exports,(function(e){return i(t[c][1][e]||e)}),d,d.exports,e,t,n,o)}return n[c].exports}for(var r="function"==typeof require&&require,c=0;c<o.length;c++)i(o[c]);return i}({1:[function(e,t,n){"use strict";Object.defineProperty(n,"__esModule",{value:!0});const o=e("@idealworld/sdk/dist/jvm");n.DewSDK=o.DewSDK;const i=e("./TodoAction1.test");n.TodoAction1_test=i;const r=e("./TodoAction2.test");n.TodoAction2_test=r},{"./TodoAction1.test":2,"./TodoAction2.test":3,"@idealworld/sdk/dist/jvm":6}],2:[function(e,t,n){"use strict";Object.defineProperty(n,"__esModule",{value:!0}),n.addItem=n.fetchItems=n.init=n.db=void 0;const o=e("@idealworld/sdk/dist/jvm");n.db=o.DewSDK.reldb.subject("todoDB"),n.init=async function(){await n.db.exec("create table if not exists todo\n(\n    id bigint auto_increment primary key,\n    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',\n    create_user varchar(255) not null comment '创建者OpenId',\n    content varchar(255) not null comment '内容'\n)\ncomment '任务表'",[]),await n.db.exec("insert into todo(content,create_user) values (?,?)",["这是个示例",""])},n.fetchItems=async function(){return async function(){if(null==o.DewSDK.iam.auth.fetch())return[];if(o.DewSDK.iam.auth.fetch().roleInfo.some(e=>"APP_ADMIN"===e.defCode))return n.db.exec("select * from todo",[]);return n.db.exec("select * from todo where create_user = ?",[o.DewSDK.iam.auth.fetch().accountCode])}()},n.addItem=async function(e){if(null==o.DewSDK.iam.auth.fetch())throw"请先登录";return await o.DewSDK.cache.set("xxx",e),n.db.exec("insert into todo(content,create_user) values (?, ?)",[e,o.DewSDK.iam.auth.fetch().accountCode]).then(()=>null)}},{"@idealworld/sdk/dist/jvm":6}],3:[function(e,t,n){"use strict";Object.defineProperty(n,"__esModule",{value:!0}),n.ioTestDtos=n.ioTestDto=n.ioTestMap=n.ioTestObj=n.ioTestArr=n.ioTestNum=n.ioTestStr=n.removeItem=void 0;const o=e("@idealworld/sdk/dist/jvm"),i=e("./TodoAction1.test");n.removeItem=async function(e){return o.DewSDK.iam.auth.fetch().roleInfo.some(e=>"APP_ADMIN"===e.defCode)?i.db.exec("delete from todo where id = ? ",[e]).then(()=>null):i.db.exec("delete from todo where id = ? and create_user = ?",[e,o.DewSDK.iam.auth.fetch().accountCode]).then(e=>{if(1===e[0])return null;throw"权限错误"})},n.ioTestStr=async function(e,t,n,o){return e},n.ioTestNum=async function(e,t,n,o){return t},n.ioTestArr=async function(e,t,n,o){return n},n.ioTestObj=async function(e,t,n,o){return o},n.ioTestMap=async function(e){return e.add="add",e},n.ioTestDto=async function(e){return e.createUserId="100",e},n.ioTestDtos=async function(e){return e[0].createUserId="100",e}},{"./TodoAction1.test":2,"@idealworld/sdk/dist/jvm":6}],4:[function(e,t,n){"use strict";var o=this&&this.__createBinding||(Object.create?function(e,t,n,o){void 0===o&&(o=n),Object.defineProperty(e,o,{enumerable:!0,get:function(){return t[n]}})}:function(e,t,n,o){void 0===o&&(o=n),e[o]=t[n]}),i=this&&this.__setModuleDefault||(Object.create?function(e,t){Object.defineProperty(e,"default",{enumerable:!0,value:t})}:function(e,t){e.default=t}),r=this&&this.__importStar||function(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var n in e)"default"!==n&&Object.prototype.hasOwnProperty.call(e,n)&&o(t,e,n);return i(t,e),t};Object.defineProperty(n,"__esModule",{value:!0}),n.SDK=void 0;const c=r(e("./util/Request")),u=r(e("./module/IAMSDK")),s=r(e("./module/CacheSDK")),a=r(e("./module/HttpSDK")),d=r(e("./module/RelDBSDK")),l=r(e("./module/TaskSDK"));n.SDK={init:function(e,t){c.setServerUrl(e),u.init(),d.init(t),s.init(t),a.init(t),l.init(t),n.SDK.reldb=d.reldbSDK(),n.SDK.cache=s.cacheSDK(),n.SDK.http=a.httpSDK(),n.SDK.task=l.taskSDK()},iam:u.iamSDK,reldb:d.reldbSDK(),cache:s.cacheSDK(),http:a.httpSDK(),task:l.taskSDK(),setting:{serverUrl:function(e){c.setServerUrl(e)},aksk:function(e,t){c.setAkSk(e,t)},ajax:function(e){c.setAjaxImpl(e)},currentTime:function(e){c.setCurrentTime(e)},signature:function(e){c.setSignature(e)}}}},{"./module/CacheSDK":7,"./module/HttpSDK":8,"./module/IAMSDK":9,"./module/RelDBSDK":10,"./module/TaskSDK":11,"./util/Request":12}],5:[function(e,t,n){"use strict";Object.defineProperty(n,"__esModule",{value:!0}),n.AuthSubjectKind=n.ResourceKind=n.AccountIdentKind=n.CommonStatus=n.OptActionKind=void 0,function(e){e.EXISTS="EXISTS",e.FETCH="FETCH",e.CREATE="CREATE",e.MODIFY="MODIFY",e.PATCH="PATCH",e.DELETE="DELETE"}(n.OptActionKind||(n.OptActionKind={})),function(e){e.DISABLED="DISABLED",e.ENABLED="ENABLED"}(n.CommonStatus||(n.CommonStatus={})),function(e){e.USERNAME="USERNAME",e.AUTH_IDENT="AUTH-IDENT",e.PHONE="PHONE",e.EMAIL="EMAIL",e.WECHAT_XCX="WECHAT-XCX"}(n.AccountIdentKind||(n.AccountIdentKind={})),function(e){e.MENU="MENU",e.ELEMENT="ELEMENT",e.OAUTH="OAUTH",e.RELDB="RELDB",e.CACHE="CACHE",e.MQ="MQ",e.OBJECT="OBJECT",e.TASK="TASK",e.HTTP="HTTP"}(n.ResourceKind||(n.ResourceKind={})),function(e){e.TENANT="TENANT",e.APP="APP",e.ROLE="ROLE",e.GROUP_NODE="GROUP_NODE",e.ACCOUNT="ACCOUNT"}(n.AuthSubjectKind||(n.AuthSubjectKind={}))},{}],6:[function(e,t,n){"use strict";Object.defineProperty(n,"__esModule",{value:!0}),n.DewSDK=void 0;const o=e("./DewSDK");n.DewSDK=o.SDK},{"./DewSDK":4}],7:[function(e,t,n){"use strict";var o=this&&this.__createBinding||(Object.create?function(e,t,n,o){void 0===o&&(o=n),Object.defineProperty(e,o,{enumerable:!0,get:function(){return t[n]}})}:function(e,t,n,o){void 0===o&&(o=n),e[o]=t[n]}),i=this&&this.__setModuleDefault||(Object.create?function(e,t){Object.defineProperty(e,"default",{enumerable:!0,value:t})}:function(e,t){e.default=t}),r=this&&this.__importStar||function(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var n in e)"default"!==n&&Object.prototype.hasOwnProperty.call(e,n)&&o(t,e,n);return i(t,e),t};Object.defineProperty(n,"__esModule",{value:!0}),n.CacheSDK=n.cacheSDK=n.init=void 0;const c=r(e("../util/Request")),u=e("../domain/Enum");let s=0;n.init=function(e){s=e},n.cacheSDK=function(){return new a("default")};class a{constructor(e){this.resourceSubjectCode=s+".cache."+e}exists(e){return d(e),l("existsCache",this.resourceSubjectCode,u.OptActionKind.EXISTS,e)}hexists(e,t){return d(e),l("hexistsCache",this.resourceSubjectCode,u.OptActionKind.EXISTS,e+"/"+t)}get(e){return d(e),l("getCache",this.resourceSubjectCode,u.OptActionKind.FETCH,e)}hgetall(e){return d(e),l("hgetallCache",this.resourceSubjectCode,u.OptActionKind.FETCH,e+"/*")}hget(e,t){return d(e),l("hgetCache",this.resourceSubjectCode,u.OptActionKind.FETCH,e+"/"+t)}del(e){return d(e),l("delCache",this.resourceSubjectCode,u.OptActionKind.DELETE,e)}hdel(e,t){return d(e),l("hdelCache",this.resourceSubjectCode,u.OptActionKind.DELETE,e+"/"+t)}incrby(e,t){return d(e),l("incrbyCache",this.resourceSubjectCode,u.OptActionKind.CREATE,e+"?incr=true",t+"")}hincrby(e,t,n){return d(e),l("hincrbyCache",this.resourceSubjectCode,u.OptActionKind.CREATE,e+"/"+t+"?incr=true",n+"")}set(e,t){return d(e),l("setCache",this.resourceSubjectCode,u.OptActionKind.CREATE,e,t)}hset(e,t,n){return d(e),l("hsetCache",this.resourceSubjectCode,u.OptActionKind.CREATE,e+"/"+t,n)}setex(e,t,n){return d(e),l("setexCache",this.resourceSubjectCode,u.OptActionKind.CREATE,e+"?expire="+n,t)}expire(e,t){return d(e),l("expireCache",this.resourceSubjectCode,u.OptActionKind.CREATE,e+"?expire="+t)}subject(e){return new a(e)}}function d(e){if(-1!==e.indexOf("/"))throw"key不能包含[/]"}function l(e,t,n,o,i){return c.req(e,"cache://"+t+"/"+o,n,i)}n.CacheSDK=a},{"../domain/Enum":5,"../util/Request":12}],8:[function(e,t,n){"use strict";var o=this&&this.__createBinding||(Object.create?function(e,t,n,o){void 0===o&&(o=n),Object.defineProperty(e,o,{enumerable:!0,get:function(){return t[n]}})}:function(e,t,n,o){void 0===o&&(o=n),e[o]=t[n]}),i=this&&this.__setModuleDefault||(Object.create?function(e,t){Object.defineProperty(e,"default",{enumerable:!0,value:t})}:function(e,t){e.default=t}),r=this&&this.__importStar||function(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var n in e)"default"!==n&&Object.prototype.hasOwnProperty.call(e,n)&&o(t,e,n);return i(t,e),t};Object.defineProperty(n,"__esModule",{value:!0}),n.HttpSDK=n.httpSDK=n.init=void 0;const c=r(e("../util/Request")),u=e("../domain/Enum");let s=0;n.init=function(e){s=e},n.httpSDK=function(){return new a("default")};class a{constructor(e){this.resourceSubjectCode=s+".http."+e}get(e,t){return d(this.resourceSubjectCode,u.OptActionKind.FETCH,e,t)}delete(e,t){return d(this.resourceSubjectCode,u.OptActionKind.DELETE,e,t)}post(e,t,n){return d(this.resourceSubjectCode,u.OptActionKind.CREATE,e,n,t)}put(e,t,n){return d(this.resourceSubjectCode,u.OptActionKind.MODIFY,e,n,t)}patch(e,t,n){return d(this.resourceSubjectCode,u.OptActionKind.PATCH,e,n,t)}subject(e){return new a(e)}}function d(e,t,n,o,i){return n.startsWith("/")||(n="/"+n),o||(o={}),o["Content-Type"]="application/json charset=utf-8",c.req("http","http://"+e+n,t,i,o,!0)}n.HttpSDK=a},{"../domain/Enum":5,"../util/Request":12}],9:[function(e,t,n){"use strict";var o=this&&this.__createBinding||(Object.create?function(e,t,n,o){void 0===o&&(o=n),Object.defineProperty(e,o,{enumerable:!0,get:function(){return t[n]}})}:function(e,t,n,o){void 0===o&&(o=n),e[o]=t[n]}),i=this&&this.__setModuleDefault||(Object.create?function(e,t){Object.defineProperty(e,"default",{enumerable:!0,value:t})}:function(e,t){e.default=t}),r=this&&this.__importStar||function(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var n in e)"default"!==n&&Object.prototype.hasOwnProperty.call(e,n)&&o(t,e,n);return i(t,e),t};Object.defineProperty(n,"__esModule",{value:!0}),n.iamSDK=n.init=void 0;const c=r(e("../util/Request")),u=e("../domain/Enum");let s;n.init=function(){};const a={fetchLoginInfo:()=>s,createLoginInfo(e){a.setLoginInfo(JSON.parse(e))},setLoginInfo(e){s=e,c.setToken(s.token)}},d={login:(e,t,n)=>(c.setToken(""),c.req("login","http://iam/common/login",u.OptActionKind.CREATE,{ak:e,sk:t,relAppId:n}).then(e=>(s=e,c.setToken(e.token),e))),logout:()=>c.req("logout","http://iam/common/logout",u.OptActionKind.DELETE).then(()=>{c.setToken("")}),registerAccount:e=>c.req("registerAccount","http://iam/console/tenant/account",u.OptActionKind.CREATE,{name:e}),createAccountIdent:(e,t,n,o)=>c.req("addAccountIdent","http://iam/console/tenant/account/"+e+"/ident",u.OptActionKind.CREATE,{kind:t,ak:n,sk:o}),bindApp:(e,t)=>c.req("bindApp","http://iam/console/tenant/account/"+e+"/app/"+t,u.OptActionKind.CREATE),bindRole:(e,t)=>c.req("bindRole","http://iam/console/tenant/account/"+e+"/role/"+t,u.OptActionKind.CREATE)},l={fetchMenu:()=>c.req("fetchMenu","menu://iam/common/resource?kind=menu",u.OptActionKind.FETCH),fetchEle:()=>c.req("fetchEle","element://iam/common/resource?kind=element",u.OptActionKind.FETCH),createResourceSubject:(e,t,n,o,i,r)=>c.req("createResourceSubject","http://iam/console/app/resource/subject",u.OptActionKind.CREATE,{codePostfix:e,kind:t,name:n,uri:o,ak:i,sk:r}),createResource:(e,t,n)=>c.req("createResource","http://iam/console/app/resource",u.OptActionKind.CREATE,{name:e,pathAndQuery:t,relResourceSubjectId:n})},f={createAuthPolicy:(e,t,n)=>c.req("createAuthPolicy","http://iam/console/app/authpolicy",u.OptActionKind.CREATE,{relSubjectKind:e,relSubjectIds:t,subjectOperator:"EQ",relResourceId:n,resultKind:"ACCEPT"})},p={registerTenant:(e,t,n,o)=>c.req("createTenant","http://iam/common/tenant",u.OptActionKind.CREATE,{tenantName:e,appName:t,accountUserName:n,accountPassword:o}),fetchTenant:()=>c.req("fetchTenant","http://iam/console/tenant/tenant",u.OptActionKind.FETCH)},h={createApp:e=>c.req("createApp","http://iam/console/tenant/app",u.OptActionKind.CREATE,{name:e}),fetchPublicKey:()=>c.req("fetchPublicKey","http://iam/console/app/app/publicKey",u.OptActionKind.FETCH),listAppIdents:()=>c.req("listAppIdents","http://iam/console/app/app/ident",u.OptActionKind.FETCH),fetchAppIdentSk:e=>c.req("fetchAppIdentSk","http://iam/console/app/app/ident/"+e+"/sk",u.OptActionKind.FETCH)},m={createRoleDef:(e,t)=>c.req("createRoleDef","http://iam/console/app/role/def",u.OptActionKind.CREATE,{code:e,name:t}),createRole:e=>c.req("createRole","http://iam/console/app/role",u.OptActionKind.CREATE,{relRoleDefId:e})};n.iamSDK={auth:{fetch:a.fetchLoginInfo,create:a.createLoginInfo,set:a.setLoginInfo},account:{login:d.login,logout:d.logout,register:d.registerAccount,bindApp:d.bindApp,bindRole:d.bindRole,ident:{create:d.createAccountIdent}},resource:{create:l.createResource,subject:{create:l.createResourceSubject},menu:{fetch:l.fetchMenu},ele:{fetch:l.fetchEle}},tenant:{register:p.registerTenant,fetch:p.fetchTenant},app:{create:h.createApp,ident:{list:h.listAppIdents,fetchSk:h.fetchAppIdentSk},key:{fetchPublicKey:h.fetchPublicKey}},role:{createRoleDef:m.createRoleDef,createRole:m.createRole},policy:{create:f.createAuthPolicy}}},{"../domain/Enum":5,"../util/Request":12}],10:[function(e,t,n){"use strict";var o=this&&this.__createBinding||(Object.create?function(e,t,n,o){void 0===o&&(o=n),Object.defineProperty(e,o,{enumerable:!0,get:function(){return t[n]}})}:function(e,t,n,o){void 0===o&&(o=n),e[o]=t[n]}),i=this&&this.__setModuleDefault||(Object.create?function(e,t){Object.defineProperty(e,"default",{enumerable:!0,value:t})}:function(e,t){e.default=t}),r=this&&this.__importStar||function(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var n in e)"default"!==n&&Object.prototype.hasOwnProperty.call(e,n)&&o(t,e,n);return i(t,e),t};Object.defineProperty(n,"__esModule",{value:!0}),n.RelDBSDK=n.reldbSDK=n.init=void 0;const c=r(e("../util/Request")),u=e("../domain/Enum");let s=0;n.init=function(e){s=e},n.reldbSDK=function(){return new a("default")};class a{constructor(e){this.resourceSubjectCode=s+".reldb."+e}exec(e,t){return function(e,t,n){let o=t.split("|");if(2!==o.length)throw"该SQL语句没有加密 : "+t;return c.req("reldb","reldb://"+e,u.OptActionKind[o[0]],{sql:o[1],parameters:n})}(this.resourceSubjectCode,e,t)}subject(e){return new a(e)}}n.RelDBSDK=a},{"../domain/Enum":5,"../util/Request":12}],11:[function(e,t,n){"use strict";var o=this&&this.__createBinding||(Object.create?function(e,t,n,o){void 0===o&&(o=n),Object.defineProperty(e,o,{enumerable:!0,get:function(){return t[n]}})}:function(e,t,n,o){void 0===o&&(o=n),e[o]=t[n]}),i=this&&this.__setModuleDefault||(Object.create?function(e,t){Object.defineProperty(e,"default",{enumerable:!0,value:t})}:function(e,t){e.default=t}),r=this&&this.__importStar||function(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var n in e)"default"!==n&&Object.prototype.hasOwnProperty.call(e,n)&&o(t,e,n);return i(t,e),t};Object.defineProperty(n,"__esModule",{value:!0}),n.TaskSDK=n.taskSDK=n.init=void 0;const c=r(e("../util/Request")),u=e("../domain/Enum");let s=0;n.init=function(e){s=e},n.taskSDK=function(){return new a("default")};class a{constructor(e){this.resourceSubjectCode=s+".task."+e}initTasks(e){return d("initTasks",this.resourceSubjectCode,u.OptActionKind.CREATE,"task",e)}create(e,t,n){let o="";return n&&(o="?cron="+encodeURIComponent(n)),d("createTask",this.resourceSubjectCode,u.OptActionKind.CREATE,"task/"+e+o,t)}modify(e,t,n){let o="";return n&&(o="?cron="+encodeURIComponent(n)),d("modifyTask",this.resourceSubjectCode,u.OptActionKind.MODIFY,"task/"+e+o,t)}delete(e){return d("deleteTask",this.resourceSubjectCode,u.OptActionKind.DELETE,"task/"+e)}execute(e,t){return d("executeTask",this.resourceSubjectCode,u.OptActionKind.CREATE,"exec/"+e,t)}subject(e){return new a(e)}}function d(e,t,n,o,i){return c.req(e,"task://"+t+"/"+o,n,i,{"Content-Type":"text/plain"})}n.TaskSDK=a},{"../domain/Enum":5,"../util/Request":12}],12:[function(e,t,n){"use strict";Object.defineProperty(n,"__esModule",{value:!0}),n.req=n.setServerUrl=n.setAkSk=n.setToken=n.addMock=n.setSignature=n.setCurrentTime=n.setAjaxImpl=n.mock=void 0;let o,i,r,c="",u="",s="",a="";const d={};n.mock=function(e){for(let t in e)d[t.toLowerCase()]=e[t]},n.setAjaxImpl=function(e){o=e},n.setCurrentTime=function(e){i=e},n.setSignature=function(e){r=e},n.addMock=function(e,t){d[e.toLowerCase()]=t},n.setToken=function(e){c=e},n.setAkSk=function(e,t){s=e,a=t},n.setServerUrl=function(e){e.trim().endsWith("/")&&(e=e.trim().substring(0,e.trim().length-1)),u=e},n.req=function(e,t,n,l,f,p){if(d.hasOwnProperty(e.toLowerCase()))return console.log("[Dew]Mock request [%s]%s",n,t),new Promise(o=>{o(d[e.toLowerCase()].call(null,n,t,l))});(f=f||{})["Dew-Token"]=c||"";let h="/exec?Dew-Resource-Action="+n+"&Dew-Resource-Uri="+encodeURIComponent(t);!function(e,t,n){if(!s||!a)return;let o=t.split("?"),c=o[0],u=2===o.length?o[1]:"";u&&(u=u.split("&").sort((e,t)=>e<t?-1:1).join("&"));let d=i();n.Authorization=s+":"+r(e+"\n"+d+"\n"+c+"\n"+u,a),n["Dew-Date"]=d}("post",h,f);let m=u+h;return console.log("[Dew]Request [%s]%s , GW = [%s]",n,t,m),new Promise((e,t)=>{o(m,f,void 0===l?"":l).then(n=>{let o=n.data;p?e(o):"200"===o.code?e(o.body):(console.error("请求错误 : ["+o.code+"]"+o.message),t("["+o.code+"]"+o.message))}).catch(e=>{console.error("服务错误 : ["+e.message+"]"+e.stack),t(e.message)})})}},{}]},{},[1])(1)}));