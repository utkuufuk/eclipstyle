# eclipstyle
**Set up Eclipse workspace preferences once and use them everywhere!**

### How Does It Work?
Clones code style formatting as well as font, color & text editor settings located in
 * `org.eclipse.jdt.core.prefs`
 * `org.eclipse.jdt.ui.prefs`
 * `org.eclipse.ui.editors.prefs`
 * `org.eclipse.ui.workbench.prefs`

files from a directory or an existing workspace to all workspaces.

### Usage
``` sh
# export preferences
java -jar eclipstyle.jar export /path/to/ref/workspace /path/to/export/prefs

# clone previously exported preferences
java -jar eclipstyle.jar clone /path/to/import/prefs /path/to/all/workspaces

# export preferences from an existing workspace
java -jar eclipstyle.jar clone /path/to/ref/workspace /path/to/all/workspaces
```

#### `export` example
``` sh
# windows
java -jar eclipstyle.jar export D:\Dev\Workspaces\eclipstyle E:\Dropbox\eclipse_prefs

# unix
java -jar eclipstyle.jar export ~/dev/workspaces/eclipstyle ~/Dropbox/eclipse_prefs
```

#### `clone` example
``` sh
# windows (clone from existing workspace)
java -jar eclipstyle.jar clone D:\Dev\Workspaces\eclipstyle D:\Dev\Workspaces

# unix (clone previously exported prefs)
java -jar eclipstyle.jar clone ~/Dropbox/eclipse_prefs ~/dev/workspaces
```

### Download Binary Release
You can download the latest release [here](https://github.com/utkuufuk/eclipstyle/releases).

### Samples
Feel free to import my [formatter profile](formatter.xml) or [preferences](sample_prefs).

### Credits
[picocli](https://github.com/remkop/picocli) for command line argument parsing.
