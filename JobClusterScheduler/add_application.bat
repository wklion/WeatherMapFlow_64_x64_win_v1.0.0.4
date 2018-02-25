title add_application

icegridadmin --Ice.Config=config.grid -e "application add application.xml"
::-------------------------------------------------------------------------------------------------------------------------------------
::只用于跨网段等
::icegridadmin --Ice.Config=config.grid -e "server start WMFGlacier2"
::icegridadmin --Ice.Default.Router="WMFGlacier2/router:tcp -h localhost -p 4063"
::-------------------------------------------------------------------------------------------------------------------------------------
pause