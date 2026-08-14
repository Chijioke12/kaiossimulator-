import urllib.request
url = "https://storage.kaiostech.com/v3.0/files/zip/1/Nnzv1ZjoMfrvODT9kc46B4S0syu2ZnTcqg4vtk/8.55.2_APP_ZIP_FILE.zip"
try:
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as resp:
        print("Status:", resp.status)
        print("Size:", len(resp.read()))
except Exception as e:
    print(e)
