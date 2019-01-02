import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Eclipstyle
{
    public enum Condition
    {
        STARTS_WITH,
        DOES_NOT_START_WITH;
    }

    private static final String UI_WORKBENCH_PREFS_FILENAME = "org.eclipse.ui.workbench.prefs";
    private static final String UI_EDITORS_PREFS_FILENAME = "org.eclipse.ui.editors.prefs";
    private static final String JDT_CORE_PREFS_FILENAME = "org.eclipse.jdt.core.prefs";
    private static final String JDT_UI_PREFS_FILENAME = "org.eclipse.jdt.ui.prefs";
    private static final String PRINT_MARGIN_KEY = "org.eclipse.jdt.core.formatter.lineSplit";
    private static final String SUB_DIR = "/.metadata/.plugins/org.eclipse.core.runtime/.settings/";

    private static List<String> readLines(String path, Condition condition, String... keys)
        throws FileNotFoundException
    {
        File file = new File(path);
        Scanner scanner = new Scanner(file);  
        List<String> lines = new ArrayList<String>();

        while (scanner.hasNextLine()) 
        {  
            boolean read = condition == Condition.DOES_NOT_START_WITH;
            String line = scanner.nextLine();

            for (String key : keys)
            {
                if (condition == Condition.DOES_NOT_START_WITH && line.startsWith(key))
                {
                    read = false;
                    break;
                }
                else if (condition == Condition.STARTS_WITH && line.startsWith(key))
                {
                    read = true;
                    break;
                }
            }

            if (read)
            {
                lines.add(line);
            }
        }
        scanner.close();
        return lines; 
    }

    private static void updateFormatter(
            String workspacePath, List<String> coreLines, List<String> uiLines) 
            throws IOException
    {
        String path = workspacePath + SUB_DIR + JDT_UI_PREFS_FILENAME;
        List<String> lines = readLines(
            path, Condition.DOES_NOT_START_WITH,
            "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles=");
        new File(path).delete();
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
        Files.write(Paths.get(path), uiLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        path = workspacePath + SUB_DIR + JDT_CORE_PREFS_FILENAME;
        lines = readLines(path, Condition.DOES_NOT_START_WITH, "org.eclipse.jdt.core.formatter");
        new File(path).delete();
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
        Files.write(Paths.get(path), coreLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        Integer printMargin = null;

        for (String line : coreLines)
        {
            if (line.startsWith(PRINT_MARGIN_KEY))
            {
                printMargin = Integer.parseInt(line.substring(line.lastIndexOf("=") + 1));
            }
        }

        if (printMargin == null)
        {
            throw new IOException("Could not find key: " + PRINT_MARGIN_KEY);
        }
        path = workspacePath + SUB_DIR + UI_EDITORS_PREFS_FILENAME;
        lines = readLines(path, Condition.DOES_NOT_START_WITH, "printMarginColumn=");
        new File(path).delete();
        lines.add("printMarginColumn=" + printMargin);
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
    }

    private static void updateStyle(
        String workspaceDir, String uiEditorsPrefsSourcePath, String uiWorkbenchPrefsSourcePath,
        String jdtCorePrefsSourcePath, String jdtUiPrefsSourcePath) throws IOException
    {
        Files.copy(
            Paths.get(uiWorkbenchPrefsSourcePath),
            Paths.get(workspaceDir + SUB_DIR + UI_WORKBENCH_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
            Paths.get(uiEditorsPrefsSourcePath),
            Paths.get(workspaceDir + SUB_DIR + UI_EDITORS_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
            Paths.get(jdtCorePrefsSourcePath),
            Paths.get(workspaceDir + SUB_DIR + JDT_CORE_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
            Paths.get(jdtUiPrefsSourcePath), 
            Paths.get(workspaceDir + SUB_DIR + JDT_UI_PREFS_FILENAME),
            StandardCopyOption.REPLACE_EXISTING);       
    }

    public static void main(String[] args)
    {
        // source files to read formatter preferences
        int lastIdx = args[1].length() - 1;
        String srcWorkspacePath = args[1].charAt(lastIdx) == File.separatorChar ? 
            args[1].substring(0, lastIdx) : args[1];
        String jdtCorePrefsSourcePath = srcWorkspacePath + SUB_DIR + JDT_CORE_PREFS_FILENAME;
        String jdtUiPrefsSourcePath = srcWorkspacePath + SUB_DIR + JDT_UI_PREFS_FILENAME;
        List<String> coreLines, uiLines;

        try
        {
            coreLines = readLines(
                jdtCorePrefsSourcePath, Condition.STARTS_WITH, "org.eclipse.jdt.core.formatter");
            uiLines = readLines(
                jdtUiPrefsSourcePath, Condition.STARTS_WITH,
                "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles=");
        }
        catch (FileNotFoundException e)
        {
            System.err.println(
                "One or both source files not found: " + 
                jdtCorePrefsSourcePath + ", " + jdtUiPrefsSourcePath);
            return;
        }

        // directory where all target workspaces reside
        File workspaceParentDir = new File(args[2]);

        String[] paths = workspaceParentDir.list(new FilenameFilter()
        {
            @Override
            public boolean accept(File current, String name)
            {
                return new File(current, name).isDirectory();
            }
        });

        for (String p : paths)
        {
            try
            {
                if (args[0].equals("style"))
                {
                    String uiWorkbenchPrefsSourcePath = srcWorkspacePath + SUB_DIR + UI_WORKBENCH_PREFS_FILENAME;
                    String uiEditorsPrefsSourcePath = srcWorkspacePath + SUB_DIR + UI_EDITORS_PREFS_FILENAME;
                    updateStyle(
                        workspaceParentDir + "/" + p, uiEditorsPrefsSourcePath,
                        uiWorkbenchPrefsSourcePath, jdtCorePrefsSourcePath, jdtUiPrefsSourcePath);
                }
                else if (args[0].equals("format"))
                {
                    updateFormatter(workspaceParentDir + "/" + p, coreLines, uiLines);
                }
                else
                {
                    System.err.println("Invalid command: " + args[0]);
                    return;
                }
                System.out.println("Workspace format successful: " + workspaceParentDir + "/" + p);
            }
            catch (IOException e)
            {
                System.err.println("Unable to format workspace: " + p + "\n" + e.getMessage() + "\n");
            }
        }
    }
}
