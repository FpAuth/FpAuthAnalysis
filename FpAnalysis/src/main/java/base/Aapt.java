package base;

import soot_analysis.SootAnalysis;
import soot_analysis.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static soot_analysis.Utils.print;

public class Aapt {

    public static String aaptResult(String fname){
        String tstr = "";

        try {
            String [] args = new String[] {"aapt", "dump", "badging", fname};
            print(Utils.join(" ", args));
            Process exec = Runtime.getRuntime().exec(args);
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            String s = null;
            while ((s = stdOut.readLine()) != null) {
                tstr += s + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tstr;
    }
}
