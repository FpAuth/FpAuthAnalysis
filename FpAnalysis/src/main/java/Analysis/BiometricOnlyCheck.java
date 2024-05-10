package Analysis;

import comm.SootConfig;
import fcm.layout.ResourceUtil;
import javafx.util.Pair;
import org.xmlpull.v1.XmlPullParserException;
import cg.Common;
import soot.*;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot_analysis.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static base.Aapt.aaptResult;
import static soot_analysis.SootAnalysis.*;
import static soot_analysis.SootAnalysis.getInvokeParameter_resolve;
import static soot_analysis.Utils.*;

public class BiometricOnlyCheck {
    public String apkPath;
    public String resPath;
    public String pkg;
    int DEVICE_CREDENTIAL = 32768;

    public BiometricOnlyCheck(String apkpath, String respath) throws XmlPullParserException, IOException {
        this.apkPath = apkpath;
        this.resPath = respath;
        this.pkg = getPkgname(apkpath);
    }

    public String getPkgname(String apkpath) throws XmlPullParserException, IOException {
        try {
            ProcessManifest processMan = new ProcessManifest(apkpath);
            return processMan.getPackageName();
        } catch (Exception e) {
            System.out.println(e.toString());
            return apkpath.substring(apkpath.lastIndexOf('/') + 1, apkpath.length() - 4);
        }
    }

    public static void main(String[] args) throws XmlPullParserException, IOException, ParseException {
        String apkpath = args[0];
        String respath = args[1];
        BiometricOnlyCheck biometricOnlyCheck = new BiometricOnlyCheck(apkpath, respath);

        boolean write_map = Boolean.parseBoolean(args[2]);
        SootConfig.init(biometricOnlyCheck.apkPath, "jimple");
        Common.init(write_map);
        ResourceUtil.init(biometricOnlyCheck.apkPath);
        biometricOnlyCheck.run();
    }

    public void run() throws IOException, ParseException {
        Features features = new Features();
        getMeta(features);
        SootContext sc = new SootContext(Scene.v());
        analyzeBiometricOnly(features, sc);
        AnalysisUtils.writeJsonToFile(features.toJson().replace("\\n", "\n"), resPath);
    }

    public void analyzeBiometricOnly(Features features, SootContext SC) {
        Collection<Pair<String, CodeLocation>> tusages = getUsages(SC);
        Collection<Pair<String, CodeLocation>> usages_filtered = filterUsages(tusages);
        analyzeUsages(features, SC, usages_filtered);
    }

    private void analyzeUsages(Features features, SootContext SC, Collection<Pair<String, CodeLocation>> usages_filtered) {
        for (Pair<String, CodeLocation> tuple : usages_filtered) {
            CodeLocation cl = tuple.getValue();
            String api = tuple.getKey();
            if (cl.smethod.getDeclaringClass().getName().startsWith("androidx")) {
                continue;
            }
            analyzeUsage(features, SC, cl, api);
        }
    }

    private void analyzeUsage(Features features, SootContext SC, CodeLocation cl, String api) {
        if(api.contains("setAllowedAuthenticators")) {
            analyzeSetAllowedAuthenticators(features, SC, cl, api);
        }
        else if (api.contains("setDeviceCredentialAllowed")) {
            analyzeSetDeviceCredentialAllowed(features, SC, cl, api);
        }
        else if (api.contains("setNegativeButton")) {
            features.add("BiometricOnly", null, cl, "BIO-ONLY", api, SC.getInvokeExpr(cl.sunit));
        }
    }

    private void analyzeSetDeviceCredentialAllowed(Features features, SootContext SC, CodeLocation cl, String api) {
        Value vv = getInvokeParameter_resolve(SC, cl.sunit, 0, cl);
        if (vv != null && vv.toString().equals("1")) {
            features.add("BiometricOnly", vv, cl, "BIO-PIN", api, SC.getInvokeExpr(cl.sunit));
        } else {
            features.add("BiometricOnly", vv, cl, "BIO-ONLY", api, SC.getInvokeExpr(cl.sunit));
        }
    }

    private void analyzeSetAllowedAuthenticators(Features features, SootContext SC, CodeLocation cl, String api) {
        Value vv = getInvokeParameter_resolve_int(SC, cl.sunit, 0, cl);
        if (vv != null && !vv.toString().contains("$") && !vv.toString().contains("r") && (Integer.parseInt(vv.toString()) & this.DEVICE_CREDENTIAL) != 0) {
            features.add("BiometricOnly", vv, cl, "BIO-PIN", api, SC.getInvokeExpr(cl.sunit));
        } else {
            features.add("BiometricOnly", vv, cl, "BIO-ONLY", api, SC.getInvokeExpr(cl.sunit));
        }
    }

    private Collection<Pair<String, CodeLocation>> getUsages(SootContext SC) {
        Collection<Pair<String, CodeLocation>> tusages = new LinkedList<>();
        Collection<Pair<String, String>> cls_apis = getClsApis();

        for (Pair<String, String> cls_api : cls_apis) {
            String className = cls_api.getKey();
            String api = cls_api.getValue();
            String methodName = className + " " + api;
            for (String clsname : Utils.expandToSupportClasses(className)) {
                for (CodeLocation cl : SC.getAPIUsage(clsname, methodName, false, true)) {
                    tusages.add(new Pair<>(methodName, cl));
                }
            }
        }
        return tusages;
    }

    private Collection<Pair<String, String>> getClsApis() {
        Collection<Pair<String, String>> cls_apis = new LinkedList<>();
        cls_apis.add(new Pair<>("android.hardware.biometrics.BiometricPrompt$Builder", "setDeviceCredentialAllowed(boolean)"));
        cls_apis.add(new Pair<>("android.hardware.biometrics.BiometricPrompt$Builder", "setNegativeButton(java.lang.CharSequence,java.util.concurrent.Executor,android.content.DialogInterface$OnClickListener)"));
        cls_apis.add(new Pair<>("android.hardware.biometrics.BiometricPrompt$Builder", "setAllowedAuthenticators(int)"));
        cls_apis.add(new Pair<>("androidx.biometric.BiometricPrompt$PromptInfo$Builder", "setDeviceCredentialAllowed(boolean)"));
        cls_apis.add(new Pair<>("androidx.biometric.BiometricPrompt$PromptInfo$Builder", "setNegativeButtonText(java.lang.CharSequence)"));
        cls_apis.add(new Pair<>("androidx.biometric.BiometricPrompt$PromptInfo$Builder", "setAllowedAuthenticators(int)"));
        return cls_apis;
    }

    private Collection<Pair<String, CodeLocation>> filterUsages(Collection<Pair<String, CodeLocation>> tusages) {
        Collection<Pair<String, CodeLocation>> usages_filtered = new LinkedList<>();
        for (Pair<String, CodeLocation> tuple : tusages) {
            CodeLocation cl = tuple.getValue();
            String api = tuple.getKey();
            if (!isSupportClass(cl.smethod.getDeclaringClass())) {
                usages_filtered.add(new Pair<>(api, cl));
            }
        }
        return usages_filtered;
    }

    public void getMeta(Features features) {
        String aaptResult = aaptResult(apkPath);
        features.addMeta("pname", this.pkg);
        features.addMeta("version", strExtract(aaptResult, "versionName='", "'"));
        features.addMeta("fname", this.apkPath);
    }
}
