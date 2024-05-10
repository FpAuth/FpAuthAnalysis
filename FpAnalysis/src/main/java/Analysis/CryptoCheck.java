package Analysis;

import comm.SootConfig;
import fcm.layout.ResourceUtil;
import org.xmlpull.v1.XmlPullParserException;
import cg.Common;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot_analysis.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static base.Aapt.aaptResult;
import static soot_analysis.SootAnalysis.*;
import static soot_analysis.SootAnalysis.isSliceToConstant;
import static soot_analysis.Utils.*;
import static soot_analysis.Utils.join;

public class CryptoCheck {
    public String apkPath;
    public String resPath;
    public String pkg;

    public CryptoCheck(String apkpath, String respath) throws XmlPullParserException, IOException {
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
        CryptoCheck cryptoCheck = new CryptoCheck(apkpath, respath);

        boolean write_map = Boolean.parseBoolean(args[2]);
        SootConfig.init(cryptoCheck.apkPath, "jimple");
        Common.init(write_map);
        ResourceUtil.init(cryptoCheck.apkPath);
        cryptoCheck.run();
    }

    public void run() throws IOException, ParseException {
        Features features = new Features();
        getMeta(features);
        SootContext sc = new SootContext(Scene.v());

        analyzeKeyGen(features, sc);
		analyzeOnAuthenticationSucceeded(features, sc);
        analyzeAuthenticationRequired(features, sc);
        analyzeAuthenticate(features, sc);
        analyzeInvalidated(features, sc);

        AnalysisUtils.writeJsonToFile(features.toJson().replace("\\n", "\n"), resPath);
    }

    public void getMeta(Features features) {
        String aaptResult = aaptResult(apkPath);
        features.addMeta("pname", this.pkg);
        features.addMeta("version", strExtract(aaptResult, "versionName='", "'"));
        features.addMeta("fname", this.apkPath);
    }


    private static void analyzeKeyGen(Features features, SootContext SC) {
        Collection<CodeLocation> usages = SC.getAPIUsage("android.security.keystore.KeyGenParameterSpec$Builder", "void <init>(java.lang.String,int)", false, false);

        for (CodeLocation cl : usages) {
            Value vv = getInvokeParameter(SC, cl.sunit, 1);
            String result = handleIntFlag(SC, cl, vv, 4, "and") ? "Asymm" : "Symm";
            features.add("Keybuilder", vv, cl, result, "", SC.getInvokeExpr(cl.sunit));
        }

        SootClass scc = SC.cm.get("java.security.spec.AlgorithmParameterSpec");
        if (scc == null) {
            return;
        }
        List<SootClass> scl = SC.ch.getDirectImplementersOf(scc);
        List<String> exotic_classes = new LinkedList<String>();
        for (SootClass sc : scl) {
            if (sc.getShortName().equals("KeyGenParameterSpec")) {
                continue;
            }
            exotic_classes.add(sc.getName() + "$Builder");
        }
        Collection<CodeLocation> exotic_usages = SC.getAPIUsage(exotic_classes, "void <init>", true, false);
        for (CodeLocation cl : exotic_usages) {
            String result = "Exotic";
            features.add("Keybuilder", SC.getInvokeExpr(cl.sunit).getMethod().getDeclaringClass().getShortName(), cl, result, "", SC.getInvokeExpr(cl.sunit));
        }
    }

    private static void analyzeAuthenticate(Features features, SootContext SC) {
        Collection<CodeLocation> usages = getUsages(SC);
        Collection<CodeLocation> usages_filtered = filterUsages(SC, usages);

        for (CodeLocation cl : usages_filtered) {
            Value vv = getInvokeParameter(SC, cl.sunit, 0);
            String result;
            String slice = "";
            if (handleIntFlag(SC, cl, vv, 0, "equal")) {
                result = "Weak";
            } else {
                String reg = String.valueOf(vv);
                Slicer sl = new Slicer(SC, cl.sunit, reg, cl.smethod);
                sl.followMethodParams = true;
                sl.followReturns = true;
                sl.followFields = true;
                Tree<SlicerState> stree = sl.run(20);
                result = isNullSliceForAuthenticate(stree) ? "Weak" : "Strong";
                slice = String.valueOf(stree);
            }
            features.add("Authenticate", vv, cl, result, slice, SC.getInvokeExpr(cl.sunit));
        }

    }

    private static Collection<CodeLocation> getUsages(SootContext SC) {
        Collection<CodeLocation> usages = new LinkedList<>();
        String[] classes = {"android.hardware.fingerprint.FingerprintManager",
                "android.hardware.biometrics.BiometricPrompt",
                "androidx.biometric.BiometricPrompt"};

        for (String className : classes) {
            for (String expandedClass : Utils.expandToSupportClasses(className)) {
                usages.addAll(SC.getAPIUsage(expandedClass, "void authenticate", true, true));
            }
        }

        if (usages.isEmpty()) {
            usages.addAll(SC.getAPIUsage("android.hardware.fingerprint.FingerprintManager", "void authenticate", true, false));
        }
        return usages;
    }

