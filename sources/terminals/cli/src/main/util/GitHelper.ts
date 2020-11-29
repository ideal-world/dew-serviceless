import gitP, {SimpleGit} from 'simple-git/promise';
import * as fileHelper from './FileHelper';
import {Response} from "simple-git/typings/simple-git";

let git: SimpleGit = gitP()

export function clone(repoPath: string, localPath: string, depth?: number): Response<string> {
    if (fileHelper.exists(localPath)) {
        throw 'The path [' + localPath + '] already exists.'
    } else {
        let option = {}
        if (depth) {
            option['--depth'] = depth
        }
        return git.clone(repoPath, localPath, option)
    }

}

