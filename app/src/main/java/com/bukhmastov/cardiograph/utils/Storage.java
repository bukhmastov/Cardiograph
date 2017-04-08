package com.bukhmastov.cardiograph.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class Storage {

    private static final String TAG = "Storage";
    private static final String APP_FOLDER = "app_data";
    private static final String APP_EXTENSION = "cardio";

    public static class file {

        @Nullable
        public static FileOutputStream openWriteStream(Context context, String path){
            try {
                File file = new File(getFileLocation(context, path, true));
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    if (!file.createNewFile()) {
                        throw new Exception("Failed to create file: " + file.getPath());
                    }
                }
                //Log.d(TAG, "openWriteStream " + file.getPath());
                return new FileOutputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        public static boolean writeToStream(FileOutputStream fileOutputStream, byte[] data) {
            for (byte d : data) {
                if (!Storage.file.writeToStream(fileOutputStream, d)) {
                    return false;
                }
            }
            return true;
        }
        public static boolean writeToStream(FileOutputStream fileOutputStream, byte data){
            try {
                fileOutputStream.write(data);
                //Log.d(TAG, "writeToStream " + (int) data);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                closeWriteStream(fileOutputStream);
                return false;
            }
        }
        public static boolean closeWriteStream(FileOutputStream fileOutputStream){
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
                //Log.d(TAG, "closeWriteStream");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public static byte[] get(Context context, String path) {
            return Storage.file.get(context, path, true);
        }
        public static byte[] get(Context context, String path, boolean withExtension){
            try {
                File file = new File(getFileLocation(context, path, withExtension));
                if (!file.exists() || file.isDirectory()) {
                    throw new Exception("File does not exist: " + file.getPath());
                }
                byte[] bytes = new byte[(int) file.length()];
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                buf.read(bytes, 0, bytes.length);
                buf.close();
                return bytes;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String[] getListOfFiles(Context context, String path){
            ArrayList<String> list = getArrayListOfFiles(context, path);
            return list.toArray(new String[list.size()]);
        }
        public static ArrayList<String> getArrayListOfFiles(Context context, String path){
            ArrayList<String> list = new ArrayList<>();
            try {
                File directory = new File(getFileLocation(context, path, false));
                if (!directory.exists() || !directory.isDirectory()) {
                    throw new Exception("Directory does not exist: " + directory.getPath());
                }
                File[] files = directory.listFiles();
                for (File file : files) {
                    list.add(file.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }
        public static boolean delete(Context context, String path){
            try {
                File file = new File(getFileLocation(context, path, true));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        public static boolean clear(Context context){
            try {
                File file = new File(getCoreLocation(context));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        public static boolean clear(Context context, String path){
            try {
                File file = new File(getFileLocation(context, path, false));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        public static boolean exists(Context context, String path){
            try {
                File file = new File(getFileLocation(context, path, true));
                return file.exists();
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean deleteRecursive(File fileOrDirectory) {
            boolean result = true;
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    if (!deleteRecursive(child)) {
                        result = false;
                    }
                }
            }
            if (!fileOrDirectory.delete()) {
                result = false;
            }
            return result;
        }
        public static String getFileLocation(Context context, String path, boolean isFile) {
            if (!path.isEmpty()) path = ("#" + path + (isFile ? "." + APP_EXTENSION : "")).replace("#", File.separator);
            return getCoreLocation(context) + path;
        }
        public static String getCoreLocation(Context context) {
            return context.getFilesDir() + File.separator + APP_FOLDER;
        }

    }
    public static class pref {
        public static synchronized void put(Context context, String key, String value){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
        }
        public static synchronized void put(Context context, String key, int value){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
        }
        public static synchronized void put(Context context, String key, boolean value){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
        }
        public static String get(Context context, String key){
            return pref.get(context, key, "");
        }
        public static String get(Context context, String key, String def){
            return PreferenceManager.getDefaultSharedPreferences(context).getString(key, def);
        }
        public static int get(Context context, String key, int def){
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, def);
        }
        public static boolean get(Context context, String key, boolean def){
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, def);
        }
        public static void delete(Context context, String key){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.contains(key)) sharedPreferences.edit().remove(key).apply();
        }
        public static void clear(Context context){
            pref.clear(context, Pattern.compile(".*"));
        }
        public static void clear(Context context, Pattern pattern){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> list = sharedPreferences.getAll();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (Map.Entry<String, ?> entry : list.entrySet()) {
                if (pattern.matcher(entry.getKey()).find()) {
                    String key = entry.getKey();
                    editor.remove(key);
                }
            }
            editor.apply();
        }
    }

}
