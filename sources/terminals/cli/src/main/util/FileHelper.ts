import path from "path";
import * as fs from "fs";

export function pwd(): string {
    return path.basename(process.cwd())
}

export function exists(filePath: string): boolean {
    return fs.existsSync(filePath)
}

export function writeFile(filePath:string,content:string):void{
    fs.writeFileSync(filePath,content,'utf8')
}

