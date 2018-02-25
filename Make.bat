@echo off
::-------------------------------------------------------------------------------------------------------------------------------------


for /f "delims=" %%i in ('dir %~dp0"PluginExample2\bin\*.svn" /ad /b /s') do (
	rd /s /q "%%i"
)
jar cvfm ./Modeler/wmf-pluginexample2.jar ./PluginExample2/WebRoot/META-INF/MANIFEST.MF -C PluginExample2/bin .
xcopy ".\Modeler\wmf-pluginexample2.jar" ".\PluginTest\lib\" /y
xcopy ".\Modeler\wmf-pluginexample2.jar" ".\JobClusterScheduler\" /y



pause