    private static Collection<CodeLocation> filterUsages(SootContext SC, Collection<CodeLocation> usages) {
        Collection<CodeLocation> usages_filtered = new LinkedList<>();
        for (CodeLocation cl : usages) {
            if (!isSupportClass(cl.smethod.getDeclaringClass())) {
                usages_filtered.add(cl);
            } else {
                BackwardCallgraph bc = new BackwardCallgraph(SC, cl.smethod);
                Tree<CallgraphState> btree = bc.run(20);

                for (Node<CallgraphState> ncs : btree.nodeMap.values()) {
                    CallgraphState cs = ncs.value;
                    if (!isSupportClass(cs.method.getDeclaringClass())) {
                        usages_filtered.add(cl);
                        break;
                    }
                }
            }
        }
        return usages_filtered;
    }

    private static void analyzeOnAuthenticationSucceeded(Features features, SootContext SC) {
        Collection<SootMethod> possibleTargets = getSignatureAndCipherMethods(SC);
        Collection<Tree<CallgraphState>> possibleTargetsTrees = getPossibleTargetsTrees(SC, possibleTargets);
        Collection<SootMethod> succeededUsagesFiltered = getSucceededUsagesFiltered(SC);
        
        for (SootMethod m : succeededUsagesFiltered) {
            analyzeSucUsage(features, SC, possibleTargetsTrees, m);
        }
    }

    private static Collection<SootMethod> getSignatureAndCipherMethods(SootContext SC) {
        Collection<SootMethod> possibleTargets = new LinkedList<>();
        String[] classes = {"java.security.Signature", "javax.crypto.Cipher"};
        String[] methods = {"sign(", " update(", "doFinal("};

        for (String className : classes) {
            SootClass sc = SC.cm.get(className);
            if (sc != null) {
                for (SootMethod mm : sc.getMethods()) {
                    for (String method : methods) {
                        if (mm.getSubSignature().contains(method)) {
                            possibleTargets.add(mm);
                        }
                    }
                }
            }
        }
        return possibleTargets;
    }

    private static Collection<Tree<CallgraphState>> getPossibleTargetsTrees(SootContext SC, Collection<SootMethod> possibleTargets) {
        Collection<Tree<CallgraphState>> possibleTargetsTrees = new LinkedList<>();
        for (SootMethod mm : possibleTargets) {
            BackwardCallgraph bc = new BackwardCallgraph(SC, mm);
            bc.skipLibraries = true;
            Tree<CallgraphState> tree = bc.run(100);
            if (tree.nodeMap.size() > 1) {
                possibleTargetsTrees.add(tree);
            }
        }
        return possibleTargetsTrees;
    }

    private static Collection<SootMethod> getSucceededUsagesFiltered(SootContext SC) {
        Collection<SootMethod> succeededUsagesFiltered = new LinkedList<>();
        for (String className : Utils.expandToSupportClasses("android.hardware.fingerprint.FingerprintManager$AuthenticationCallback")) {
            SootMethod mm = SC.resolveMethod(className, "void onAuthenticationSucceeded", true);
            if (mm != null) {
                Collection<SootMethod> tusages = SC.getOverrides(mm);
                for (SootMethod m : tusages) {
                    if (!Utils.isSupportClass(m.getDeclaringClass())) {
                        succeededUsagesFiltered.add(m);
                    }
                }
            }
        }
        return succeededUsagesFiltered.isEmpty() ? succeededUsagesFiltered : new LinkedList<>();
    }

    private static void analyzeSucUsage(Features features, SootContext SC, Collection<Tree<CallgraphState>> possibleTargetsTrees, SootMethod m) {
        ForwardCallgraph fc = new ForwardCallgraph(SC, m);
        Tree<CallgraphState> tree = fc.run(200);

        boolean foundSomething = false;

        for (Tree<CallgraphState> btree : possibleTargetsTrees) {
            Tree<CallgraphState> connectedTree = intersectTrees(tree, btree);
            if (connectedTree != null) {
                foundSomething = analyzeConnectedTree(features, SC, m, connectedTree);
            }
        }

        if (!foundSomething) {
            features.add("Succeeded", "", join(",", new Object[]{m, null, null}), "Unknown", "", "");
        }
    }

