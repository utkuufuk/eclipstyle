import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Eclipstyle
{
    private static final String UI_WORKBENCH_PREFS_FILENAME = "org.eclipse.ui.workbench.prefs";
    private static final String UI_EDITORS_PREFS_FILENAME = "org.eclipse.ui.editors.prefs";
    private static final String JDT_CORE_PREFS_FILENAME = "org.eclipse.jdt.core.prefs";
    private static final String JDT_UI_PREFS_FILENAME = "org.eclipse.jdt.ui.prefs";
    private static final String PRINT_MARGIN_KEY = "org.eclipse.jdt.core.formatter.lineSplit";
    private static final String SUB_DIR = "/.metadata/.plugins/org.eclipse.core.runtime/.settings/";

    private static List<String> preserve(List<String> lines, String... keys)
    {
        List<String> preservedLines = new ArrayList<String>();

        for (String line : lines)
        {
            for (String key : keys)
            {
                if (line.startsWith(key))
                {
                    preservedLines.add(line);
                    break;
                }
            }
        }
        return preservedLines;
    }

    private static List<String> eliminate(List<String> lines, String... keys)
    {
        List<String> preservedLines = new ArrayList<String>();

        for (String line : lines)
        {
            preservedLines.add(line);

            for (String key : keys)
            {
                if (line.startsWith(key))
                {
                    preservedLines.remove(line);
                    break;
                }
            }
        }
        return preservedLines;
    }

    private static void updateFormatter(String wspace, List<String> coreLines, List<String> uiLines) 
        throws IOException
    {
        // format JDT UI prefs
        Path path = Paths.get(wspace + SUB_DIR + JDT_UI_PREFS_FILENAME);
        List<String> lines = Files.readAllLines(path);
        eliminate(lines, "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles=");
        Files.delete(path);
        Files.write(path, lines, Charset.forName("UTF-8"));
        Files.write(path, uiLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        // format JDT CORE prefs
        path = Paths.get(wspace + SUB_DIR + JDT_CORE_PREFS_FILENAME);
        lines = Files.readAllLines(path);
        eliminate(lines, "org.eclipse.jdt.core.formatter");
        Files.delete(path);
        Files.write(path, lines, Charset.forName("UTF-8"));
        Files.write(path, coreLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        // read print margin from JDT CORE prefs & set it in UI EDITORS prefs as well
        preserve(coreLines, PRINT_MARGIN_KEY);

        if (coreLines.isEmpty())
        {
            throw new IOException("Could not find print margin key: " + PRINT_MARGIN_KEY);
        }
        int margin = Integer.parseInt(lines.get(0).substring(lines.get(0).lastIndexOf("=") + 1));
        path = Paths.get(wspace + SUB_DIR + UI_EDITORS_PREFS_FILENAME);
        lines = Files.readAllLines(path);
        eliminate(lines, "printMarginColumn=");
        Files.delete(path);
        lines.add("printMarginColumn=" + margin);
        Files.write(path, lines, Charset.forName("UTF-8"));
    }

    private static void updateStyle(
        String wspace, Path uiEditorsPrefsSrc, Path uiWorkbenchPrefsSrc,
        Path jdtCorePrefsSrc, Path jdtUiPrefsSrc) throws IOException
    {
        Files.copy(
            uiWorkbenchPrefsSrc, Paths.get(wspace + SUB_DIR + UI_WORKBENCH_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
            uiEditorsPrefsSrc, Paths.get(wspace + SUB_DIR + UI_EDITORS_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
            jdtCorePrefsSrc, Paths.get(wspace + SUB_DIR + JDT_CORE_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
            jdtUiPrefsSrc, Paths.get(wspace + SUB_DIR + JDT_UI_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);       
    }

    public static void main(String[] args)
    {
        // evaluate source file paths to read formatter preferences
        int lastIdx = args[1].length() - 1;
        String srcWorkspace = args[1].charAt(lastIdx) == File.separatorChar ? 
            args[1].substring(0, lastIdx) : args[1];
        Path jdtCorePrefsSrcPath = Paths.get(srcWorkspace + SUB_DIR + JDT_CORE_PREFS_FILENAME);
        Path jdtUiPrefsSrcPath = Paths.get(srcWorkspace + SUB_DIR + JDT_UI_PREFS_FILENAME);

        // read formatter preferences
        List<String> coreLines, uiLines;

        try
        {
            coreLines = Files.readAllLines(jdtCorePrefsSrcPath);
            preserve(coreLines, "org.eclipse.jdt.core.formatter");
            uiLines = Files.readAllLines(jdtUiPrefsSrcPath);
            preserve(uiLines, "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles=");
        }
        catch (IOException e)
        {
            System.err.println(
                "Unable read from one or both source files: " + 
                jdtCorePrefsSrcPath + ", " + jdtUiPrefsSrcPath);
            return;
        }

        // get workspace paths
        List<Path> paths = null;

        try
        {
            paths = Files.walk(Paths.get(args[2]), 1)
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
        }
        catch (IOException e1)
        {
            System.err.println("Unable to retrieve sub-directories of " + args[2]);
        }

        for (Path p : paths)
        {
            if (!Files.isDirectory(Paths.get(p + "/.metadata")))
            {
                continue;
            }

            try
            {
                if (args[0].equals("style"))
                {
                    Path uiWorkbenchPrefsSrc = 
                        Paths.get(srcWorkspace + SUB_DIR + UI_WORKBENCH_PREFS_FILENAME);
                    Path uiEditorsPrefsSrc = 
                        Paths.get(srcWorkspace + SUB_DIR + UI_EDITORS_PREFS_FILENAME);
                    updateStyle(
                        p.toString(), uiEditorsPrefsSrc,
                        uiWorkbenchPrefsSrc, jdtCorePrefsSrcPath, jdtUiPrefsSrcPath);
                }
                else if (args[0].equals("format"))
                {
                    updateFormatter(p.toString(), coreLines, uiLines);
                }
                else
                {
                    System.err.println("Invalid command: " + args[0]);
                    return;
                }
                System.out.println("Workspace format successful: " + args[2] + "/" + p);
            }
            catch (IOException e)
            {
                System.err.println("Workspace format failed: " + p + "\n" + e.getMessage() + "\n");
            }
        }
    }
}
