package com.shellever.dexclassloader;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.shellever.plugin.PluginIf;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DexClassLoader";

    private TextView mLoaderResultTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoaderResultTv = findViewById(R.id.tv_loader_result);

        // 使用接口方式调用dex里面的方法
        Log.d(TAG, "testDexClassLoaderWithIf()");
        testDexClassLoaderWithIf();

        // 使用反射方式调用dex里面的方法
        Log.d(TAG, "testDexClassLoaderWithReflect()");
        testDexClassLoaderWithReflect();
    }

    public void testDexClassLoaderWithReflect() {
        HashMap<String, DeviceInfo> deviceInfo = parseDeviceInfo(); // 解析xml定义文件

        // 加载接口具体实现的经过dex转换过的jar包
        DexClassLoader dexClassLoader = getClassLoader("pluginDevInfo.jar");
        // 获取COMMON模块构造方法及参数
        DeviceInfo localDeviceInfo = deviceInfo.get("COMMON");
        String className = localDeviceInfo.name;
        String classArgs = localDeviceInfo.args;
        Log.d(TAG, "className = " + className);

        // 获取COMMON模块接口方法及参数
        DeviceInfo localDeviceInfo2 = deviceInfo.get("COMMON" + "getDeviceInfo");
        String functionName = localDeviceInfo2.name;
        String functionArgs = localDeviceInfo2.args;
        Log.d(TAG, "functionName = " + functionName);
        try {
            Class pluginDevInfoClazz = dexClassLoader.loadClass(className);
            Object localObject = pluginDevInfoClazz.getConstructor(getParamType(classArgs)).newInstance(new Object[]{this});
            Method localMethod = localObject.getClass().getDeclaredMethod(functionName, getParamType(functionArgs));
            String returnType = localMethod.getReturnType().getSimpleName(); // 获取方法返回值类型
            if("String".equals(returnType)){
                // 通过反射调用接口方法
                String devinfo = (String) localMethod.invoke(localObject, new Object[0]);
                mLoaderResultTv.append("\n" + devinfo);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, DeviceInfo> parseDeviceInfo(){
        HashMap<String, DeviceInfo> deviceInfo = new HashMap<>();
        XmlResourceParser parser = getResources().getXml(R.xml.device_class_module);
        try {
            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        if("module".equals(parser.getName())){
                            DeviceInfo localDeviceInfo = new DeviceInfo();
                            localDeviceInfo.name = parser.getAttributeValue(null, "name");
                            localDeviceInfo.args = parser.getAttributeValue(null, "args");
                            deviceInfo.put(parser.getAttributeValue(null, "id"), localDeviceInfo);
                        }
                        if("function".equals(parser.getName())){
                            DeviceInfo localDeviceInfo = new DeviceInfo();
                            localDeviceInfo.name = parser.getAttributeValue(null, "name");
                            localDeviceInfo.args = parser.getAttributeValue(null, "args");
                            deviceInfo.put(parser.getAttributeValue(null, "id") + localDeviceInfo.name, localDeviceInfo);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deviceInfo;
    }

    public class DeviceInfo {
        public String args;
        public String name;
    }

    public class retResult  {
        public byte[] bytes;
        public String[] strings;
        private String string;

        public retResult() {}
    }

    private Class<?>[] getParamType(String paramString) {
        if ((paramString == null) || ("".equals(paramString))) {
            return null;
        }
        String[] paramStringArray = paramString.split(" ");
        Class[] arrayOfClass = new Class[paramStringArray.length];
        int i = 0;
        while (i < paramStringArray.length) {
            arrayOfClass[i] = getClassType(paramStringArray[i]);
            i += 1;
        }
        return arrayOfClass;
    }

    private Class<?> getClassType(String paramString) {
        if ("Context".equals(paramString)) {
            return Context.class;
        }
        if ("String".equals(paramString)) {
            return String.class;
        }
        if ("Int".equals(paramString)) {
            return Integer.TYPE;
        }
        if ("StringArray".equals(paramString)) {
            return String[].class;
        }
        if ("ByteArray".equals(paramString)) {
            return byte[].class;
        }
        return null;
    }

    // =============================================================================================
    public void testDexClassLoaderWithIf() {
        // 加载接口具体实现的经过dex转换过的jar包
        DexClassLoader dexClassLoader = getClassLoader("pluginImpl.jar");
        try {
            // 加载接口具体实现类
            Class pluginImplClazz = dexClassLoader.loadClass("com.shellever.plugin.PluginImpl");
            PluginIf pluginIf = (PluginIf) pluginImplClazz.newInstance();   // 直接强制类型转换
            String devInfo = pluginIf.getDeviceInfo();  // 调用接口方法
            mLoaderResultTv.setText(devInfo);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private DexClassLoader getClassLoader(String paramString) {
        String dexPath = new File(getAppJarDir(), paramString).getAbsolutePath(); // 经过dex转码后的jar包存放路径
        String optimizedDirectory = getAppDexDir().getAbsolutePath();   // dex优化文件存放路径
        String librarySearchPath = getAppLibDir().getAbsolutePath();    // 本地依赖库存放路径
        ClassLoader parent = getClassLoader();                          // 父类的类加载器
        return new DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent);
    }

    // Android/data/<package-name>/driver/lib/
    public File getAppLibDir() {
        File localFile = new File(getAppHomeDir(), "driver/lib/");
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        return localFile;
    }

    // /storage/emulated/0/Android/data/com.shellever.dexclassloader/driver/dex
    public File getAppDexDir() {
        File localFile = new File(getAppHomeDir(), "driver/dex/");
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        return localFile;
    }

    // Android/data/<package-name>/driver/jar/
    public File getAppJarDir() {
        File localFile = new File(getAppHomeDir(), "driver/jar/");
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        return localFile;
    }

    public File getAppHomeDir() {
        String packageName = getPackageName();
        File sdcardDir = Environment.getExternalStorageDirectory(); // /storage/emulated/0/
        String appHomeDir = sdcardDir + File.separator + "Android" + File.separator + "data" + File.separator + packageName + File.separator;
        File localFile = new File(appHomeDir);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        return localFile;
    }
}
