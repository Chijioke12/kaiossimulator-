const crypto = require('crypto');
const https = require('https');

async function doHawk() {
    const fetchToken = () => new Promise((resolve, reject) => {
        const data = JSON.stringify({
          brand: "AlcatelOneTouch",
          device_id: "123456789012345",
          device_type: 999999,
          model: "GoFlip2",
          os: "KaiOS",
          os_version: "2.5.4",
          reference: "4044O-2BAQUS1-R"
        });
        
        const req = https.request('https://api.kaiostech.com/v3.0/applications/CAlTn_6yQsgyJKrr-nCh/tokens', {
            method: 'POST',
            headers: {
                'Authorization': 'Key baJ_nea27HqSskijhZlT',
                'Content-Type': 'application/json'
            }
        }, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => resolve(JSON.parse(body)));
        });
        req.write(data);
        req.end();
    });

    const token = await fetchToken();
    const kid = token.kid;
    const macKey = Buffer.from(token.mac_key, 'base64');
    
    const targetUrl = "https://api.kaiostech.com/kc_ksfe/v1.0/apps?bookmark=false&imei=123456789012345&os=2.5.4&page_size=50&page_num=1&mnc=0&mcc=0&category=10";
    const parsedUrl = new URL(targetUrl);
    
    const ts = Math.floor(Date.now() / 1000).toString();
    const nonce = crypto.randomUUID().substring(0, 6);
    const path = parsedUrl.pathname + parsedUrl.search;
    const host = parsedUrl.hostname;
    const port = "443";
    
    const normalized = `hawk.1.header\n${ts}\n${nonce}\nGET\n${path}\n${host}\n${port}\n\n\n`;
    const hmac = crypto.createHmac('sha256', macKey);
    hmac.update(normalized);
    const macValue = hmac.digest('base64');
    
    const authHeader = `Hawk id="${kid}", ts="${ts}", nonce="${nonce}", mac="${macValue}"`;
    
    const req = https.request(targetUrl, {
        method: 'GET',
        headers: {
            'Kai-API-Version': '3.0',
            'Kai-Request-Info': `ct="wifi", rt="auto", utc="${Date.now()}", utc_off="1", mcc="0", mnc="0", net_mcc="null", net_mnc="null"`,
            'Kai-Device-Info': 'imei="123456789012345", curef="4044O-2BAQUS1-R"',
            'User-agent': 'Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4',
            'Authorization': authHeader,
            'Accept-Encoding': 'gzip, deflate, br'
        }
    }, (res) => {
        console.log("STATUS:", res.statusCode);
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', () => console.log("Done", body.length));
    });
    req.end();
}
doHawk();
