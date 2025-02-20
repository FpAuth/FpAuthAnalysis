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

## Vulnerability IDs

The following are the CVE IDs we obtained:

* CVE-2024-40238 to CVE-2024-40300
* CVE-2024-40139 to CVE-2024-40189
* CVE-2024-40191 to CVE-2024-40237
* CVE-2024-31681 to CVE-2024-31703


The following are the CNVD IDs we obtained:

CNVD-2023-19675, CNVD-2023-56454, CNVD-2023-66432, CNVD-2023-68099, CNVD-2023-68953, CNVD-2023-68954, CNVD-2023-69760, CNVD-2023-85534, CNVD-2023-86945, CNVD-2023-86947, CNVD-2023-94481, CNVD-2023-94843, CNVD-2023-101432, CNVD-2024-03058, CNVD-2024-03059, CNVD-2024-07356, CNVD-2024-22620, CNVD-2024-23119, CNVD-2024-23121
