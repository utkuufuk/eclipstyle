# eclipstyle
**Set up Eclipse workspace preferences once and use them everywhere!**

### How Does It Work?
Clones
 * code style formatting,
 * font & color settings,
 * text editor settings

from a reference workspace to all other workspaces.

### Usage
``` sh
java -jar eclipstyle.jar /path/to/ref/workspace /path/to/all/workspaces
```

For example:
``` sh
# windows
java -jar eclipstyle.jar D:\Dev\Workspaces\eclipstyle D:\Dev\Workspaces

# unix
java -jar eclipstyle.jar ~/eclipse_workspaces/eclipstyle ~/eclipse_workspaces
```

### Download Binary Release
You can download the latest release [here](https://github.com/utkuufuk/eclipstyle/releases).

### Samples
Feel free to import my [formatter profile](formatter.xml) or [preferences](sample_prefs).

### Credits
[picocli](https://github.com/remkop/picocli)
