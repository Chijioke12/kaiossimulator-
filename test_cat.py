import urllib.request
import gzip
try:
    req = urllib.request.Request("https://api.kaiostech.com/v3.0/categories", headers={"Accept-Encoding": "gzip"})
    with urllib.request.urlopen(req) as resp:
        print(gzip.decompress(resp.read()).decode('utf-8'))
except Exception as e:
    print(e)
