//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleOutput.java
// \brief 输出模块
// \author 王庆飞
// \date 2017-4-18
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.wmf.model.*;

public class ModuleOutput extends Module 
{
	public ModuleOutput(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new Port(this));
    }

	public String GetGroupName()
	{
		return "调试";
	}
	public String GetName()
	{
		return "输出";
	}
	public String GetDescription()
	{
		return "";
	}
	
	public boolean OnAttach(Port portFrom, Port portTo)
	{
		try
		{
			int i = FindPort(portFrom, false);
			if (i != -1)
				return true;
			i = FindPort(portTo, true);
			if (i == -1)
				return false;
			
			m_alParam.clear();
			portFrom.GetModule().GetOutputParam(portFrom, m_alParam);
			portTo.SetType(portFrom.GetType());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean Execute()
	{
		try
		{
			if (m_alParam.size() == 0 || m_alParam.get(0).m_objValue == null)
				return false;
			if (m_alParam.get(0).m_objValue instanceof List<?>)
			{
				List<String> list = (List<String>)m_alParam.get(0).m_objValue;
				Iterator<String> it = list.iterator();
				while (it.hasNext())
				{
					m_model.OutputLog(Model.LogLevel.Debug, it.next()); 
				}
			}
			else if (m_alParam.get(0).m_objValue instanceof ResultSet)
			{
				ResultSet rs = (ResultSet)m_alParam.get(0).m_objValue;
				ResultSetMetaData rsmd = rs.getMetaData();
				int i = 0;
				String str = "";
				for (i = 0; i < rsmd.getColumnCount(); i++)
				{
					if (i > 0)
						str += ",";
					str += rsmd.getColumnLabel(i + 1);
				}
				m_model.OutputLog(Model.LogLevel.Debug, str);
				
				rs.beforeFirst();
				while (rs.next())
				{
					str = "";
					for (i = 0; i < rsmd.getColumnCount(); i++)
					{
						if (i > 0)
							str += ",";
						str += rs.getString(i + 1);
					}
					m_model.OutputLog(Model.LogLevel.Debug, str);
				}
			}
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
	
	ArrayList<Param> m_alParam = new ArrayList<Param>();
}
