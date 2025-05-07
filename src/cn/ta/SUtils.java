package cn.ta;

import java.io.*;
import java.nio.charset.Charset;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

public class SUtils {
    public static void checkAssertion() throws UnexpectedException {
        boolean assertCatched = false;
        try {
            assert(false);
        }
        catch (AssertionError e) {
            assertCatched = true;
        }
        if(!assertCatched) {
            throw new UnexpectedException("Assertion not enabled, need -ea option");
        }
    }

    public static boolean isWinOS() {
        return System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;
    }

    public static List<String> getRegMatch(String regExp, String input) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(input);
        if(matcher.find()) {
            List<String> results = new LinkedList<>();
            for(int i = 0; i < matcher.groupCount(); ++i) {
                String group = matcher.group(i + 1);
                results.add(group);
            }
            return results;
        }
        return null;
    }

    public static boolean isRegMatch(String regExp, String input) {
        Pattern pattern = Pattern.compile(regExp);
        return pattern.matcher(input).find();
    }

    public static boolean isRegMatchAll(String regExp, String input) {
        Pattern pattern = Pattern.compile(regExp);
        return pattern.matcher(input).matches();
    }

    public static String getRegMatchFirst(String regExp, String input) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(input);
        if(matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    public static List<String> getRegMatchAll(String regExp, String input) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(input);
        List<String> results = new LinkedList<>();
        while(matcher.find()) {
            String r = matcher.group();
            results.add(r);
        }
        return results;
    }

    public static String regRepFirst(String regExp, String input, String replacement) {
        return input.replaceFirst(regExp, replacement);
    }

    public static String regRepAll(String regExp, String input, String replacement) {
        return input.replaceAll(regExp, replacement);
    }

    public static String replaceStr(String str, String r1, String r2) {
        return regRepAll(r1, str, r2);
    }

    // 去掉点
    public static String NDOT(String input) {
        // 转换开头的./
        while(SUtils.isRegMatch("^\\./", input)) {
            input = SUtils.regRepAll("^\\./", input, "");
        }

        while(SUtils.isRegMatch("/\\./", input)) {
            input = SUtils.regRepAll("/\\./", input, "/");
        }

        while(SUtils.isRegMatch("[^\\:\\\\/]+/\\.\\./", input)) {
            input = SUtils.regRepAll("[^\\:\\\\/]+/\\.\\./", input, "");
        }

        return input;
    }

    // 转换为标准路径
    public static String P(String input) {
        input = SUtils.regRepAll("[\\\\/]+", input, "/");
        input = SUtils.NDOT(input);
        return input;
    }

    // 转换为目录名称
    public static String D(String input) {
        input = SUtils.regRepAll("[\\\\/]+", input, "/");
        input = SUtils.regRepAll("[\\\\/]+$", input, "");
        input = input + "/";
        input = SUtils.NDOT(input);
        return input;
    }

    // 转换为文件名称
    public static String F(String input) {
        input = SUtils.regRepAll("[\\\\/]+", input, "/");
        input = SUtils.regRepAll("[\\\\/]+$", input, "");
        input = SUtils.NDOT(input);
        return input;
    }

    // 转换为本地操作系统路径名称(不进行转码)
    public static String OP(String input) {
        if(SUtils.isWinOS()) {
            input = SUtils.regRepAll("[\\\\/]", input, "\\\\");
        }
        else {
            input = SUtils.regRepAll("[\\\\/]", input, "/");
        }
        return input;
    }

    // 编码成操作系统路径名
    public static String EOP (String input) {
        if(SUtils.isWinOS()) {
            input = SUtils.regRepAll("[\\\\/]", input, "\\\\");
            //$path = Encode::encode("gbk", $path);
        }
        else {
            input = SUtils.regRepAll("[\\\\/]", input, "/");
            //$path = Encode::encode("utf8", $path);
        }
        return input;
    }

    // 从操作系统路径名解码
    public static String DOP(String input) {
        if(SUtils.isWinOS()) {
            //$path = Encode::decode("gbk", $path);
        }
        else {
            //$path = Encode::decode("utf8", $path);
        }
        input = SUtils.regRepAll("[\\\\/]", input, "/");
        return input;
    }

    public static boolean fileExists(String name) {
        File file = new File(name);
        return file.exists() && file.isFile();
    }

    public static boolean dirExists(String name) {
        File file = new File(name);
        return file.exists() && file.isDirectory();
    }

    public static boolean exists(String name) {
        File file = new File(name);
        return file.exists();
    }

    public static String convertToRelative(String home, String path) {
        if(isAbsolute(home) && isAbsolute(path)) {
            try {
                home = new File(home).getCanonicalPath();
                path = new File(path).getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
            home = home + "/";
            path = path + "/";
            if(path.startsWith(home)) {
                return path.substring(home.length());
            }
            else {
                return path;
            }
        }
        else {
            return path;
        }
    }

    private static boolean isAbsolute(String input) {
        if(isRegMatch("^[\\\\/]", input)) {
            return false;
        }
        else if(isRegMatch("^[a-zA-Z]:", input)) {
            return true;
        }
        else {
            return false;
        }
    }

    public static String WD() {
        try {
            return SUtils.D(new File(".").getCanonicalPath());
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println(-1);
            return null;
        }
    }

    // 转换为绝对路径
    private static String AP(String inputDir, String workingDir) {
        if(workingDir == null) {
            workingDir = WD();
        }
        if(!isAbsolute(inputDir)) {
            inputDir = new File(workingDir, inputDir).getAbsolutePath();
        }
        if(isRegMatch("[\\\\/]/$", inputDir)) {
            inputDir = SUtils.D(inputDir);
        }
        else {
            inputDir = SUtils.F(inputDir);
        }
        return inputDir;
    }

    //# 转换为相对路径
    private static String RP(String inputDir, String workingDir) {
        if(workingDir == null) {
            workingDir = WD();
        }
        if(isAbsolute(inputDir)) {
            inputDir = SUtils.convertToRelative(SUtils.F(workingDir), SUtils.F(inputDir));
        }
        inputDir = SUtils.P(inputDir);
        return inputDir;
    }

    // 转换为绝对目录
    public static String AD(String inputDir) {
        return AD(inputDir, WD());
    }

    public static String AD(String inputDir, String workingDir) {
        return SUtils.D(SUtils.AP(inputDir, workingDir));
    }

    //# 转换为相对目录
    public static String RD(String inputDir, String workingDir) {
        return SUtils.D(SUtils.RP(inputDir, workingDir));
    }

    // 转换为绝对文件
    public static String AF(String inputDir, String workingDir) {
        return SUtils.F(SUtils.AP(inputDir, workingDir));
    }

    // 转换为相对文件
    public static String RF(String inputDir, String workingDir) {
        return SUtils.F(SUtils.RP(inputDir, workingDir));
    }

    public static String getBaseNameWithoutExtName(String pathName) {
        return pathName.replaceFirst("^.*[\\\\/]+", "").replaceFirst("\\.\\w+$", "");
    }

    private static boolean isValidFileOfListingFiles(String name, String[] ignores, String[] matches) {
        // 存在忽略条件时
        if(ignores != null && ignores.length > 0) {
            for(int i = 0; i < ignores.length; ++i) {
                if(isRegMatch(ignores[i], name)) {
                    return false;
                }
            }
        }

        // 存在匹配条件时需要检查
        if(matches != null && matches.length > 0) {
            for(int i = 0; i < matches.length; ++i) {
                if(isRegMatch(matches[i], name)) {
                    return true; // 与一个匹配达成时
                }
            }
            return false; // 与任何匹配都未达成时
        }

        return true;
    }

    private static void listAllFilesEx(List<String> results, String folder, String root,  boolean deep, String[] ignores, String[] matches) throws IOException {
        File[] files = new File(SUtils.EOP(folder)).listFiles();
        if(files != null) {
            List<String> folders = new LinkedList<>();
            for (File file : files) {
                String fullName = DOP(file.getCanonicalPath());
                if(!fullName.startsWith(folder)) {
                    throw new UnexpectedException("fatal error");
                }
                String baseName = fullName.substring(folder.length());
                if(baseName.equals(".") || baseName.equals("..")) {
                    throw new UnexpectedException("fatal error");
                }
                else if (file.isDirectory()) {
                    folders.add(fullName);
                } else if(file.isFile()) {
                    if(isValidFileOfListingFiles(baseName, ignores, matches)) {
                        results.add(fullName);
                    }
                }
                else {
                    throw new UnexpectedException("impossible " + fullName);
                }
            }

            if(deep) {
                for(int i = 0; i < folders.size(); ++i) {
                    String childFolder = folders.get(i) + "/";
                    listAllFilesEx(results, childFolder, root, deep, ignores, matches);
                }
            }
        }
    }

    public static List<String> listAllFiles(String inputDir, boolean deep, String[] ignores, String[] matches) throws IOException {
        List<String> results = new LinkedList<>();
        listAllFilesEx(results, inputDir, inputDir, deep, ignores, matches);
        return results;
    }

    public static List<String> listAllFiles(String inputDir, boolean deep, String[] ignores) throws IOException {
        return listAllFiles(inputDir, deep, ignores, null);
    }

    public static List<String> listAllFiles(String inputDir, boolean deep) throws IOException {
        return listAllFiles(inputDir, deep, null, null);
    }

    public static boolean deleteFile(String fileName) {
        return new File(fileName).delete();
    }

    public static void saveFile(String fileName, String content, String charset) throws IOException {
        saveFile(fileName, content.getBytes(Charset.forName(charset)));
    }

    public static void saveFile(String fileName, String content) throws IOException {
        saveFile(fileName, content, "UTF8");
    }

    public static void saveFile(String fileName, List<String> lines) throws IOException {
        saveFile(fileName, StringUtils.join(lines,"\n"));
    }

    public static void saveFile(String fileName, List<String> lines, String charset) throws IOException {
        saveFile(fileName, StringUtils.join(lines, "\n"), charset);
    }

    public static void saveFile(String fileName, byte[] bytes) throws IOException {
        File file = new File(EOP(fileName));
        if(!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream os = new FileOutputStream(file);
        os.write(bytes);
        os.close();
    }

    public static String encodeJson(JSONObject json, int pretty) {
        return json.toString(pretty);
    }

    public static String encodeJson(JSONArray json, int pretty) {
        return json.toString(pretty);
    }

    public static boolean makeDirAll(String dir) {
        if(dir.endsWith("/") || dir.endsWith("\\")) {
            return new File(dir).mkdirs();
        }
        else {
            dir = dir.replaceFirst("[\\/][^\\/]*?$", "");
            return new File(dir).mkdirs();
        }
    }

    public static List<String> runCmd(String cmd) {
        return runCmd(cmd, null);
    }

    public static List<String> runCmd(String cmd, String path) {
        List<String> results = new LinkedList<>();
        File dir = new File(path == null ? "." : path);
        try {
            Process ps = Runtime.getRuntime().exec(cmd, null, dir);
            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream(), Charset.forName(isWinOS() ? "GBK" : "UTF8")));
            String line = null;
            while ((line = br.readLine()) != null) {
                results.add(line);
            }
            br.close();
            ps.waitFor();
            return results;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getSVNRevision(String dir) throws IOException {
        if(dir == null) {
            dir = WD();
        }
        dir = D(dir);

        List<String> lines = runCmd("svn info " + dir);
        if(lines == null || lines.size() == 0) {
            lines = runCmd("\"C:\\Program Files\\TortoiseSVN\\bin\\svn.exe\" info " + dir);
        }

        if(lines == null || lines.size() != 13) {
            throw new UnexpectedException("runCmd failed");
        }

        String rev = null;
        List<String> matchResults = getRegMatch(":\\s+(\\d+)$", lines.get(10));
        if(matchResults != null) {
            rev = matchResults.get(0);
        }

        if(rev == null) {
            throw new UnexpectedException("impossible");
        }

        return rev;
    }

    public static String lcfirst(String s) {
        if(s.isEmpty()) {
            return s;
        }
        else {
            return s.substring(0, 1).toLowerCase() + s.substring(1);
        }
    }

    public static String ucfirst(String s) {
        if(s.isEmpty()) {
            return s;
        }
        else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    // 读取excel
    public static Workbook readExcel(String filePath){
        Workbook wb = null;
        if(filePath == null){
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
            if(filePath.endsWith(".xls")) {
                return wb = new HSSFWorkbook(is);
            }
            else if(filePath.endsWith(".xlsx")) {
                return wb = new XSSFWorkbook(is);
            }
            else {
                return wb = null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wb;
    }
}
