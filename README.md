## Configuration

This project requires some initial configuration before it can be run. The configuration settings are located in the `Config.java` file in the `comm` package.

1. **Android SDK Path**: Set the `androidPlatformPath` variable to the path of your Android SDK. This is used for static analysis of the APK file. For example:

```Java
public static String androidPlatformPath = "/path/to/your/android_sdk/";
```



## Running

After building the project, you can run the generated `jar` file using the following command. 

```Bash
java -jar FpAnalysis.jar /path1/$pkg.apk /path2/$pkg.json false
```

1. `/path1/$pkg.apk`: the path to the APK file you want to analyze.
2. `/path2/$pkg.json`: the path to the JSON file where the analysis results will be written.
3. `false`: whether to save the callgraph map.



## Result Processing

```Bash
python3 FpAnalysis_result_process.py
```
