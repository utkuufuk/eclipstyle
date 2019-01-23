package eclipstyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    description = "Propagates code style & format of a workspace across multiple workspaces.",
    name = "eclipstyle", mixinStandardHelpOptions = true, version = "eclipstyle 0.1")
public class Eclipstyle implements Callable<Void>
{
    private static final String UI_WBENCH_PREFS_FILENAME = "org.eclipse.ui.workbench.prefs";
    private static final String UI_EDITORS_PREFS_FILENAME = "org.eclipse.ui.editors.prefs";
    private static final String JDT_CORE_PREFS_FILENAME = "org.eclipse.jdt.core.prefs";
    private static final String JDT_UI_PREFS_FILENAME = "org.eclipse.jdt.ui.prefs";
    private static final String PREFS_SUB_DIR =
        "/.metadata/.plugins/org.eclipse.core.runtime/.settings/";

    @Parameters(index = "0", description = "Program command (clone/export).")
    private String command;

    @Parameters(index = "1", description = "Source path.")
    private Path from;

    @Parameters(index = "2", description = "Destionation path.")
    private Path to;

    /**
     * Copies a preferences file from a workspace (or an arbitrary directory) into another workspace.
     * 
     * @param source source workspace/directory
     * @param target target workspace
     * @param prefsName name of the preferences file
     * @throws IOException upon failure to locate the preferences file
     */
    private static void copyPrefs(Path source, Path target, String prefsName) throws IOException
    {
        if (Files.exists(source.resolve(prefsName)))
        {
            source = source.resolve(prefsName);
        }
        else if (Files.exists(Paths.get(source + PREFS_SUB_DIR + prefsName)))
        {
            source = Paths.get(source + PREFS_SUB_DIR + prefsName);
        }
        else
        {
            System.err.println("Could not locate prefs file " + prefsName + " in " + source);
            return;
        }
        target = target.resolve(prefsName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Returns a set of valid Eclipse workspace paths within a parent directory.
     * 
     * @param parentDir parent directory
     * @return a set of workspace paths
     * @throws IOException
     */
    private static Set<Path> getValidWorkspaces(Path parentDir) throws IOException
    {
        try
        {
            Set<Path> workspaces = new HashSet<Path>();
            Set<Path> subDirs = Files.walk(parentDir, 1)
                .filter(Files::isDirectory)
                .collect(Collectors.toSet());
            subDirs.remove(parentDir);

            for (Path ws : subDirs)
            {
                if (!Files.isDirectory(Paths.get(ws + PREFS_SUB_DIR)))
                {
                    System.err.println("Not an Eclipse workspace: " + ws);
                    continue;
                } 
                workspaces.add(ws);
            }
            return workspaces;
        }
        catch (IOException e)
        {
            throw new IOException(
                "Failed to retrieve workspaces in " + parentDir + ": " + e.getMessage());
        }
    }

    @Override
    public Void call()
    {
        try
        {
            if (command.equals("clone"))
            {
                Set<Path> workspaces = getValidWorkspaces(to);

                for (Path ws : workspaces)
                {
                    copyPrefs(from, Paths.get(ws + PREFS_SUB_DIR), UI_WBENCH_PREFS_FILENAME);
                    copyPrefs(from, Paths.get(ws + PREFS_SUB_DIR), UI_EDITORS_PREFS_FILENAME);
                    copyPrefs(from, Paths.get(ws + PREFS_SUB_DIR), JDT_CORE_PREFS_FILENAME);
                    copyPrefs(from, Paths.get(ws + PREFS_SUB_DIR), JDT_UI_PREFS_FILENAME);
                    System.out.println("Successfully updated Workspace preferences: " + ws);
                }
            }
            else if (command.equals("export"))
            {
                copyPrefs(from, to, UI_WBENCH_PREFS_FILENAME);
                copyPrefs(from, to, UI_EDITORS_PREFS_FILENAME);
                copyPrefs(from, to, JDT_CORE_PREFS_FILENAME);
                copyPrefs(from, to, JDT_UI_PREFS_FILENAME);
                System.out.println("Exported preferences of '" + from + "' to '" + to + "'");
            }
            else
            {
                System.err.println("Invalid command: " + command);
            }   
        }
        catch (IOException e)
        {
            System.err.println(command + " failed: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args)
    {
        CommandLine.call(new Eclipstyle(), args);
    }
}
