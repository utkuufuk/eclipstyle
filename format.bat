set WORKSPACE=%1
set TARGET="%WORKSPACE%\.metadata\.plugins\org.eclipse.core.runtime\.settings\"
copy ".\org.eclipse.jdt.core.prefs" %TARGET%
copy ".\org.eclipse.jdt.ui.prefs" %TARGET%
copy ".\org.eclipse.ui.editors.prefs" %TARGET%