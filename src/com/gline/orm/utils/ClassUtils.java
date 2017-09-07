package com.gline.orm.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.gline.orm.base.ITable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

/**
 * 类相关的工具类
 */
public class ClassUtils {

    public static interface IClassFilter {
        boolean filter(Class<?> clazz);
    }

    public static final IClassFilter TABLE_FILTER = new IClassFilter() {
        @Override
        public boolean filter(Class<?> clazz) {
            ITable cTableName = clazz.getAnnotation(ITable.class);
            if (cTableName == null) {
                return false;
            }
            return true;
        }
    };

    /**
     * 取得某个接口下所有实现这个接口的类
     */
    public static final List<Class<?>> getAllClassByInterface(Class<?> c, IClassFilter filter) {
        List<Class<?>> returnClassList = null;
        if (c.isInterface()) {
            String packageName = c.getPackage().getName();
            List<Class<?>> allClass = getAllClasses(packageName, filter);
            if (allClass != null) {
                returnClassList = new ArrayList<Class<?>>();
                for (Class<?> clazz : allClass) {
                    if (c.isAssignableFrom(clazz)) {
                        if (!c.equals(clazz)) {
                            returnClassList.add(clazz);
                        }
                    }
                }
            }
        }
        return returnClassList;
    }

    public static final List<Class<?>> getAllClasses(Context context, IClassFilter filter) {
        List<Class<?>> classes = new ArrayList<>();
        ApplicationInfo ai = context.getApplicationInfo();
        DexFile dex;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ai.splitSourceDirs != null && ai.splitSourceDirs.length > 0) {
                for (String classPath : ai.splitSourceDirs) {
                    try {
                        dex = new DexFile(classPath);
                        Enumeration<String> apkClassNames = dex.entries();
                        while (apkClassNames.hasMoreElements()) {
                            String className = apkClassNames.nextElement();
                            try {
                                Class<?> entryClass = context.getClassLoader().loadClass(className);
                                if (entryClass != null && (filter == null || filter.filter(entryClass))) {
                                    classes.add(entryClass);
                                }
                            } catch (ClassNotFoundException e) {
                                continue;
                            }
                        }
                    } catch (IOException e) {
                        continue;
                    }
                }
            }
        }
        try {
            dex = new DexFile(ai.sourceDir);
            Enumeration<String> apkClassNames = dex.entries();
            while (apkClassNames.hasMoreElements()) {
                String className = apkClassNames.nextElement();
                try {
                    Class<?> entryClass = context.getClassLoader().loadClass(className);
                    if (entryClass != null && (filter == null || filter.filter(entryClass))
                            && !classes.contains(entryClass)) {
                        classes.add(entryClass);
                    }
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
        } catch (IOException e) {
        }
        return classes;
    }

    public static final List<Class<?>> getAllClasses(IClassFilter filter) {
        List<Class<?>> classes = new ArrayList<>();
        Field dexField;
        try {
            dexField = BaseDexClassLoader.class.getDeclaredField("mDexs");
        } catch (NoSuchFieldException e) {
            return classes;
        }
        dexField.setAccessible(true);
        PathClassLoader classLoader = (PathClassLoader) Thread.currentThread().getContextClassLoader();
        DexFile[] dexFiles;
        try {
            dexFiles = (DexFile[]) dexField.get(classLoader);
        } catch (IllegalAccessException e) {
            return classes;
        }
        for (DexFile dex : dexFiles) {
            Enumeration<String> entries = dex.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement();
                Class<?> entryClass = dex.loadClass(entry, classLoader);
                if (entryClass != null && (filter == null || filter.filter(entryClass))) {
                    classes.add(entryClass);
                }
            }
        }
        return classes;
    }

    /**
     * 从包package中获取所有的Class
     *
     * @param packageName
     * @param filter
     * @return
     */
    public static final List<Class<?>> getAllClasses(String packageName, IClassFilter filter) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        boolean recursive = true;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findClassesInPackageByFile(packageName, filePath, recursive, classes, filter);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                if (idx != -1) {
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                if ((idx != -1) || recursive) {
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            Class<?> clazz = Class.forName(packageName + '.' + className);
                                            if (filter == null || filter.filter(clazz)) {
                                                classes.add(clazz);
                                            }
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    public static final void findClassesInPackageByFile(String packageName, String packagePath,
                                                        final boolean recursive, List<Class<?>> classes,
                                                        IClassFilter filter) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirs = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirs) {
            if (file.isDirectory()) {
                findClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes, filter);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(packageName + '.' + className);
                    if (filter == null || filter.filter(clazz)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}