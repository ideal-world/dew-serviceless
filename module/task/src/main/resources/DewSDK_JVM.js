"use strict";

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

(function (f) {
  if ((typeof exports === "undefined" ? "undefined" : _typeof(exports)) === "object" && typeof module !== "undefined") {
    module.exports = f();
  } else if (typeof define === "function" && define.amd) {
    define([], f);
  } else {
    var g;

    if (typeof window !== "undefined") {
      g = window;
    } else if (typeof global !== "undefined") {
      g = global;
    } else if (typeof self !== "undefined") {
      g = self;
    } else {
      g = this;
    }

    g.JVM = f();
  }
})(function () {
  var define, module, exports;
  return function () {
    function r(e, n, t) {
      function o(i, f) {
        if (!n[i]) {
          if (!e[i]) {
            var c = "function" == typeof require && require;
            if (!f && c) return c(i, !0);
            if (u) return u(i, !0);
            var a = new Error("Cannot find module '" + i + "'");
            throw a.code = "MODULE_NOT_FOUND", a;
          }

          var p = n[i] = {
            exports: {}
          };
          e[i][0].call(p.exports, function (r) {
            var n = e[i][1][r];
            return o(n || r);
          }, p, p.exports, r, e, n, t);
        }

        return n[i].exports;
      }

      for (var u = "function" == typeof require && require, i = 0; i < t.length; i++) {
        o(t[i]);
      }

      return o;
    }

    return r;
  }()({
    1: [function (require, module, exports) {
      "use strict";
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

      var __createBinding = this && this.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        Object.defineProperty(o, k2, {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        });
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });

      var __setModuleDefault = this && this.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });

      var __importStar = this && this.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) {
          if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        }

        __setModuleDefault(result, mod);

        return result;
      };

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.SDK = void 0;

      var request = __importStar(require("./util/Request"));

      var iamSDK = __importStar(require("./module/IAMSDK"));

      var cacheSDK = __importStar(require("./module/CacheSDK"));

      var httpSDK = __importStar(require("./module/HttpSDK"));

      var reldbSDK = __importStar(require("./module/RelDBSDK"));

      var taskSDK = __importStar(require("./module/TaskSDK"));

      exports.SDK = {
        init: init,
        iam: iamSDK.iamSDK,
        reldb: reldbSDK.reldbSDK(),
        cache: cacheSDK.cacheSDK(),
        http: httpSDK.httpSDK(),
        task: taskSDK.taskSDK(),
        setting: {
          // TODO jvm执行并发问题
          token: function token(_token2) {
            request.setToken(_token2);
          },
          serverUrl: function serverUrl(_serverUrl2) {
            request.setServerUrl(_serverUrl2);
          },
          ajax: function ajax(impl) {
            request.setAjaxImpl(impl);
          },
          currentTime: function currentTime(impl) {
            request.setCurrentTime(impl);
          },
          signature: function signature(impl) {
            request.setSignature(impl);
          }
        }
      };
      /**
       * 初始化SDK
       * @param serverUrl 服务网关地址
       * @param appId 当前应用Id
       */

      function init(serverUrl, appId) {
        request.setServerUrl(serverUrl);
        iamSDK.init();
        reldbSDK.init(appId);
        cacheSDK.init(appId);
        httpSDK.init(appId);
        taskSDK.init(appId); // 重新赋值一次

        exports.SDK.reldb = reldbSDK.reldbSDK();
        exports.SDK.cache = cacheSDK.cacheSDK();
        exports.SDK.http = httpSDK.httpSDK();
        exports.SDK.task = taskSDK.taskSDK();
      }
    }, {
      "./module/CacheSDK": 4,
      "./module/HttpSDK": 5,
      "./module/IAMSDK": 6,
      "./module/RelDBSDK": 7,
      "./module/TaskSDK": 8,
      "./util/Request": 9
    }],
    2: [function (require, module, exports) {
      "use strict";
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

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.AuthSubjectKind = exports.ResourceKind = exports.AccountIdentKind = exports.CommonStatus = exports.OptActionKind = void 0;
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
    }, {}],
    3: [function (require, module, exports) {
      "use strict";
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

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.DewSDK = void 0;

      var DewSDK_1 = require("./DewSDK");

      exports.DewSDK = DewSDK_1.SDK;
    }, {
      "./DewSDK": 1
    }],
    4: [function (require, module, exports) {
      "use strict";
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

      var __createBinding = this && this.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        Object.defineProperty(o, k2, {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        });
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });

      var __setModuleDefault = this && this.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });

      var __importStar = this && this.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) {
          if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        }

        __setModuleDefault(result, mod);

        return result;
      };

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.CacheSDK = exports.cacheSDK = exports.init = void 0;

      var request = __importStar(require("../util/Request"));

      var Enum_1 = require("../domain/Enum");

      var _appId = 0;

      function init(appId) {
        _appId = appId;
      }

      exports.init = init;

      function cacheSDK() {
        return new CacheSDK(_appId + ".cache.default");
      }

      exports.cacheSDK = cacheSDK;

      var CacheSDK = /*#__PURE__*/function () {
        function CacheSDK(resourceSubjectCode) {
          _classCallCheck(this, CacheSDK);

          this.resourceSubjectCode = resourceSubjectCode;
        }

        _createClass(CacheSDK, [{
          key: "exists",
          value: function exists(key) {
            checkKey(key);
            return cache("existsCache", this.resourceSubjectCode, Enum_1.OptActionKind.EXISTS, key);
          }
        }, {
          key: "hexists",
          value: function hexists(key, fieldName) {
            checkKey(key);
            return cache("hexistsCache", this.resourceSubjectCode, Enum_1.OptActionKind.EXISTS, key + "/" + fieldName);
          }
        }, {
          key: "get",
          value: function get(key) {
            checkKey(key);
            return cache("getCache", this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, key);
          }
        }, {
          key: "hgetall",
          value: function hgetall(key) {
            checkKey(key);
            return cache("hgetallCache", this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, key + '/*');
          }
        }, {
          key: "hget",
          value: function hget(key, fieldName) {
            checkKey(key);
            return cache("hgetCache", this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, key + '/' + fieldName);
          }
        }, {
          key: "del",
          value: function del(key) {
            checkKey(key);
            return cache("delCache", this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, key);
          }
        }, {
          key: "hdel",
          value: function hdel(key, fieldName) {
            checkKey(key);
            return cache("hdelCache", this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, key + '/' + fieldName);
          }
        }, {
          key: "incrby",
          value: function incrby(key, step) {
            checkKey(key);
            return cache("incrbyCache", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '?incr=true', step + '');
          }
        }, {
          key: "hincrby",
          value: function hincrby(key, fieldName, step) {
            checkKey(key);
            return cache("hincrbyCache", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '/' + fieldName + '?incr=true', step + '');
          }
        }, {
          key: "set",
          value: function set(key, value) {
            checkKey(key);
            return cache("setCache", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key, value);
          }
        }, {
          key: "hset",
          value: function hset(key, fieldName, value) {
            checkKey(key);
            return cache("hsetCache", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '/' + fieldName, value);
          }
        }, {
          key: "setex",
          value: function setex(key, value, expireSec) {
            checkKey(key);
            return cache("setexCache", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '?expire=' + expireSec, value);
          }
        }, {
          key: "expire",
          value: function expire(key, expireSec) {
            checkKey(key);
            return cache("expireCache", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, key + '?expire=' + expireSec);
          }
        }, {
          key: "subject",
          value: function subject(resourceSubject) {
            return new CacheSDK(resourceSubject);
          }
        }]);

        return CacheSDK;
      }();

      exports.CacheSDK = CacheSDK;

      function checkKey(key) {
        if (key.indexOf('/') !== -1) {
          throw 'key不能包含[/]';
        }
      }

      function cache(name, resourceSubjectCode, optActionKind, pathAndQuery, body) {
        return request.req(name, 'cache://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body);
      }
    }, {
      "../domain/Enum": 2,
      "../util/Request": 9
    }],
    5: [function (require, module, exports) {
      "use strict";
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

      var __createBinding = this && this.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        Object.defineProperty(o, k2, {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        });
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });

      var __setModuleDefault = this && this.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });

      var __importStar = this && this.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) {
          if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        }

        __setModuleDefault(result, mod);

        return result;
      };

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.HttpSDK = exports.httpSDK = exports.init = void 0;

      var request = __importStar(require("../util/Request"));

      var Enum_1 = require("../domain/Enum");

      var _appId = 0;

      function init(appId) {
        _appId = appId;
      }

      exports.init = init;

      function httpSDK() {
        return new HttpSDK(_appId + ".http.default");
      }

      exports.httpSDK = httpSDK;

      var HttpSDK = /*#__PURE__*/function () {
        function HttpSDK(resourceSubjectCode) {
          _classCallCheck(this, HttpSDK);

          this.resourceSubjectCode = resourceSubjectCode;
        }

        _createClass(HttpSDK, [{
          key: "get",
          value: function get(pathAndQuery, header) {
            return http(this.resourceSubjectCode, Enum_1.OptActionKind.FETCH, pathAndQuery, header);
          }
        }, {
          key: "delete",
          value: function _delete(pathAndQuery, header) {
            return http(this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, pathAndQuery, header);
          }
        }, {
          key: "post",
          value: function post(pathAndQuery, body, header) {
            return http(this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, pathAndQuery, header, body);
          }
        }, {
          key: "put",
          value: function put(pathAndQuery, body, header) {
            return http(this.resourceSubjectCode, Enum_1.OptActionKind.MODIFY, pathAndQuery, header, body);
          }
        }, {
          key: "patch",
          value: function patch(pathAndQuery, body, header) {
            return http(this.resourceSubjectCode, Enum_1.OptActionKind.PATCH, pathAndQuery, header, body);
          }
        }, {
          key: "subject",
          value: function subject(resourceSubject) {
            return new HttpSDK(resourceSubject);
          }
        }]);

        return HttpSDK;
      }();

      exports.HttpSDK = HttpSDK;

      function http(resourceSubjectCode, optActionKind, pathAndQuery, header, body) {
        if (!pathAndQuery.startsWith('/')) {
          pathAndQuery = '/' + pathAndQuery;
        }

        if (!header) {
          header = {};
        }

        header["Content-Type"] = "application/json charset=utf-8";
        return request.req('http', 'http://' + resourceSubjectCode + pathAndQuery, optActionKind, body, header, true);
      }
    }, {
      "../domain/Enum": 2,
      "../util/Request": 9
    }],
    6: [function (require, module, exports) {
      "use strict";
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

      var __createBinding = this && this.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        Object.defineProperty(o, k2, {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        });
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });

      var __setModuleDefault = this && this.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });

      var __importStar = this && this.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) {
          if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        }

        __setModuleDefault(result, mod);

        return result;
      };

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.iamSDK = exports.init = void 0;

      var request = __importStar(require("../util/Request"));

      var Enum_1 = require("../domain/Enum");

      var iamModuleName = 'iam';

      var _identOptInfo;

      function init() {}

      exports.init = init;
      var account = {
        login: function login(userName, password, appId) {
          request.setToken('');
          return request.req('login', 'http://' + iamModuleName + '/common/login', Enum_1.OptActionKind.CREATE, {
            ak: userName,
            sk: password,
            relAppId: appId
          }).then(function (identOptInfo) {
            request.setToken(identOptInfo.token);
            _identOptInfo = identOptInfo;
            return identOptInfo;
          });
        },
        logout: function logout() {
          return request.req('logout', 'http://' + iamModuleName + '/common/logout', Enum_1.OptActionKind.DELETE).then(function () {
            request.setToken('');
          });
        },
        fetchLoginInfo: function fetchLoginInfo() {
          return _identOptInfo;
        },
        registerAccount: function registerAccount(accountName) {
          return request.req('registerAccount', 'http://' + iamModuleName + '/console/tenant/account', Enum_1.OptActionKind.CREATE, {
            name: accountName
          });
        },
        createAccountIdent: function createAccountIdent(accountId, identKind, ak, sk) {
          return request.req('addAccountIdent', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/ident', Enum_1.OptActionKind.CREATE, {
            kind: identKind,
            ak: ak,
            sk: sk
          });
        },
        bindApp: function bindApp(accountId, appId) {
          return request.req('bindApp', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/app/' + appId, Enum_1.OptActionKind.CREATE);
        },
        bindRole: function bindRole(accountId, roleId) {
          return request.req('bindRole', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/role/' + roleId, Enum_1.OptActionKind.CREATE);
        }
      };
      var resource = {
        fetchMenu: function fetchMenu() {
          return request.req('fetchMenu', 'menu://' + iamModuleName + '/common/resource?kind=menu', Enum_1.OptActionKind.FETCH);
        },
        fetchEle: function fetchEle() {
          return request.req('fetchEle', 'element://' + iamModuleName + '/common/resource?kind=element', Enum_1.OptActionKind.FETCH);
        },
        createResourceSubject: function createResourceSubject(code, kind, name, uri, ak, sk) {
          return request.req('createResourceSubject', 'http://' + iamModuleName + '/console/app/resource/subject', Enum_1.OptActionKind.CREATE, {
            codePostfix: code,
            kind: kind,
            name: name,
            uri: uri,
            ak: ak,
            sk: sk
          });
        },
        createResource: function createResource(name, pathAndQuery, resourceSubjectId) {
          return request.req('createResource', 'http://' + iamModuleName + '/console/app/resource', Enum_1.OptActionKind.CREATE, {
            name: name,
            pathAndQuery: pathAndQuery,
            relResourceSubjectId: resourceSubjectId
          });
        }
      };
      var authpolicy = {
        createAuthPolicy: function createAuthPolicy(subjectKind, subjectId, resourceId) {
          return request.req('createAuthPolicy', 'http://' + iamModuleName + '/console/app/authpolicy', Enum_1.OptActionKind.CREATE, {
            relSubjectKind: subjectKind,
            relSubjectIds: subjectId,
            subjectOperator: "EQ",
            relResourceId: resourceId,
            resultKind: "ACCEPT"
          });
        }
      };
      var tenant = {
        registerTenant: function registerTenant(tenantName, appName, tenantAdminUsername, tenantAdminPassword) {
          return request.req('createTenant', 'http://' + iamModuleName + '/common/tenant', Enum_1.OptActionKind.CREATE, {
            tenantName: tenantName,
            appName: appName,
            accountUserName: tenantAdminUsername,
            accountPassword: tenantAdminPassword
          });
        },
        fetchTenant: function fetchTenant() {
          return request.req('fetchTenant', 'http://' + iamModuleName + '/console/tenant/tenant', Enum_1.OptActionKind.FETCH);
        }
      };
      var app = {
        createApp: function createApp(appName) {
          return request.req('createApp', 'http://' + iamModuleName + '/console/tenant/app', Enum_1.OptActionKind.CREATE, {
            name: appName
          });
        },
        fetchPublicKey: function fetchPublicKey() {
          return request.req('fetchPublicKey', 'http://' + iamModuleName + '/console/app/app/publicKey', Enum_1.OptActionKind.FETCH);
        },
        listAppIdents: function listAppIdents() {
          return request.req('listAppIdents', 'http://' + iamModuleName + '/console/app/app/ident', Enum_1.OptActionKind.FETCH);
        },
        fetchAppIdentSk: function fetchAppIdentSk(identId) {
          return request.req('fetchAppIdentSk', 'http://' + iamModuleName + '/console/app/app/ident/' + identId + '/sk', Enum_1.OptActionKind.FETCH);
        }
      };
      var role = {
        createRoleDef: function createRoleDef(roleDefCode, roleDefName) {
          return request.req('createRoleDef', 'http://' + iamModuleName + '/console/app/role/def', Enum_1.OptActionKind.CREATE, {
            code: roleDefCode,
            name: roleDefName
          });
        },
        createRole: function createRole(relRoleDefId) {
          return request.req('createRole', 'http://' + iamModuleName + '/console/app/role', Enum_1.OptActionKind.CREATE, {
            relRoleDefId: relRoleDefId
          });
        }
      };
      exports.iamSDK = {
        account: {
          login: account.login,
          logout: account.logout,
          register: account.registerAccount,
          bindApp: account.bindApp,
          bindRole: account.bindRole,
          ident: {
            create: account.createAccountIdent
          },
          fetchLoginInfo: account.fetchLoginInfo
        },
        resource: {
          create: resource.createResource,
          subject: {
            create: resource.createResourceSubject
          },
          menu: {
            fetch: resource.fetchMenu
          },
          ele: {
            fetch: resource.fetchEle
          }
        },
        tenant: {
          register: tenant.registerTenant,
          fetch: tenant.fetchTenant
        },
        app: {
          create: app.createApp,
          ident: {
            list: app.listAppIdents,
            fetchSk: app.fetchAppIdentSk
          },
          key: {
            fetchPublicKey: app.fetchPublicKey
          }
        },
        role: {
          createRoleDef: role.createRoleDef,
          createRole: role.createRole
        },
        authpolicy: {
          create: authpolicy.createAuthPolicy
        }
      };
    }, {
      "../domain/Enum": 2,
      "../util/Request": 9
    }],
    7: [function (require, module, exports) {
      "use strict";
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

      var __createBinding = this && this.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        Object.defineProperty(o, k2, {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        });
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });

      var __setModuleDefault = this && this.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });

      var __importStar = this && this.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) {
          if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        }

        __setModuleDefault(result, mod);

        return result;
      };

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.RelDBSDK = exports.reldbSDK = exports.init = void 0;

      var request = __importStar(require("../util/Request"));

      var Enum_1 = require("../domain/Enum");

      var _appId = 0;

      function init(appId) {
        _appId = appId;
      }

      exports.init = init;

      function reldbSDK() {
        return new RelDBSDK(_appId + ".reldb.default");
      }

      exports.reldbSDK = reldbSDK;

      var RelDBSDK = /*#__PURE__*/function () {
        function RelDBSDK(resourceSubjectCode) {
          _classCallCheck(this, RelDBSDK);

          this.resourceSubjectCode = resourceSubjectCode;
        }

        _createClass(RelDBSDK, [{
          key: "exec",
          value: function exec(encryptedSql, parameters) {
            return doExec(this.resourceSubjectCode, encryptedSql, parameters);
          }
        }, {
          key: "subject",
          value: function subject(resourceSubject) {
            return new RelDBSDK(resourceSubject);
          }
        }]);

        return RelDBSDK;
      }();

      exports.RelDBSDK = RelDBSDK;

      function doExec(resourceSubjectCode, encryptedSql, parameters) {
        var item = encryptedSql.split('|');

        if (item.length !== 2) {
          throw "该SQL语句没有加密 : " + encryptedSql;
        }

        return request.req('reldb', 'reldb://' + resourceSubjectCode, Enum_1.OptActionKind[item[0]], {
          sql: item[1],
          parameters: parameters
        });
      }
    }, {
      "../domain/Enum": 2,
      "../util/Request": 9
    }],
    8: [function (require, module, exports) {
      "use strict";
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

      var __createBinding = this && this.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        Object.defineProperty(o, k2, {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        });
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });

      var __setModuleDefault = this && this.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });

      var __importStar = this && this.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) {
          if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        }

        __setModuleDefault(result, mod);

        return result;
      };

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.TaskSDK = exports.taskSDK = exports.init = void 0;

      var request = __importStar(require("../util/Request"));

      var Enum_1 = require("../domain/Enum");

      var _appId = 0;

      function init(appId) {
        _appId = appId;
      }

      exports.init = init;

      function taskSDK() {
        return new TaskSDK(_appId + ".task.default");
      }

      exports.taskSDK = taskSDK;

      var TaskSDK = /*#__PURE__*/function () {
        function TaskSDK(resourceSubjectCode) {
          _classCallCheck(this, TaskSDK);

          this.resourceSubjectCode = resourceSubjectCode;
        }

        _createClass(TaskSDK, [{
          key: "create",
          value: function create(taskCode, fun, cron) {
            var query = '';

            if (cron) {
              query = '?cron=' + encodeURIComponent(cron);
            }

            return task("createTask", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, 'task/' + taskCode + query, fun);
          }
        }, {
          key: "modify",
          value: function modify(taskCode, fun, cron) {
            var query = '';

            if (cron) {
              query = '?cron=' + encodeURIComponent(cron);
            }

            return task("modifyTask", this.resourceSubjectCode, Enum_1.OptActionKind.MODIFY, 'task/' + taskCode + query, fun);
          }
        }, {
          key: "delete",
          value: function _delete(taskCode) {
            return task("deleteTask", this.resourceSubjectCode, Enum_1.OptActionKind.DELETE, 'task/' + taskCode);
          }
        }, {
          key: "execute",
          value: function execute(taskCode) {
            return task("executeTask", this.resourceSubjectCode, Enum_1.OptActionKind.CREATE, 'exec/' + taskCode);
          }
        }, {
          key: "subject",
          value: function subject(resourceSubject) {
            return new TaskSDK(resourceSubject);
          }
        }]);

        return TaskSDK;
      }();

      exports.TaskSDK = TaskSDK;

      function task(name, resourceSubjectCode, optActionKind, pathAndQuery, body) {
        return request.req(name, 'task://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body);
      }
    }, {
      "../domain/Enum": 2,
      "../util/Request": 9
    }],
    9: [function (require, module, exports) {
      "use strict";
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

      Object.defineProperty(exports, "__esModule", {
        value: true
      });
      exports.req = exports.setServerUrl = exports.setAkSk = exports.setToken = exports.addMock = exports.setSignature = exports.setCurrentTime = exports.setAjaxImpl = exports.mock = void 0;
      var TOKEN_FLAG = 'Dew-Token';
      var REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri";
      var REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action";
      var AUTHENTICATION_HEAD_NAME = 'Authentication';
      var DATE_HEAD_NAME = 'Dew-Date';
      var _token = '';
      var _serverUrl = '';
      var _ak = '';
      var _sk = '';
      var ajax;
      var currentTime;
      var signature;
      var MOCK_DATA = {};

      function mock(items) {
        for (var k in items) {
          MOCK_DATA[k.toLowerCase()] = items[k];
        }
      }

      exports.mock = mock;

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

      function addMock(code, fun) {
        MOCK_DATA[code.toLowerCase()] = fun;
      }

      exports.addMock = addMock;

      function setToken(token) {
        _token = token;
      }

      exports.setToken = setToken;

      function setAkSk(ak, sk) {
        _ak = ak;
        _sk = sk;
      }

      exports.setAkSk = setAkSk;

      function setServerUrl(serverUrl) {
        if (serverUrl.trim().endsWith("/")) {
          serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1);
        }

        _serverUrl = serverUrl + '/exec';
      }

      exports.setServerUrl = setServerUrl;

      function req(name, resourceUri, optActionKind, body, headers, rawResult) {
        if (MOCK_DATA.hasOwnProperty(name.toLowerCase())) {
          console.log('[Dew]Mock request [%s]%s', optActionKind, resourceUri);
          return new Promise(function (resolve) {
            resolve(MOCK_DATA[name.toLowerCase()].call(null, optActionKind, resourceUri, body));
          });
        }

        headers = headers ? headers : {};
        headers[TOKEN_FLAG] = _token ? _token : '';
        var pathAndQuery = REQUEST_RESOURCE_URI_FLAG + '=' + encodeURIComponent(resourceUri) + '&' + REQUEST_RESOURCE_ACTION_FLAG + '=' + optActionKind;
        generateAuthentication('post', pathAndQuery, headers);
        console.log('[Dew]Request [%s]%s , GW = [%s]', optActionKind, resourceUri, _serverUrl);
        return new Promise(function (resolve, reject) {
          ajax(_serverUrl + '?' + pathAndQuery, headers, typeof body === "undefined"?"":body).then(function (res) {
            var data = res.data;

            if (rawResult) {
              resolve(data);
            } else {
              if (data.code === '200') {
                resolve(data.body);
              } else {
                console.error('请求错误 : [' + data.code + ']' + data.message);
                reject('[' + data.code + ']' + data.message);
              }
            }
          })["catch"](function (error) {
            console.error('请求错误 : [' + error.message + ']' + error.stack);
            reject(error);
          });
        });
      }

      exports.req = req;

      function generateAuthentication(method, pathAndQuery, headers) {
        if (!_ak || !_sk) {
          return;
        }

        var item = pathAndQuery.split('?');
        var path = item[0];
        var query = item.length === 2 ? item[1] : '';

        if (query) {
          query = query.split('&').sort(function (a, b) {
            return a < b ? 1 : -1;
          }).join("&");
        }

        var date = currentTime();
        headers[AUTHENTICATION_HEAD_NAME] = _ak + ':' + signature(method + '\n' + date + '\n' + path + '\n' + query, _sk);
        headers[DATE_HEAD_NAME] = date;
      }
    }, {}]
  }, {}, [3])(3);
});
