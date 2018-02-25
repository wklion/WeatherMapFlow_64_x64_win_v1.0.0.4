//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleImportCSV.java
// \brief 导入CSV模块类
// \author 王庆飞
// \date 2017-5-30
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;

import com.wmf.model.*;

public class ModuleImportCSV extends Module
{
	public ModuleImportCSV(int nID)
	{
		super(nID);
		
		m_alOutputPort.add(new Port(this, Port.Type.CSV));
    }

	public String GetGroupName()
	{
		return "数据导入";
	}
	public String GetName()
	{
		return "导入CSV";
	}
	public String GetDescription()
	{
		return "从CSV文件导入CSV格式数据。";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);

			alParam.add(new Param("FileName", m_strFileName, "CSV文件", "", "", Param.EditType.File, "CSV文件(*.csv)|*.csv|所有文件(*.*)|*.*"));
			
			Model.GetAlias(alParam, m_alAlias, nOffset);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}		
		
		return alParam.size();
	}
	//用于 XML->参数 等
	public int SetParam(final ArrayList<Param> alParam)
	{
		int i = super.SetParam(alParam);
		try
		{
			m_strFileName = (String)alParam.get(i++).m_objValue;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public boolean Execute()
	{
		try
		{
			String strFileName = m_strFileName;
			if (!new File(strFileName).isAbsolute())
				strFileName = new File(m_model.m_strFileName).getParent() + "/" + m_strFileName;
			if (!new File(strFileName).exists())
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块CSV文件\"%s\"不存在。", GetNestedName(), GetID(), strFileName));
				return false;
			}
			
			m_ll.clear();
			String str;
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(strFileName), "UTF-8"));
			while ((str = br.readLine()) != null) 
	        {
				m_ll.add(str);
	        }
	        br.close();
	        
	        OnParamChanged();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		alParam.add(new Param("CSV", m_ll));
		return alParam.size();
	}

	protected String m_strFileName;
	LinkedList<String> m_ll = new LinkedList<String>();
}
