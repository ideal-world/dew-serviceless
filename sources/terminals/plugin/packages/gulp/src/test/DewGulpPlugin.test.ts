import File from 'vinyl'

const plugin = require('../main/DewGulpPlugin');

test('Test gulp plugin', () => {
    let fakeFile = new File({
        contents: Buffer.from(`
        let sqlJson = {
        s1: 'insert into main(name, age) values (?, ?)'
    }
    sqlJson[s2] = 'update main set name = ? where id = ?'
    sqlJson[s3] = {}
    sqlJson[s3][s33] = 'update main set name = ? where id = ?'
    Hi.sql(sqlJson.s1,10,'20')
    Hi.sql(sqlJson.s2,10,'20')
    `)
    })
    let pStream = plugin()
    pStream.write(fakeFile)
    pStream.once('data', function (file) {
        expect(file.isBuffer()).toBe(true)
        expect(file.contents.toString('utf8')).not.toMatch('insert')
    });
})
