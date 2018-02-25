//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleBase.java
// \brief 模块基类
// \author 王庆飞
// \date 2016-7-13
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import com.wmf.model.*;
import com.wmf.model.Model.LogLevel;

public class ModuleBase extends Module
{
	public ModuleBase(int nID)
	{
		super(nID);
    }
	
	public boolean OnAttachModel(Model model)
	{
		if (!(model instanceof Model2))
		{
			model.OutputLog(LogLevel.Error, String.format("模块\"%s\"不支持模型\"%s\"，请创建模型\"%s\"。", 
					this.getClass().getName(), model.getClass().getName(), Model2.class.getName())); 
			return false;
		}
		return true;
	}
}
