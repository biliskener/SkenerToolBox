package cn.ta;

import java.io.IOException;

public abstract class BaseCodeGenerator {
    protected String inputDir;

    public abstract void cleanDirs() throws IOException;

    public abstract void saveAllFiles() throws IOException;

    protected String Q(String in) {
        return "\"" + escapeStringForLua(in) + "\"";
    }

    protected String escapeStringForLua(String in) {
        in = in.replaceAll("\\\\", "\\\\\\\\");
        in = in.replaceAll("\"", "\\\\\\\"");
        in = in.replaceAll("\'", "\\\\\\\'");
        in = in.replaceAll("\r?\n", "\\\\n");
        return in;
    }
}
