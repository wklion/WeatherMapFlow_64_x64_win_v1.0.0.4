title JobClusterScheduler

cd /d %~dp0
rd /s/q db
md db
cd /d %~dp0db
md node
md registry
cd /d %~dp0
icegridnode --Ice.Config=config.grid
pause