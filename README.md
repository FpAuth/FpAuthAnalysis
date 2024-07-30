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

The following is a list of the received CVE IDs:

<table>
<tr>
    <td>CVE-2024-40238</td>
    <td>CVE-2024-40239</td>
    <td>CVE-2024-40240</td>
    <td>CVE-2024-40241</td>
    <td>CVE-2024-40242</td>
    <td>CVE-2024-40243</td>
    <td>CVE-2024-40244</td>
    <td>CVE-2024-40245</td>
</tr>
<tr>
    <td>CVE-2024-40246</td>
    <td>CVE-2024-40247</td>
    <td>CVE-2024-40248</td>
    <td>CVE-2024-40249</td>
    <td>CVE-2024-40250</td>
    <td>CVE-2024-40251</td>
    <td>CVE-2024-40252</td>
    <td>CVE-2024-40253</td>
</tr>
<tr>
    <td>CVE-2024-40254</td>
    <td>CVE-2024-40255</td>
    <td>CVE-2024-40256</td>
    <td>CVE-2024-40257</td>
    <td>CVE-2024-40258</td>
    <td>CVE-2024-40259</td>
    <td>CVE-2024-40260</td>
    <td>CVE-2024-40261</td>
</tr>
<tr>
    <td>CVE-2024-40262</td>
    <td>CVE-2024-40263</td>
    <td>CVE-2024-40264</td>
    <td>CVE-2024-40265</td>
    <td>CVE-2024-40266</td>
    <td>CVE-2024-40267</td>
    <td>CVE-2024-40268</td>
    <td>CVE-2024-40269</td>
</tr>
<tr>
    <td>CVE-2024-40270</td>
    <td>CVE-2024-40271</td>
    <td>CVE-2024-40272</td>
    <td>CVE-2024-40273</td>
    <td>CVE-2024-40274</td>
    <td>CVE-2024-40275</td>
    <td>CVE-2024-40276</td>
    <td>CVE-2024-40277</td>
</tr>
<tr>
    <td>CVE-2024-40278</td>
    <td>CVE-2024-40279</td>
    <td>CVE-2024-40280</td>
    <td>CVE-2024-40281</td>
    <td>CVE-2024-40282</td>
    <td>CVE-2024-40283</td>
    <td>CVE-2024-40284</td>
    <td>CVE-2024-40285</td>
</tr>
<tr>
    <td>CVE-2024-40286</td>
    <td>CVE-2024-40287</td>
    <td>CVE-2024-40288</td>
    <td>CVE-2024-40289</td>
    <td>CVE-2024-40290</td>
    <td>CVE-2024-40291</td>
    <td>CVE-2024-40292</td>
    <td>CVE-2024-40293</td>
</tr>
<tr>
    <td>CVE-2024-40294</td>
    <td>CVE-2024-40295</td>
    <td>CVE-2024-40296</td>
    <td>CVE-2024-40297</td>
    <td>CVE-2024-40298</td>
    <td>CVE-2024-40299</td>
    <td>CVE-2024-40300</td>
    <td>CVE-2024-40139</td>
</tr>
<tr>
    <td>CVE-2024-40140</td>
    <td>CVE-2024-40141</td>
    <td>CVE-2024-40142</td>
    <td>CVE-2024-40143</td>
    <td>CVE-2024-40144</td>
    <td>CVE-2024-40145</td>
    <td>CVE-2024-40146</td>
    <td>CVE-2024-40147</td>
</tr>
<tr>
    <td>CVE-2024-40148</td>
    <td>CVE-2024-40149</td>
    <td>CVE-2024-40150</td>
    <td>CVE-2024-40151</td>
    <td>CVE-2024-40152</td>
    <td>CVE-2024-40153</td>
    <td>CVE-2024-40154</td>
    <td>CVE-2024-40155</td>
</tr>
<tr>
    <td>CVE-2024-40156</td>
    <td>CVE-2024-40157</td>
    <td>CVE-2024-40158</td>
    <td>CVE-2024-40159</td>
    <td>CVE-2024-40160</td>
    <td>CVE-2024-40161</td>
    <td>CVE-2024-40162</td>
    <td>CVE-2024-40163</td>
</tr>
<tr>
    <td>CVE-2024-40164</td>
    <td>CVE-2024-40165</td>
    <td>CVE-2024-40166</td>
    <td>CVE-2024-40167</td>
    <td>CVE-2024-40168</td>
    <td>CVE-2024-40169</td>
    <td>CVE-2024-40170</td>
    <td>CVE-2024-40171</td>
</tr>
<tr>
    <td>CVE-2024-40172</td>
    <td>CVE-2024-40173</td>
    <td>CVE-2024-40174</td>
    <td>CVE-2024-40175</td>
    <td>CVE-2024-40176</td>
    <td>CVE-2024-40177</td>
    <td>CVE-2024-40178</td>
    <td>CVE-2024-40179</td>
