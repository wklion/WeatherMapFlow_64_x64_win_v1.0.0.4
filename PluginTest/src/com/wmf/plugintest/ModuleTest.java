//-------------------------------------------------------------
// \project PluginTest
// \file ModuleTest.java
// \brief 测试模块类
// \author 王庆飞
// \date 2016-4-14
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.plugintest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.wmf.model.*;

public class ModuleTest extends Module 
{
	public ModuleTest(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new Port(this));
		m_alInputPort.add(new Port(this));
		m_alInputPort.add(new Port(this));
		m_alOutputPort.add(new Port(this));
    }

	public String GetGroupName()
	{
		return "测试";
	}
	public String GetName()
	{
		return "测试";
	}
	public String GetDescription()
	{
		return "测试";
	}
	
	public boolean OnAttach(Port portFrom, Port portTo)
	{
		return true;
	}
	
	public boolean Execute()
	{
		m_model.OutputLog(Model.LogLevel.Debug, "ModuleTest.Execute()");
		
		OnParamChanged();
		
		return true;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		if (i == 0)
		{
			//--------------------------------------------------------------------
			//test ModuleImportPoint
//			LinkedList<String> ll = new LinkedList<String>();
//			ll.add("StationNum,x,y,z,value");
//			ll.add("VARCHAR,DOUBLE,DOUBLE,DOUBLE,FLOAT");
//			ll.add("0001,110.0,30.0,500.0,10.0");
//			ll.add("0002,110.2,30.5,510.0,20.0");
//			ll.add("0003,111.2,25.5,400.0,30.0");
//			
//			alParam.add(new Param("CSV", ll));
			//--------------------------------------------------------------------
			Map<String, String> map = new HashMap<String, String>();
			map.put("1", "测试1");
			map.put("2", "测试2");
			alParam.add(new Param("Map", map));
			
			
		}
		
		return alParam.size();
	}
}
