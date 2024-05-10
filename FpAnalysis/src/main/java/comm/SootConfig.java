package comm;

import soot.*;
import soot.options.Options;

import java.util.Collections;

import static soot_analysis.Utils.print;

public class SootConfig {

    public static void init(String appPath, String format) {
        G.reset();
        Options.v().set_src_prec(Options.src_prec_apk);
        java.nio.file.Path p = java.nio.file.Paths.get(appPath);
        String filename = p.getFileName().toString();
        Options.v().set_full_resolver(true);
        Options.v().set_drop_bodies_after_load(false);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().ignore_resolution_errors();
        Options.v().set_no_writeout_body_releasing(true);
        Options.v().set_output_dir("./output/FpAnalysis/" + filename + "/");

        Options.v().set_whole_program(true);
        Options.v().set_process_multiple_dex(true);

        Options.v().set_exclude(Common.excludeList);


        if("shimple".equals(format)) {
//            Options.v().set_output_format(Options.output_format_none);
            Options.v().set_output_format(Options.output_format_shimple);
            Options.v().setPhaseOption("cg", "enabled:false");

            String[] sootArgs = new String[]{
                    "-pp",
                    "-android-jars", Config.androidPlatformPath,
                    "-process-dir", appPath
            };
            Main.main(sootArgs);
        } else if ("jimple".equals(format)) {
//            Options.v().set_output_format(Options.output_format_jimple);
            Options.v().set_output_format(Options.output_format_none);
            String[] sootArgs = new String[]{
                    "-pp",
                    "-android-jars", Config.androidPlatformPath,
                    "-process-dir", appPath
            };
            Main.main(sootArgs);
        }
    }
}
