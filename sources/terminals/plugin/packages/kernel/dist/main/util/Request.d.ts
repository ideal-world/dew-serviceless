import { Method } from "axios";
export declare function setAkSk(ak: string, sk: string): void;
export declare function setServerUrl(serverUrl: string): void;
export declare function req<T>(name: string, method: Method, pathAndQuery: string, body?: any, headers?: any): Promise<T>;
//# sourceMappingURL=Request.d.ts.map