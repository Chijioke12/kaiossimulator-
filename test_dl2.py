import urllib.request
import json
import time
import uuid
import base64
import hmac
import hashlib
from urllib.parse import urlparse

# 1. Get token
token_url = "https://api.kaiostech.com/v3.0/applications/CAlTn_6yQsgyJKrr-nCh/tokens"
headers = {
    "Authorization": "Key baJ_nea27HqSskijhZlT",
    "Content-Type": "application/json"
}
payload = {
    "brand": "AlcatelOneTouch",
    "device_id": "123456789012345",
    "device_type": 999999,
    "model": "GoFlip2",
    "os": "KaiOS",
    "os_version": "2.5.4",
    "reference": "4044O-2BAQUS1-R"
}

req = urllib.request.Request(token_url, data=json.dumps(payload).encode('utf-8'), headers=headers, method="POST")
with urllib.request.urlopen(req) as resp:
    token_data = json.loads(resp.read().decode('utf-8'))

kid = token_data.get('kid')
mac_key_b64 = token_data.get('mac_key')
mac_key = base64.b64decode(mac_key_b64)

dl_url = "https://storage.kaiostech.com/v3.0/files/zip/1/Nnzv1ZjoMfrvODT9kc46B4S0syu2ZnTcqg4vtk/8.55.2_APP_ZIP_FILE.zip"

parsed_url = urlparse(dl_url)
ts = str(int(time.time()))
nonce = str(uuid.uuid4())[:6]
method = "GET"
path = parsed_url.path
host = parsed_url.hostname
port = "443"

normalized = f"hawk.1.header\n{ts}\n{nonce}\n{method}\n{path}\n{host}\n{port}\n\n\n"

mac = hmac.new(mac_key, normalized.encode('utf-8'), hashlib.sha256).digest()
mac_str = base64.b64encode(mac).decode('utf-8')

auth_header = f'Hawk id="{kid}", ts="{ts}", nonce="{nonce}", mac="{mac_str}"'

app_headers = {
    "Kai-API-Version": "3.0",
    "Kai-Request-Info": f'ct="wifi", rt="auto", utc="{int(time.time()*1000)}", utc_off="1", mcc="0", mnc="0", net_mcc="null", net_mnc="null"',
    "Kai-Device-Info": 'imei="123456789012345", curef="4044O-2BAQUS1-R"',
    "User-agent": "Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4",
    "Authorization": auth_header
}

req2 = urllib.request.Request(dl_url, headers=app_headers, method="GET")
try:
    with urllib.request.urlopen(req2) as resp2:
        print("Status:", resp2.status)
        print("Size:", len(resp2.read()))
except Exception as e:
    print(e)
