package fcm.layout;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.IOException;
import java.util.*;

public class ResourceUtil {
    private static LayoutFileParserForTextExtraction lfpft;
    public static Map<String, LayoutTextTreeNode> textTreeMap = new HashMap<String, LayoutTextTreeNode>();
    public static Map<String, Set<String>> actionToReceiverMap = new HashMap<>();
    public static Map<Integer, LayoutTextTreeNode> id2Node = new HashMap<Integer, LayoutTextTreeNode>();
    private static ProcessManifest processMan= null;

    public static void init(String apkPath){
        try {
            processMan = new ProcessManifest(apkPath);
        } catch (IOException | XmlPullParserException e) {
            return;
        }

        ARSCFileParser resParser = new ARSCFileParser();
        try {
            resParser.parse(apkPath);
        } catch (Exception e) {
            System.err.println("NULIST: failed to init FlowTriggerEventAnalyzer: ARSCFileParser");
            //e.printStackTrace();
        }
        String packageName = processMan.getPackageName();
        lfpft = new LayoutFileParserForTextExtraction(packageName,resParser);
        lfpft.parseLayoutFileForTextExtraction(apkPath);
        lfpft.findClassLayoutMappings();
        textTreeMap = lfpft.getTextTreeMap();
        id2Node = lfpft.getId2Node();
    }

    public static Map<String, Set<String>> getActionToReceiverMap(){
        if(processMan==null)
            return actionToReceiverMap;
        List<AXmlNode> receivers = processMan.getReceivers();
        receivers.addAll(processMan.getServices());
        receivers.addAll(processMan.getActivities());
        for(AXmlNode receiverNode: receivers){
            String receiverName = (String) receiverNode.getAttribute("name").getValue();
            for(AXmlNode intentNode: receiverNode.getChildren()){
                if(!intentNode.getTag().equals("intent-filter"))
                    continue;
                for(AXmlNode actionNode : intentNode.getChildren()){
                    if(!actionNode.getTag().equals("action"))
                        continue;
                    String actionName = (String) actionNode.getAttribute("name").getValue();
                    if(actionToReceiverMap.containsKey(actionName)){
                        actionToReceiverMap.get(actionName).add(receiverName);
                    }else {
                        Set<String> valueSet = new HashSet<>();
                        valueSet.add(receiverName);
                        actionToReceiverMap.put(actionName,valueSet);
                    }

                }
            }
        }
        return actionToReceiverMap;
    }

    public static String getResourceNameBaseOnId(int id){
        return lfpft.getResourceNameBaseOnId(id);
    }

    public static Set<String> getLayoutTexts(String className){
        Set<Integer> res = lfpft.layoutClasses.get(className);
        Set<String> texts = new HashSet<>();
        if(res!=null){
           // System.out.println("find： "+className+" "+ Arrays.toString(res.toArray()));
            for(Integer id:res){
                String layoutName = lfpft.getResourceNameBaseOnId(id);
                if(!layoutName.equals("")){
                    LayoutTextTreeNode node = textTreeMap.get(layoutName.trim());
                    if(node==null)
                        continue;
                    texts.addAll(node.extractTexts());
                }
            }
        }
        return texts;
    }

    public static Set<String> getTextStringBasedOnID(int id){
        return lfpft.getTextStringBasedOnID(id);
    }
}
