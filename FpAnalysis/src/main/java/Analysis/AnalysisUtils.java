package Analysis;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot_analysis.Features;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static base.Aapt.aaptResult;
import static soot_analysis.Utils.strExtract;

public class AnalysisUtils {
    public static void writeJsonToFile(String jsonstr, String jsonpath) throws IOException {
        FileWriter fw = new FileWriter(jsonpath);
        PrintWriter out = new PrintWriter(fw);
        out.write(jsonstr);
        out.println();
        fw.close();
        out.close();
    }

    public static boolean isAndroidOrJavaClass(SootClass sootClass) {
        return (sootClass.getPackageName().startsWith("java.") || sootClass.getPackageName().startsWith("android.")
                || sootClass.getPackageName().startsWith("androidx.") || sootClass.getPackageName().startsWith("javax."))
                && !sootClass.getName().toLowerCase().contains("fingerprint") && !sootClass.getName().toLowerCase().contains("biometric");
    }

    public static List<String> readFile(String fileName) {
        List<String> fileLines = new ArrayList<>();
        File file = new File(fileName);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fileLines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileLines;
    }
}
