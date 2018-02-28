@echo off
::-------------------------------------------------------------------------------------------------------------------------------------


for /f "delims=" %%i in ('dir %~dp0"PluginProject\bin\*.svn" /ad /b /s') do (
	rd /s /q "%%i"
)
jar cvfm ./Modeler/wmf-pluginproject.jar ./PluginProject/WebRoot/META-INF/MANIFEST.MF -C PluginProject/bin .
xcopy ".\Modeler\wmf-pluginproject.jar" ".\Modeler\" /y
xcopy ".\Modeler\wmf-pluginproject.jar" ".\JobClusterScheduler\" /y



pause

