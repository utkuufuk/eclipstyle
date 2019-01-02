import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Formatter
{
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

    private static void updatePrintMargin(String workspacePath, int printMargin) throws IOException
    {
        String path = workspacePath + SUB_DIR + "org.eclipse.ui.editors.prefs";
        List<String> lines = readLines(path, Condition.DOES_NOT_START_WITH, "printMarginColumn=");
        new File(path).delete();
        lines.add("printMarginColumn=" + printMargin);
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
    }

    private static int updateFormatter(
            String workspacePath, List<String> coreLines, List<String> uiLines) 
            throws IOException
    {
        String path = workspacePath + SUB_DIR + "org.eclipse.jdt.ui.prefs";
        List<String> lines = readLines(
            path, Condition.DOES_NOT_START_WITH,
            "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles");
        new File(path).delete();
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
        Files.write(Paths.get(path), uiLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        path = workspacePath + SUB_DIR + "org.eclipse.jdt.core.prefs";
        lines = readLines(path, Condition.DOES_NOT_START_WITH, "org.eclipse.jdt.core.formatter");
        new File(path).delete();
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
        Files.write(Paths.get(path), coreLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        for (String line : coreLines)
        {
            if (line.startsWith(PRINT_MARGIN_KEY))
            {
                return Integer.parseInt(line.substring(line.lastIndexOf("=") + 1));
            }
        }
        throw new IOException("Could not find key: " + PRINT_MARGIN_KEY);
    }

    public static void main(String[] args)
    {
        // source files to read formatter preferences
        int lastIdx = args[0].length() - 1;
        String srcWorkspacePath = args[0].charAt(lastIdx) == File.separatorChar ? 
            args[0].substring(0, lastIdx) : args[0];
        String corePrefsSourcePath = srcWorkspacePath + SUB_DIR + "org.eclipse.jdt.core.prefs";
        String uiPrefsSourcePath = srcWorkspacePath + SUB_DIR + "org.eclipse.jdt.ui.prefs";
        List<String> coreLines, uiLines;

        try
        {
            coreLines = readLines(
                corePrefsSourcePath, Condition.STARTS_WITH, "org.eclipse.jdt.core.formatter");
            uiLines = readLines(
                uiPrefsSourcePath, Condition.STARTS_WITH,
                "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles=");
        }
        catch (FileNotFoundException e)
        {
            System.err.println(
                "One or both source files not found: " + 
                corePrefsSourcePath + ", " + uiPrefsSourcePath);
            return;
        }

        // directory where all target workspaces reside
        File workspaceParentDir = new File(args[1]);

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
                int printMargin = updateFormatter(workspaceParentDir + "/" + p, coreLines, uiLines);
                updatePrintMargin(workspaceParentDir + "/" + p, printMargin);
                System.out.println("Workspace format successful: " + workspaceParentDir + "/" + p);
            }
            catch (IOException e)
            {
                System.err.println("Unable to format workspace: " + p + "\n" + e.getMessage() + "\n");
            }
        }
    }
}