    private static boolean analyzeConnectedTree(Features features, SootContext SC, SootMethod m, Tree<CallgraphState> connectedTree) {
        boolean foundSomething = false;
        for (Node<CallgraphState> n : connectedTree.nodeMap.values()) {
            SootMethod m2 = n.value.method;
            String cname = m2.getDeclaringClass().getName();
            String mname = m2.getSubSignature();
            Tuple<Unit, InvokeExpr> u_i = SC.recoverEdge(n.value.method, n.parent.value.method);
            if (u_i == null) {
                continue;
            }
            Unit uu = u_i.x;
            InvokeExpr ie = u_i.y;
            
            if (cname.equals("java.security.Signature") && (mname.contains("sign(") || mname.contains(" update("))) {
                String result = "Asymm";
                String extra = "";
                if (mname.contains("update(")) {
                    Value vv2 = ie.getArgs().get(0);
                    String reg = String.valueOf(vv2);
                    if (reg.startsWith("$")) {
                        Slicer sl = new Slicer(SC, uu, reg, n.parent.value.method);
                        Tree<SlicerState> stree = sl.run(20);
                        extra = String.valueOf(stree);
                    } else {
                        extra = String.valueOf(reg);
                    }
                }
                if (mname.contains("sign(") && uu.getDefBoxes().size() == 0) {
                    result = "Weak";
                }
                features.add("Succeeded", m2.getSignature(), join(",", new Object[]{m, uu, n.parent.value.method}), result, "", extra);
                foundSomething = true;
            }

            if (cname.equals("javax.crypto.Cipher") && (mname.contains("doFinal(") || mname.contains(" update("))) {
                if (mname.contains("doFinal(")) {
                    boolean isEncryptingConstant = false;
                    if (ie.getArgs().size() == 1) {
                        String reg = String.valueOf(ie.getArg(0));
                        if (reg.startsWith("$")) {
                            Slicer sl = new Slicer(SC, uu, reg, n.parent.value.method);
                            sl.skipThisReg = false;
                            sl.followMethodParams = true;
                            Tree<SlicerState> stree = sl.run(20);
                            isEncryptingConstant = isSliceToConstant(stree);
                        }
                    }
                    String result = (isEncryptingConstant || uu.getDefBoxes().size() == 0) ? "Weak" : "Symm";
                    features.add("Succeeded", m2.getSignature(), join(",", new Object[]{m, uu, n.parent.value.method}), result, "", "");
                } else {
                    features.add("Succeeded", m2.getSignature(), join(",", new Object[]{m, uu, n.parent.value.method}), "Symm", "", "");
                }
                foundSomething = true;
            }
        }
        return foundSomething;
    }

    private static void analyzeAuthenticationRequired(Features features, SootContext SC) {
        Collection<CodeLocation> usages = SC.getAPIUsage("android.security.keystore.KeyGenParameterSpec$Builder", "android.security.keystore.KeyGenParameterSpec$Builder setUserAuthenticationRequired(boolean)", false, false);
        for (CodeLocation cl : usages) {
            Value vv = getInvokeParameter(SC, cl.sunit, 0);
            features.add("AuthenticationRequired", vv, cl, "", "KeyGenParameterSpec$Builder", SC.getInvokeExpr(cl.sunit));
        }
        usages = SC.getAPIUsage("android.security.keystore.KeyProtection$Builder", "android.security.keystore.KeyProtection$Builder setUserAuthenticationRequired(boolean)", false, false);
        for (CodeLocation cl : usages) {
            Value vv = getInvokeParameter(SC, cl.sunit, 0);
            features.add("AuthenticationRequired", vv, cl, "", "KeyProtection$Builder", SC.getInvokeExpr(cl.sunit));
        }
    }

    private static void analyzeInvalidated(Features features, SootContext SC){
        Collection<CodeLocation> usages = SC.getAPIUsage("android.security.keystore.KeyGenParameterSpec$Builder", "android.security.keystore.KeyGenParameterSpec$Builder setInvalidatedByBiometricEnrollment(boolean)", false, false);
        for(CodeLocation cl : usages){
            Value vv = getInvokeParameter_resolve(SC,cl.sunit, 0, cl);
            features.add("InvalidatedByBiometricEnrollment", vv, cl, "", "KeyGenParameterSpec$Builder", SC.getInvokeExpr(cl.sunit));
        }
        usages = SC.getAPIUsage("android.security.keystore.KeyProtection$Builder", "android.security.keystore.KeyProtection$Builder setInvalidatedByBiometricEnrollment(boolean)", false, false);
        for(CodeLocation cl : usages){
            Value vv = getInvokeParameter_resolve(SC,cl.sunit, 0, cl);
            features.add("InvalidatedByBiometricEnrollment", vv, cl, "", "KeyProtection$Builder", SC.getInvokeExpr(cl.sunit));
        }
    }

}
