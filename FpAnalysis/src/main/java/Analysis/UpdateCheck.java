package Analysis;

import comm.SootConfig;
import fcm.layout.ResourceUtil;
import org.xmlpull.v1.XmlPullParserException;
import cg.Common;
import soot.*;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot_analysis.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static base.Aapt.aaptResult;
import static soot_analysis.Utils.*;

public class UpdateCheck {
    public String apkPath;
    public String resPath;
    public String pkg;

    public UpdateCheck(String apkpath, String respath) throws XmlPullParserException, IOException {
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
        UpdateCheck updateCheck = new UpdateCheck(apkpath, respath);

        boolean write_map = Boolean.parseBoolean(args[2]);
        SootConfig.init(updateCheck.apkPath, "shimple");
        Common.init(write_map);
        ResourceUtil.init(updateCheck.apkPath);
        updateCheck.run();
    }

    public void run() throws IOException, ParseException {
        Features features = new Features();
        getMeta(features);
        SootContext sc = new SootContext(Scene.v());

        analyzeDelete(features, sc);

        String jsonstr = features.toJson();
        jsonstr = jsonstr.replace("\\n", "\n");
        AnalysisUtils.writeJsonToFile(jsonstr, resPath);
    }

    public void getMeta(Features features) {
        String aaptResult = aaptResult(apkPath);
        String pname = strExtract(aaptResult, "package: name='", "'");
        print(pname);
        String pversion = strExtract(aaptResult, "versionName='", "'");
        print(pversion);

        features.addMeta("pname", this.pkg);
        features.addMeta("version", pversion);
        features.addMeta("fname", this.apkPath);
    }


    private void analyzeDelete(Features features, SootContext SC) {
        Collection<CodeLocation> usages_filtered = getUsagesFiltered(SC);

        Collection<Tree<SlicerState>> strees = new LinkedList<>();
        for (CodeLocation cl : usages_filtered) {
            Unit uu = cl.sunit;
            List<ValueBox> defBoxes = uu.getDefBoxes();
            if (defBoxes.size() < 1)
                continue;
            Value returnValue = defBoxes.get(0).getValue();

            ForwardSlicer FS = new ForwardSlicer(SC, uu, String.valueOf(returnValue), cl.smethod);
            Tree<SlicerState> tree = FS.run_track(100);
            strees.add(tree);

            if (isWeakUnlock(tree)) {
                features.add("Unlock", uu.toString(), cl, "WEAK", String.valueOf(returnValue), String.valueOf(tree));
            } else {
                features.add("Unlock", uu.toString(), cl, "STRONG", String.valueOf(returnValue), String.valueOf(tree));
            }
        }
    }

    private static boolean isWeakUnlock(Tree<SlicerState> stree) {
        for (SlicerState ss : stree.getAllNodes()) {
            if (ss.reg.equals("if")) {
                Node<SlicerState> sn = stree.getNode(ss);
                List<Node<SlicerState>> children = sn.children;
                if (children.size() == 2) {
                    String intent1 = children.get(0).value.reg;
                    String intent2 = children.get(1).value.reg;
                    if (isDifferentIntent(intent1, intent2) || isSameIntent(intent1, intent2) || isAuthenticateIntent(intent1, intent2)) {
                        return true;
                    }
                }
            }
            if (ss.reg.equals("WEAK")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDifferentIntent(String intent1, String intent2) {
        return !intent1.equals(intent2) && (intent1.contains("MainActivity") || intent2.contains("MainActivity"));
    }

    private static boolean isSameIntent(String intent1, String intent2) {
        return intent1.contains("MainActivity") && intent2.contains("MainActivity");
    }

    private static boolean isAuthenticateIntent(String intent1, String intent2) {
        return intent1.contains("authenticate") ^ intent2.contains("authenticate") && !(intent1.contains("keyword") || intent2.contains("keyword"));
    }

    private Collection<CodeLocation> getUsagesFiltered(SootContext SC) {
        Collection<CodeLocation> usages = new LinkedList<>();
        for (String className : Utils.expandToSupportClasses("android.hardware.fingerprint.FingerprintManager")) {
            usages.addAll(SC.getAPIUsage(className, "boolean hasEnrolledFingerprints", true, true));
        }
        for (String className : Utils.expandToSupportClasses("androidx.biometric.BiometricManager")) {
            usages.addAll(SC.getAPIUsage(className, "int canAuthenticate", true, true));
        }
        for (String className : Utils.expandToSupportClasses("android.hardware.biometrics.BiometricManager")) {
            usages.addAll(SC.getAPIUsage(className, "int canAuthenticate", true, true));
        }

        Collection<CodeLocation> usages_filtered = new LinkedList<>();
        for (CodeLocation cl : usages) {
            if (!isSupportClass(cl.smethod.getDeclaringClass())) {
                usages_filtered.add(cl);
            }
        }
        return usages_filtered;
    }

}