</tr>
<tr>
    <td>CVE-2024-40180</td>
    <td>CVE-2024-40181</td>
    <td>CVE-2024-40182</td>
    <td>CVE-2024-40183</td>
    <td>CVE-2024-40184</td>
    <td>CVE-2024-40185</td>
    <td>CVE-2024-40186</td>
    <td>CVE-2024-40187</td>
</tr>
<tr>
    <td>CVE-2024-40188</td>
    <td>CVE-2024-40189</td>
    <td>CVE-2024-40191</td>
    <td>CVE-2024-40192</td>
    <td>CVE-2024-40193</td>
    <td>CVE-2024-40194</td>
    <td>CVE-2024-40195</td>
    <td>CVE-2024-40196</td>
</tr>
<tr>
    <td>CVE-2024-40197</td>
    <td>CVE-2024-40198</td>
    <td>CVE-2024-40199</td>
    <td>CVE-2024-40200</td>
    <td>CVE-2024-40201</td>
    <td>CVE-2024-40202</td>
    <td>CVE-2024-40203</td>
    <td>CVE-2024-40204</td>
</tr>
<tr>
    <td>CVE-2024-40205</td>
    <td>CVE-2024-40206</td>
    <td>CVE-2024-40207</td>
    <td>CVE-2024-40208</td>
    <td>CVE-2024-40209</td>
    <td>CVE-2024-40210</td>
    <td>CVE-2024-40211</td>
    <td>CVE-2024-40212</td>
</tr>
<tr>
    <td>CVE-2024-40213</td>
    <td>CVE-2024-40214</td>
    <td>CVE-2024-40215</td>
    <td>CVE-2024-40216</td>
    <td>CVE-2024-40217</td>
    <td>CVE-2024-40218</td>
    <td>CVE-2024-40219</td>
    <td>CVE-2024-40220</td>
</tr>
<tr>
    <td>CVE-2024-40221</td>
    <td>CVE-2024-40222</td>
    <td>CVE-2024-40223</td>
    <td>CVE-2024-40224</td>
    <td>CVE-2024-40225</td>
    <td>CVE-2024-40226</td>
    <td>CVE-2024-40227</td>
    <td>CVE-2024-40228</td>
</tr>
<tr>
    <td>CVE-2024-40229</td>
    <td>CVE-2024-40230</td>
    <td>CVE-2024-40231</td>
    <td>CVE-2024-40232</td>
    <td>CVE-2024-40233</td>
    <td>CVE-2024-40234</td>
    <td>CVE-2024-40235</td>
    <td>CVE-2024-40236</td>
</tr>
<tr>
    <td>CVE-2024-40237</td>
    <td>CVE-2024-31681</td>
    <td>CVE-2024-31682</td>
    <td>CVE-2024-31683</td>
    <td>CVE-2024-31684</td>
    <td>CVE-2024-31685</td>
    <td>CVE-2024-31686</td>
    <td>CVE-2024-31687</td>
</tr>
<tr>
    <td>CVE-2024-31688</td>
    <td>CVE-2024-31689</td>
    <td>CVE-2024-31690</td>
    <td>CVE-2024-31691</td>
    <td>CVE-2024-31692</td>
    <td>CVE-2024-31693</td>
    <td>CVE-2024-31694</td>
    <td>CVE-2024-31695</td>
</tr>
<tr>
    <td>CVE-2024-31696</td>
    <td>CVE-2024-31697</td>
    <td>CVE-2024-31698</td>
    <td>CVE-2024-31699</td>
    <td>CVE-2024-31700</td>
    <td>CVE-2024-31701</td>
    <td>CVE-2024-31702</td>
    <td>CVE-2024-31703</td>
</tr>
</table>


The following is a list of the received CNVD IDs:

<table>
<tr>
    <td>CNVD-2023-19675</td>
    <td>CNVD-2023-56454</td>
    <td>CNVD-2023-66432</td>
    <td>CNVD-2023-68099</td>
    <td>CNVD-2023-69760</td>
    <td>CNVD-2023-68954</td>
    <td>CNVD-2023-68953</td>
    <td>CNVD-2023-85534</td>
</tr>
<tr>
    <td>CNVD-2023-86947</td>
    <td>CNVD-2023-86945</td>
    <td>CNVD-2023-94843</td>
    <td>CNVD-2023-94481</td>
    <td>CNVD-2023-101432</td>
    <td>CNVD-2024-07356</td>
    <td>CNVD-2024-03058</td>
    <td>CNVD-2024-03059</td>
</tr>
<tr>
    <td>CNVD-2024-22620</td>
    <td>CNVD-2024-23119</td>
    <td>CNVD-2024-23121</td>
</tr>
</table>
