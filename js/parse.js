const fpFolder = './fp/';
const fs = require('fs');

const files = fs.readdirSync(fpFolder)
const outfile = './fp.txt'
out = ''
files.forEach(file => {
    const map = JSON.parse(fs.readFileSync(fpFolder + file, { encoding: 'ascii' }))
    out += [map['FINGERPRINT'], map['PRODUCT'], map['DEVICE'], map['MANUFACTURER'],
           map['BRAND'], map['MODEL']].join('\t')
    out += '\n'
})
fs.appendFile(outfile,
    out,
    { encoding: 'ascii' }, test => {}
)
