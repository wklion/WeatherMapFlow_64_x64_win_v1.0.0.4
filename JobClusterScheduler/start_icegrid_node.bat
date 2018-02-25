title JobClusterScheduler

cd /d %~dp0
md db
cd /d %~dp0db
rd /s/q node2
md node2
cd /d %~dp0
icegridnode --Ice.Config=config.node
pause