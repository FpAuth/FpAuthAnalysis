package comm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Common {
    public static List<String> excludeList = new LinkedList<>(Arrays.asList(
            "java.*",
            "sun.*",
            "android.*",
            "androidx.*",
            "org.apache.*",
            "org.eclipse.*",
            "soot.*",
            "javax.*",
            "jdk.*"
    ));


    public static List<String> analyzeDeleteKeywords = new LinkedList<>(Arrays.asList(
            "Credential"
    ));
}
