package cg;

import soot.SootClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UserInputApi {
    public static HashMap<String, String> inputApis = new HashMap<>(); //class -> method.subsignature
    public static Set<String> AndroidCallbackSet = new HashSet<>();

    static {
        inputApis.put("android.view.View$OnClickListener", "void onClick(android.view.View)");
        inputApis.put("android.view.View$OnLongClickListener", "boolean onLongClick(android.view.View)");
        inputApis.put("android.view.View$OnFocusChangeListener", "void onFocusChange(android.view.View,boolean)");
        inputApis.put("android.view.View$OnFocusChangedListener", "void onFocusChanged(android.view.View,boolean)");
        inputApis.put("android.view.View$OnKeyListener", "boolean onKey(android.view.View,int,android.view.KeyEvent)");
        inputApis.put("android.view.View$OnKeyDownListener", "boolean onKeyDown(int,android.view.KeyEvent)");
        inputApis.put("android.view.View$OnKeyUpListener", "boolean onKeyUp(int,android.view.KeyEvent)");
        inputApis.put("android.view.View$OnKeyLongPressListener", "void onKeyLongPress(android.view.View)");
        inputApis.put("android.view.View$OnTouchEventListener", "boolean onTouchEvent(android.view.MotionEvent)");
        inputApis.put("android.view.View$OnTouchListener", "boolean onTouch(android.view.View,android.view.MotionEvent)");
        inputApis.put("android.view.View$OnHoverListener", "boolean onHover(android.view.View,android.view.MotionEvent)");
        inputApis.put("android.view.View$OnDragListener", "boolean onDrag(android.view.View,android.view.DragEvent)");
        inputApis.put("android.view.View$OnGenericMotionListener", "boolean onGenericMotion(android.view.View,android.view.MotionEvent)");
        inputApis.put("android.view.View$OnSystemUiVisibilityChangeListener", "void onSystemUiVisibilityChange(int)");
        inputApis.put("android.view.View$OnScrollChangeListener", "void onScrollChange(android.view.View,int,int,int,int)");
        inputApis.put("android.view.View$OnContextClickListener", "boolean onContextClick(android.view.View)");

        inputApis.put("android.widget.CompoundButton$OnCheckedChangeListener","void onCheckedChanged(android.widget.CompoundButton,boolean)");
        inputApis.put("android.widget.RadioGroup$OnCheckedChangeListener","void onCheckedChanged(android.widget.RadioGroup,int)");
        inputApis.put("android.widget.Switch$OnCheckedChangeListener","void onCheckedChanged(android.widget.Switch,boolean)");
        inputApis.put("android.widget.Switch$OnTouchEventListener","boolean onTouchEvent(android.view.MotionEvent)");

        inputApis.put("com.google.android.material.button.MaterialButton$OnCheckedChangeListener","void onCheckedChanged(com.google.android.material.button.MaterialButton,boolean)");
        inputApis.put("com.google.android.material.card.MaterialCardView$OnCheckedChangeListener","void onCheckedChanged(com.google.android.material.card.MaterialCardView,boolean)");
        inputApis.put("com.google.android.material.chip.ChipGroup$OnCheckedChangeListener","void onCheckedChanged(com.google.android.material.chip.ChipGroup,boolean)");

        for(String api :inputApis.keySet()){
            String eventName = api.substring(api.indexOf('$')+1);
            AndroidCallbackSet.add("void set"+eventName+"("+api+")");
        }
    }
    public static boolean isListener(SootClass sootClass) {
        for (SootClass iface : sootClass.getInterfaces()) {
            if (inputApis.containsKey(iface.getName())) {
                return true;
            }
        }
        return false;
    }

    public static String getCallbackMethodName(SootClass sootClass) {
        for (SootClass iface : sootClass.getInterfaces()) {
            if (inputApis.containsKey(iface.getName())) {
                return inputApis.get(iface.getName());
            }
        }
        return null;
    }

    public static String getCallbackMethodName(SootClass sootClass, String setCallbackStr){
        String interfaceName = setCallbackStr.substring(setCallbackStr.indexOf('(')+1,setCallbackStr.indexOf(')'));
        String eventName = inputApis.get(interfaceName);
        for(SootClass iface : sootClass.getInterfaces()){
            if(iface.getName().equals(interfaceName)){
                return eventName;
            }
        }
        return null;
    }
}
