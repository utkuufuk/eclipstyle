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
    private static final int PRINT_MARGIN = 100;
    private static final String SUB_DIR = "/.metadata/.plugins/org.eclipse.core.runtime/.settings/";

    private static List<String> readExcept(String path, String... ignoredKeys) throws FileNotFoundException
    {
        File file = new File(path);
        Scanner scanner = new Scanner(file);  
        List<String> lines = new ArrayList<String>();

        while (scanner.hasNextLine()) 
        {  
            boolean read = true;
            String line = scanner.nextLine();

            for (String key : ignoredKeys)
            {
                if (line.startsWith(key))
                {
                    read = false;
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
        List<String> lines = readExcept(path, "printMarginColumn=");
        new File(path).delete();
        lines.add("printMarginColumn=" + printMargin);
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
    }

    private static void updateFormatter(
            String workspacePath, List<String> coreLines, List<String> editorLines) 
            throws IOException
    {
        String path = workspacePath + SUB_DIR + "org.eclipse.jdt.core.prefs";
        List<String> lines = readExcept(path, "org.eclipse.jdt.core.formatter");
        new File(path).delete();
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
        Files.write(Paths.get(path), coreLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        path = workspacePath + SUB_DIR + "org.eclipse.jdt.ui.prefs";
        lines = readExcept(path, "formatter_profile", "org.eclipse.jdt.ui.formatterprofiles");
        new File(path).delete();
        Files.write(Paths.get(path), lines, Charset.forName("UTF-8"));
        Files.write(Paths.get(path), editorLines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
    }

    public static void main(String[] args)
    {
        File mainWorkspaceDir = new File(args[0]);
        List<String> coreLines, editorLines;

        try
        {
            coreLines = readExcept("./core.prefs", "READ_ALL");
            editorLines = readExcept("./ui.prefs", "READ_ALL");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("One or both source files not found: " + args[1] + ", " + args[2]);
            return;
        }

        String[] paths = mainWorkspaceDir.list(new FilenameFilter()
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
                updatePrintMargin(mainWorkspaceDir + "/" + p, PRINT_MARGIN);
                updateFormatter(mainWorkspaceDir + "/" + p, coreLines, editorLines);
                System.out.println("Workspace format successful: " + mainWorkspaceDir + "/" + p);
            }
            catch (IOException e)
            {
                System.err.println("Unable to format workspace: " + p + "\n" + e.getMessage() + "\n");
            }
        }
    }
}
