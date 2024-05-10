package Analysis;

import comm.SootConfig;
import fcm.layout.ResourceUtil;
import javafx.util.Pair;
import org.xmlpull.v1.XmlPullParserException;
import cg.Common;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot_analysis.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static base.Aapt.aaptResult;
import static soot_analysis.Utils.*;
import static soot_analysis.Utils.print;

public class DeactivationAnalysis {
    public String apkPath;
    public String resPath;
    public String pkg;

    public DeactivationAnalysis(String apkpath, String respath) throws XmlPullParserException, IOException {
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
        DeactivationAnalysis deactivationAnalysis = new DeactivationAnalysis(apkpath, respath);

        boolean write_map = Boolean.parseBoolean(args[2]);
        SootConfig.init(deactivationAnalysis.apkPath, "shimple");
        Common.init(write_map);
        ResourceUtil.init(deactivationAnalysis.apkPath);
        deactivationAnalysis.run();
    }

    public void run() throws IOException, ParseException {
        Features features = new Features();
        getMeta(features);
        SootContext sc = new SootContext(Scene.v());
        analyzeDisableAuthentication_semantics(features);
        AnalysisUtils.writeJsonToFile(features.toJson().replace("\\n", "\n"), resPath);
    }

    public void getMeta(Features features) {
        String aaptResult = aaptResult(apkPath);
        features.addMeta("pname", this.pkg);
        features.addMeta("version", strExtract(aaptResult, "versionName='", "'"));
        features.addMeta("fname", this.apkPath);
    }

    public static Collection<SootMethod> getAuthenticateUsages() {
        Collection<SootMethod> usages = new LinkedList<SootMethod>();
        for (String className : Utils.expandToSupportClasses("android.hardware.fingerprint.FingerprintManager")) {
            usages.addAll(getTargetAPI(className, "authenticate"));
        }
        for (String className : Utils.expandToSupportClasses("android.hardware.biometrics.BiometricPrompt")) {
            usages.addAll(getTargetAPI(className, "authenticate"));
        }
        for (String className : Utils.expandToSupportClasses("androidx.biometric.BiometricPrompt")) {
            usages.addAll(getTargetAPI(className, "authenticate"));
        }
        if (usages.size() == 0) {
            print("[analyzeCaller]: no authenticate found!");
        }
        return usages;
    }



    private void analyzeDisableAuthentication_semantics(Features features) {
        Collection<SootMethod> usages = getAuthenticateUsages();
        String res = null;
        for (SootMethod usage : usages) {
            res = switch_dfs_limitedDepth_semantics(usage, 100);
            if (res.contains("@@@")) {
                String[] tres = res.split("@@@");        // callerClass@@@caller_chain
                features.add("Authenticate_bg_dfs", tres[0], usage, String.valueOf(true), "", "");
                break;
            } else {
                features.add("Authenticate_bg_dfs", "", usage, String.valueOf(false), "", res);
            }
        }
    }



