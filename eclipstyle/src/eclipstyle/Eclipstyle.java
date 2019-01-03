package eclipstyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    @Parameters(index = "0", description = "Source workspace path.")
    private Path sourceWorkspace;

    @Parameters(index = "1", description = "Parent directory of target workspaces.")
    private Path workspacesDir;

    private static void updateWorkspace(Path source, Path target) throws IOException
    {
        // evaluate source file paths to read formatter preferences
        Path uiWbenchSrc = Paths.get(source + PREFS_SUB_DIR + UI_WBENCH_PREFS_FILENAME);
        Path uiEditorsSrc = Paths.get(source + PREFS_SUB_DIR + UI_EDITORS_PREFS_FILENAME);
        Path jdtCoreSrc = Paths.get(source + PREFS_SUB_DIR + JDT_CORE_PREFS_FILENAME);
        Path jdtUiSrc = Paths.get(source + PREFS_SUB_DIR + JDT_UI_PREFS_FILENAME);

        // evaluate source file paths to read formatter preferences
        Path uiWbenchTarget = Paths.get(target + PREFS_SUB_DIR + UI_WBENCH_PREFS_FILENAME);
        Path uiEditorsTarget = Paths.get(target + PREFS_SUB_DIR + UI_EDITORS_PREFS_FILENAME);
        Path jdtCoreTarget = Paths.get(target + PREFS_SUB_DIR + JDT_CORE_PREFS_FILENAME);
        Path jdtUiTarget = Paths.get(target + PREFS_SUB_DIR + JDT_UI_PREFS_FILENAME);

        // copy prefs from source workspace into target workspace 
        Files.copy(uiWbenchSrc, uiWbenchTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(uiEditorsSrc, uiEditorsTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jdtCoreSrc, jdtCoreTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jdtUiSrc, jdtUiTarget, StandardCopyOption.REPLACE_EXISTING);       
    }

    @Override
    public Void call() throws Exception
    {
        // get workspace paths
        List<Path> targetWorkspaces = null;

        try
        {
            targetWorkspaces = Files.walk(workspacesDir, 1)
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
            targetWorkspaces.remove(0);
        }
        catch (IOException e1)
        {
            System.err.println("Unable to retrieve sub-directories of " + workspacesDir);
        }

        for (Path target : targetWorkspaces)
        {
            if (!Files.isDirectory(Paths.get(target + PREFS_SUB_DIR)))
            {
                System.err.println("Not an Eclipse workspace: " + target);
                continue;
            }

            try
            {
                updateWorkspace(sourceWorkspace, target);
                System.out.println("Workspace updated successfully: " + target);
            }
            catch (IOException e)
            {
                System.err.println("Workspace update failed: " + target + "\n" + e.getMessage() + "\n");
            }
        }
        return null;
    }

    public static void main(String[] args)
    {
        CommandLine.call(new Eclipstyle(), args);
    }
}
