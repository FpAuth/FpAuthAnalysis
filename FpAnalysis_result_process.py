import sys
import os
import json
import csv

def load_json(fp):
    cc = open(fp, 'r').read()
    res = json.loads(cc.strip(), strict=False)
    return res


def get_crypto_check_result():
    dataset = 'gp'
    pkglist = [_.strip() for _ in open(f'./dataset/{dataset}_FpAuth_app.txt', 'r').readlines()]
    writer = csv.writer(open(f'./res/{dataset}_FpAuth_app_crypto_check.csv', 'w', newline=''))
    writer.writerow(['pkg', 'crypto use', 'setUserAuthenticationRequired', 'setInvalidatedByBiometricEnrollment', 'crypto validation'])

    huaweidir = f'./res/{dataset}/'
    cnt = 0
    total = len(pkglist)
    for pkg in pkglist:
        cnt += 1
        print(cnt, '/', total, ":", pkg)
        jsonPath = os.path.join(huaweidir, pkg + '.json')

        if not os.path.exists(jsonPath):
            writer.writerow([pkg, 'fail', 'fail', 'fail', 'fail'])
            return

        res = load_json(jsonPath)

        config = []     
        pkg = res['meta']['pname']
        crypto_use = "1"
        req = "true"
        inv = "true"
        crypto_val = "1"
        features = res['features']
        fnames = [f['name'] for f in features]
        res = ""

        if "Keybuilder" not in fnames:
            crypto_use = "0"
        elif all(["Exotic" in f['result'] for f in features if (f['name'] == 'Keybuilder')]):
            crypto_use = "0"

        if "Authenticate" not in fnames:
            crypto_use = "NO_AUTHENTICATE"
        if any(["Weak" in f['result'] for f in features if (f['name'] == 'Authenticate' and not f['location'].startswith("androidx"))]):
            crypto_use = "0"
        elif all([f['location'].startswith("androidx") for f in features if f['name'] == 'Authenticate']):
            crypto_use = "0"
        
        if "AuthenticationRequired" not in fnames:
            req = "ns"
        elif any([f['value'] == "0" for f in features if f['name'] == 'AuthenticationRequired']):
            req = "false"
        elif all([f['value'].startswith('$') for f in features if f['name'] == 'AuthenticationRequired']):
            req = "$"
        
        if "InvalidatedByBiometricEnrollment" not in fnames:
            inv = "ns"
        elif any([f['value'] == "0" for f in features if f['name'] == 'InvalidatedByBiometricEnrollment']):
            inv = "false"
        elif all([(f['value'].startswith('$') or f['value'].startswith('r')) for f in features if f['name'] == 'InvalidatedByBiometricEnrollment']):
            inv = "$"

        # crypto validation
        if "Succeeded" not in fnames:
            crypto_val = "NO_SUCCEEDED"
        elif any([(f['result'] == "Weak" or f['result'] == "Unknown") for f in features if f['name'] == 'Succeeded']):
            crypto_val = "0"

        config = [pkg, crypto_use, req, inv, crypto_val]
        writer.writerow(config)


def get_deactivation_result():
    dataset = 'gp'
    pkglist = [_.strip() for _ in open(f'/dataset/{dataset}_FpAuth_app.txt', 'r').readlines()]
    writer = csv.writer(open(f'./res/{dataset}_FpAuth_app_disable.csv', 'w', newline=''))
    writer.writerow(['pkg', 'result'])

    huaweidir = f'./res/{dataset}/'
    cnt = 0
    total = len(pkglist)
    for pkg in pkglist:
        cnt += 1
        print(cnt, '/', total, ":", pkg)
        jsonPath = os.path.join(huaweidir, pkg + '.json')

        if not os.path.exists(jsonPath):
            writer.writerow([pkg, 'fail'])
            return

        res = load_json(jsonPath)
        config = []     
        pkg = res['meta']['pname']
        result = "0"
        features = res['features']
        fnames = [f['name'] for f in features]
        res = ""

        if "Authenticate_bg_dfs" not in fnames:
            result = "fail"
        
        if any(["true" in f['result'] for f in features if (f['name'] == 'Authenticate_bg_dfs')]):
            result = "1"

        config = [pkg, result]
        writer.writerow(config)


