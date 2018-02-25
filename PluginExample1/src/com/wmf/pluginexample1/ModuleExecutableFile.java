//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleExecutableFile.java
// \brief 可执行文件模块类
// \author 王庆飞
// \date 2017-7-27
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.util.ArrayList;

import com.wmf.model.*;

public class ModuleExecutableFile extends Module 
{
	public ModuleExecutableFile(int nID) 
	{
		super(nID);
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "可执行文件";
	}
	public String GetDescription()
	{
		return "";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			
			alParam.add(new Param("FileName", m_strFileName, "文件名", "", "", Param.EditType.File, "exe文件(*.exe)|*.exe|bat文件(*.bat)|*.bat|所有文件(*.*)|*.*"));
			alParam.add(new Param("Arguments", m_strArguments, "参数", "", ""));
			
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
			m_strArguments = (String)alParam.get(i++).m_objValue;
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
			Process p = Runtime.getRuntime().exec(m_strFileName + " " + m_strArguments);
			p.waitFor();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	protected String m_strFileName;
	protected String m_strArguments;
}
