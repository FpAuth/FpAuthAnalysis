package Analysis;

import comm.CallPath;
import comm.SootConfig;
import fcm.layout.ResourceUtil;
import org.xmlpull.v1.XmlPullParserException;
import cg.Common;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot_analysis.Features;
import soot_analysis.Utils;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static base.Aapt.aaptResult;
import static soot_analysis.Utils.print;
import static soot_analysis.Utils.strExtract;



public class CallchainAnalysis {
    public String apkPath;
    public String resPath;
    public String pkg;
    private Set<SootMethod> visited = new HashSet<>();
    private Map<SootMethod, Boolean> cannotReached = new HashMap<>();
    private List<String> entryPoints = new ArrayList<>();
    private Set<String> launcherActivitiesPrefix = new HashSet<>();

    public CallchainAnalysis(String apkPath, String resPath) throws XmlPullParserException, IOException {
        this.apkPath = apkPath;
        this.pkg = getPkgname(apkPath);
        this.resPath = resPath;
    }

    public String getPkgname(String apkpath) throws XmlPullParserException, IOException {
        try {
            ProcessManifest processMan = new ProcessManifest(apkpath);
            entryPoints.addAll(processMan.getEntryPointClasses());

            Set<AXmlNode> launchableActivities = processMan.getLaunchableActivities();
            for (AXmlNode node : launchableActivities) {
                String className = getActivityName(node);
                launcherActivitiesPrefix.add(getPrefix(className));
            }
            return processMan.getPackageName();
        } catch (Exception e) {
            return apkpath.substring(apkpath.lastIndexOf('/') + 1, apkpath.length() - 4);
        }
    }

    public static String getActivityName(AXmlNode node) {
        AXmlAttribute<?> attr = node.getAttribute("name");
        return (String) attr.getValue();
    }

    public static String getPrefix(String cla) {
        int firstDot = cla.indexOf('.');
        if(firstDot == -1) return cla;
        int secondDot = cla.indexOf('.', firstDot + 1);
        if(secondDot == -1) return cla;
        int thirdDot = cla.indexOf('.', secondDot + 1);

        if (thirdDot != -1) {
            return cla.substring(0, thirdDot);
        } else {
            return cla;
        }
    }

    public static void main(String[] args) throws XmlPullParserException, IOException, ParseException {
        String apkpath = args[0];
        String respath = args[1];
        CallchainAnalysis callchainAnalysis = new CallchainAnalysis(apkpath, respath);

        boolean write_map = Boolean.parseBoolean(args[2]);
        SootConfig.init(callchainAnalysis.apkPath, "jimple");
        Common.init(write_map);
        ResourceUtil.init(callchainAnalysis.apkPath);
        callchainAnalysis.run();
    }

    public void run() throws IOException, ParseException {
        Features features = new Features();
        getMeta(features);
        analyzeCaller(features);
        print(features.toJson());
        AnalysisUtils.writeJsonToFile(features.toJson().replace("\\n", "\n"), resPath);
    }

    public void getMeta(Features features) {
        String aaptResult = aaptResult(apkPath);
        features.addMeta("pname", this.pkg);
        features.addMeta("version", strExtract(aaptResult, "versionName='", "'"));
        features.addMeta("fname", this.apkPath);
    }

    private void analyzeCaller(Features features) {
        List<String> classNames = Arrays.asList(
                "android.hardware.fingerprint.FingerprintManager",
                "android.hardware.biometrics.BiometricPrompt",
                "androidx.biometric.BiometricPrompt"
        );
        Collection<SootMethod> usages = getUsagesForClasses(classNames, "authenticate");

        if (usages.isEmpty()) {
            print("[analyzeCaller]: no authenticate found!");
            return;
        }

        for (SootMethod api : usages) {
            visited.clear();
            List<CallPath> callPaths = getCallChain(api);

            if(callPaths.isEmpty()) {
                features.add("Authenticate_caller_in_pkg", "", "", String.valueOf(false),"","");
                continue;
            }
            for(CallPath callPath: callPaths) {
                String path = callPath.toString();
                if(!path.isEmpty()) {
                    SootMethod last = callPath.getLast();
                    features.add("Authenticate_caller_in_pkg", last.getDeclaringClass().toString(),last.getSignature(), String.valueOf(true), "", path);
                } else {
                    features.add("Authenticate_caller_in_pkg", "", "", String.valueOf(false),"","");
                }
            }
        }
    }

    private Collection<SootMethod> getUsagesForClasses(List<String> classNames, String methodName) {
        Collection<SootMethod> usages = new LinkedList<>();
        for (String className : classNames) {
            for (String expandedClassName : Utils.expandToSupportClasses(className)) {
                usages.addAll(getTargetAPI(expandedClassName, methodName));
            }
        }
        return usages;
    }

    private Set<SootMethod> getTargetAPI(String classname, String methodName) {
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

    private List<CallPath> getCallChain(SootMethod sootMethod) {
        List<CallPath> allPath = new ArrayList<>();
        CallPath path = new CallPath(sootMethod);
        cannotReached.clear();
        getCallChainRecursive(sootMethod, path, allPath);
        return allPath;
    }

    private boolean getCallChainRecursive(SootMethod sootMethod, CallPath path, List<CallPath> allPath) {
        if (isEntryPoint(sootMethod.getDeclaringClass())) {
            allPath.add(path);
            return true;
        }
        if (!cannotReached.getOrDefault(sootMethod, true)) return false;
        if (visited.contains(sootMethod)) return false;

        visited.add(sootMethod);
        if (path.size() != 1 && (sootMethod.isJavaLibraryMethod() || AnalysisUtils.isAndroidOrJavaClass(sootMethod.getDeclaringClass())))
            return false;

        if (!Common.CalleeToCallerMap.containsKey(sootMethod) || Common.CalleeToCallerMap.get(sootMethod).isEmpty())
            return false;

        int size = Common.CalleeToCallerMap.get(sootMethod).size();
        if (size > 50) return false;

        if (sootMethod.getDeclaringClass().toString().matches(".*(io\\.reactivex).*|.*(kotlin).*|.*(okhttps\\.Call).*|.*(com\\.google\\.android\\.gms\\.internal).*")) {
            if (size > 40 && path.size() != 1)
                return false;
        }

        boolean reached = false;
        for (SootMethod method : Common.CalleeToCallerMap.get(sootMethod)) {
            if (path.hasMethod(method))
                continue;
            CallPath newPath = new CallPath(path);
            newPath.addCall(method);
            if (getCallChainRecursive(method, newPath, allPath))
                reached = true;
        }

        cannotReached.put(sootMethod, reached);
        return reached;
    }

    public boolean isEntryPoint(SootClass sootClass) {
        String pkgPrefix = "";
        int firstdot = pkg.indexOf('.');
        if(firstdot == -1)  pkgPrefix = pkg;
        else {
            int seconddot = pkg.indexOf('.', firstdot + 1);
            if(seconddot == -1) pkgPrefix = pkg;
            else pkgPrefix = pkg.substring(0, seconddot);
        }
        return entryPoints.contains(sootClass.toString()) || sootClass.toString().startsWith(pkgPrefix) || isLauncherPrefix(sootClass.toString());
    }

    public boolean isLauncherPrefix(String sc) {
        for(String pre:launcherActivitiesPrefix) {
            if(sc.startsWith(pre))
                return true;
        }
        return false;
    }
}