def get_delete_result():
    dataset = 'hw'
    pkglist = [_.strip() for _ in open(f'/dataset/{dataset}_FpAuth_app.txt', 'r').readlines()]
    writer = csv.writer(open(f'./res/{dataset}_FpAuth_app_delete.csv', 'w', newline=''))
    writer.writerow(['pkg', 'result'])

    huaweidir = f'./res/{dataset}/'
    cnt = 0
    total = len(pkglist)
    for pkg in pkglist:
        cnt += 1
        print(cnt, '/', total, ":", pkg)
        jsonPath = os.path.join(huaweidir, pkg + '.json')

        if not os.path.exists(jsonPath):
            writer.writerow([pkg, 'fail'])
            return

        res = load_json(jsonPath)
        config = []     
        pkg = res['meta']['pname']
        result = "1"
        features = res['features']
        fnames = [f['name'] for f in features]
        res = ""

        if "Unlock" not in fnames:
            result = "no API usage"
            print(pkg)
        
        if any(["WEAK" in f['result'] for f in features if (f['name'] == 'Unlock')]):
            result = "0"

        config = [pkg, result]
        writer.writerow(config)


def get_biometric_only_result():
    dataset = 'hw'
    pkglist = [_.strip() for _ in open(f'/dataset/{dataset}_FpAuth_app.txt', 'r').readlines()]
    writer = csv.writer(open(f'./res/{dataset}_FpAuth_app_bioOnly.csv', 'w', newline=''))
    writer.writerow(['pkg', 'android:setNegativeButtonText', 'android:setDeviceCredentialAllowed', 'android:setAllowedAuthenticators', 
                     'androidx:setNegativeButtonText', 'androidx:setDeviceCredentialAllowed', 'androidx:setAllowedAuthenticators', 'result_android', 'result_androidx', 'result_final'])

    huaweidir = f'./res/{dataset}/'
    cnt = 0
    total = len(pkglist)
    for pkg in pkglist:
        cnt += 1
        print(cnt, '/', total, ":", pkg)
        jsonPath = os.path.join(huaweidir, pkg + '.json')

        if not os.path.exists(jsonPath):
            writer.writerow([pkg, 'fail'])
            return

        res = load_json(jsonPath)
        config = []     
        btn = device = auth = btnx = devicex = authx = ""
        pkg = res['meta']['pname']
        result = resultx = ""
        features = res['features']
        fnames = [f['name'] for f in features]
        res = ""

        if "BiometricOnly" not in fnames:
            result = resultx = "null"
        
        for f in features:
            if "setNegativeButton" in f['slice'] and f['slice'].startswith("android."):
                btn = f['value']
            if "setDeviceCredentialAllowed" in f['slice'] and f['slice'].startswith("android."):
                device = f['value']
                if device == "1":
                    result = "BIO-PIN"
            if "setAllowedAuthenticators" in f['slice'] and f['slice'].startswith("android."):
                auth = f['value'] + '/' + f['result']
                if "BIO-PIN" in auth:
                    result = "BIO-PIN"

            if "setNegativeButton" in f['slice'] and f['slice'].startswith("androidx."):
                btnx = f['value']
            if "setDeviceCredentialAllowed" in f['slice'] and f['slice'].startswith("androidx."):
                devicex = f['value']
                if devicex == "1":
                    resultx = "BIO-PIN"
            if "setAllowedAuthenticators" in f['slice'] and f['slice'].startswith("androidx."):
                authx = f['value'] + '/' + f['result']
                if "BIO-PIN" in authx:
                    resultx = "BIO-PIN"

        if result == "":
            result = "BIO-ONLY"
        if resultx == "":
            resultx = "BIO-ONLY"
        
        res_final = "BIO-ONLY"
        if result == "BIO-PIN" or resultx == "BIO-PIN":
            res_final = "BIO-PIN"

        config = [pkg, btn, device, auth, btnx, devicex, authx, result, resultx, res_final]
        writer.writerow(config)


if __name__ == "__main__":

    get_crypto_check_result()
    get_deactivation_result()
    get_delete_result()
    get_biometric_only_result()     # PIN-Alternative