    private static String switch_dfs_limitedDepth_semantics(SootMethod authenticateUsage, Integer limitDepth) {
        Deque<Pair<SootMethod, Integer>> dfsStack = new ArrayDeque<>();
        dfsStack.push(new Pair<>(authenticateUsage, 0));
        StringBuilder caller_chain = new StringBuilder();
        Set<SootMethod> visitedMethods = new HashSet<>();
        List<String> featureSdkList = Arrays.asList("com.baidu", "com.tencent", "com.taobao", "com.huawei", "com.meituan", "com.alipay.security");

        while (!dfsStack.isEmpty()) {
            Pair<SootMethod, Integer> methodDepthPair = dfsStack.pop();
            SootMethod method = methodDepthPair.getKey();
            int depth = methodDepthPair.getValue();

            if (visitedMethods.contains(method) || depth > limitDepth) {
                continue;
            }

            caller_chain.append(String.valueOf(methodDepthPair) + '\n');
            visitedMethods.add(method);

            if (isSDKMethod(method, featureSdkList)) {
                return "***SDK:" + method + "***" + caller_chain;
            }

            if (method.toString().toLowerCase().contains("onclick")) {
                if (!method.hasActiveBody()) continue;
                for (Unit unit : method.getActiveBody().getUnits()) {
                    Stmt s = (Stmt) unit;
                    if (s.toString().toLowerCase().contains("checked")) {
                        caller_chain.append(String.valueOf(new Pair<>(method, depth + 1)) + '\n');
                        return method + ":::" + s.toString() + "@@@" + caller_chain;
                    }
                }
            }

            if (!Common.CalleeToCallerMap.containsKey(method) || Common.CalleeToCallerMap.get(method).isEmpty())
                continue;
            int size = Common.CalleeToCallerMap.get(method).size();
            if (size > 50) continue;

            for (SootMethod caller : Common.CalleeToCallerMap.get(method)) {
                if (String.valueOf(caller).equals(String.valueOf(method))) {
                    continue;
                }
                String callerMethod = caller.getSignature();
                if (callerMethod.toLowerCase().contains("switch")) {
                    caller_chain.append(String.valueOf(new Pair<>(callerMethod, depth + 1)) + '\n');
                    return callerMethod + "@@@" + caller_chain;
                }
                else if (callerMethod.contains("onCheckedChanged")) {
                    if(resolve_onCheckedChanged(caller, method)) {
                        caller_chain.append(String.valueOf(new Pair<>(callerMethod, depth + 1)) + '\n');
                        return callerMethod + "@@@" + caller_chain;
                    }
                    else {
                        return "***ONLY WHEN ENABLING***" + caller_chain;
                    }
                }
                else {
                    dfsStack.push(new Pair<>(caller, depth + 1));
                }
            }
        }
        return caller_chain.toString();
    }

    private static boolean isSDKMethod(SootMethod method, List<String> featureSdkList) {
        return featureSdkList.stream().anyMatch(sdk -> method.toString().toLowerCase().contains(sdk));
    }

    private static boolean resolve_onCheckedChanged(SootMethod method, SootMethod callee) {
        SootContext SC = new SootContext(Scene.v());
        Value isChecked = method.getActiveBody().getParameterLocal(1);
        Collection<Tuple<Unit, SootMethod>> toExploreUnits = new LinkedList<>();
        Collection<Unit> useUnits = SC.getUseUnits(isChecked.toString(), method);
        if (useUnits != null) {
            for (Unit newUnit : useUnits) {
                toExploreUnits.add(new Tuple(newUnit, method));
            }
        }
        Stmt targetIfStmt = null;
        for (Tuple<Unit, SootMethod> tstate : toExploreUnits) {
            Unit newUnit = tstate.x;
            Stmt smt = (Stmt) newUnit;

            if (smt instanceof IfStmt) {
                targetIfStmt = smt;
                break;
            }
        }

        UnitGraph cfg = new ExceptionalUnitGraph(method.retrieveActiveBody());
        boolean inIfBranch = false;
        List<Unit> ifBranchStatements = new ArrayList<>();
        List<Unit> elseBranchStatements = new ArrayList<>();

        for (Unit unit : cfg.getBody().getUnits()) {
            if (unit instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) unit;
                String condition = ifStmt.toString();
                if (condition.equals(targetIfStmt.toString())) {
                    inIfBranch = true;
                } else {
                    inIfBranch = false;
                }
            } else if (inIfBranch) {
                ifBranchStatements.add(unit);
            } else {
                elseBranchStatements.add(unit);
            }
        }
        if(isAuthenticateInvoked(ifBranchStatements, callee) ^ isAuthenticateInvoked(elseBranchStatements, callee)) {
            return false;
        }
        return true;
    }

    private static boolean isAuthenticateInvoked(List<Unit> unitList, SootMethod targetCallee) {
        for(Unit unit:unitList) {
            Stmt s = (Stmt) unit;
            if (s.containsInvokeExpr()) {
                SootMethod callee = s.getInvokeExpr().getMethodRef().tryResolve();
                if (callee == null)
                    continue;
                if(callee.toString().equals(targetCallee.toString()))
                    return true;
            }
        }
        return false;
    }

    private static Set<SootMethod> getTargetAPI(String classname, String methodName) {
        Set<SootMethod> methods = new HashSet<>();
        if (!Scene.v().containsClass(classname))
            return methods;
        SootClass sootClass = Scene.v().getSootClass(classname);
        if (!sootClass.declaresMethodByName(methodName))
            return methods;
        for (SootMethod sootMethod : sootClass.getMethods()) {
            if (!sootMethod.getName().equals(methodName))
                continue;
            methods.add(sootMethod);
        }
        return methods;
    }
}