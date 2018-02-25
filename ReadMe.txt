                                                             WeatherMapFlow
----------------------------------------------------------------------------------------------------------------------------------
可视化建模器(Modeler)：可视化交互创建、调试模型。
运行WMFModeler.bat即可；
WMFModel.xml用于配置插件jar包，全局对象；
WMFModeler.xml用于配置表达式，投影等；

模型管理器：
剪切、复制、粘贴功能未使用系统剪贴板，不支持和系统功能交叉使用；

构造表达式对话框操作说明：
[Alt]+[1]设置焦点到变量列表；
[Alt]+[2]设置焦点到操作符列表；
[Alt]+[3]设置焦点到函数列表；
获取焦点后，按[Enter]键添加选中列表项到表达式文本框。
鼠标双击列表项也可添加选中列表项到表达式文本框。

模型参数或模块参数对话框操作说明：
别名列用于WMFModelServer参数传递；
[上下箭头]切换当前选中参数；
按照编辑类型分类：
缺省类型：直接按键输入，[Enter]确认输入，[Esc]取消输入；
多行输入：[Alt]+[下箭头]获取焦点或直接按键输入，[Ctrl]+[Enter]确认输入，[Esc]取消输入；
单选类型：[Alt]+[下箭头]弹出下拉列表，[上下箭头]选择，[Enter]确认选择，[Esc]取消选择；
单选或输入类型：相当于单选类型和缺省类型；
多选类型：自动弹出下拉列表，[Alt]+[下箭头]设置焦点到下拉列表，[Shift]+[上下箭头]选择，[Enter]确认选择，[Esc]取消选择；
Int,UInt,Float：类似缺省类型；
FixedIntArray,FixedUIntArray,FixedFloatArray,IntArray,UIntArray,FloatArray：[Tab]切换当前选中数组元素，
直接按键输入，[Enter]确认输入，[Esc]取消输入；
Color,File,Dir,DateTime,Expression,Proj：[Alt]+[下箭头]弹出对话框；对话框关闭后，设置焦点到按钮，[Enter]即可再次弹出对话框；

入度为0的模块的执行顺序：
按照模块左上角坐标，从上到下，从左到右，顺序执行（模块y坐标差<32，认为在同一行）；
----------------------------------------------------------------------------------------------------------------------------------
插件开发：支持java扩展开发模型、模块。
插件wmf-pluginexample2.jar使用了组件WeatherMapObjectsJava，强烈建议将组件dll单独放在一个目录，然后将该目录配置到PATH环境变量；

----------------------------------------------------------------------------------------------------------------------------------
任务集群调度器(JobClusterScheduler)：任务管理器(WMFJobManager)和模型服务器(WMFModelServer)的集群调度服务端。
启动方法：
1.运行start_icegrid_service.bat；
2.运行add_application.bat；
3.如果需要多节点集群，在子节点上运行start_icegrid_node.bat。
----------------------------------------------------------------------------------------------------------------------------------
任务管理器(WMFJobManager)：任务管理，任务调度，任务监控等。
B/S方式实现；
手动创建mysql数据库：quartz，编码选择utf8_general_ci；用tables_mysql_innodb.sql创建表；

已执行任务存取只支持mysql数据库，在\WMFJobManager\WEB-INF\WMFJobManager.xml中配置；
手动创建mysql数据库：wmf_job_manager，编码选择utf8_general_ci；用wmf_tables_mysql_innodb.sql创建表；

将文件夹WMFJobManager直接复制到tomcat安装目录\webapps即可，开发时使用apache-tomcat-7.0.68调试；
开发时访问地址：http://localhost:8080/WMFJobManager/login.html
开发时使用浏览器Google Chrome 版本 50.0.2661.94 m；IE最低版本要求IE10，未测试IE；

身份验证和授权：
不提供用户管理功能，只提供用户信息获取接口：wmf-userinfofinder.jar；
插件方式实现用户信息获取，提供默认插件：wmf-defaultuserinfofinder.jar；
在WMFJobManager.xml中配置用户信息获取插件；
----------------------------------------------------------------------------------------------------------------------------------
模型服务器(WMFModelServer)：服务管理，服务测试，服务响应等。
B/S方式实现；

将文件夹WMFModelServer直接复制到tomcat安装目录\webapps即可，开发时使用apache-tomcat-7.0.68调试；
开发时访问地址：http://localhost:8080/WMFModelServer/index.html
开发时使用浏览器Google Chrome 版本 50.0.2661.94 m；IE最低版本要求IE10，未测试IE；