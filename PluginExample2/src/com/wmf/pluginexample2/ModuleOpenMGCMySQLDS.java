//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleOpenMGCMySQLDS.java
// \brief 打开MGCMySQL数据源模块类
// \author 王庆飞
// \date 2018-1-18
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.io.File;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleOpenMGCMySQLDS extends ModuleBase
{
	public ModuleOpenMGCMySQLDS(int nID)
	{
		super(nID);
		
		m_alOutputPort.add(new Port(this));

		m_strAlias = String.format("Datasource%d", m_nID);
		m_strServer = "127.0.0.1";
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "打开\nMGCMySQL数据源";
	}
	public String GetDescription()
	{
		return "打开一个MGCMySQL数据源。";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		int nOffset = super.GetParam(alParam);
		
		alParam.add(new Param("Alias", m_strAlias, "别名", "", ""));
		alParam.add(new Param("Server", m_strServer, "主机名/IP", "", ""));
		alParam.add(new Param("User", m_strUser, "用户", "", ""));
		alParam.add(new Param("Password", m_strPassword, "密码", "", ""));
		alParam.add(new Param("DB", m_strDB, "数据库", "", ""));
		alParam.add(new Param("Port", m_strPort, "端口", "", ""));
		
		Model.GetAlias(alParam, m_alAlias, nOffset);
		
		return alParam.size();
	}
	//用于 XML->参数 等
	public int SetParam(final ArrayList<Param> alParam)
	{
		int i = super.SetParam(alParam);
		try
		{
			Model2 model = (Model2)m_model;
			if (!m_strAlias.isEmpty())
				model.m_ws.CloseDatasource(m_strAlias);
			
			m_strAlias = (String)alParam.get(i++).m_objValue;
			m_strServer = (String)alParam.get(i++).m_objValue;
			m_strUser = (String)alParam.get(i++).m_objValue;
			m_strPassword = (String)alParam.get(i++).m_objValue;
			m_strDB = (String)alParam.get(i++).m_objValue;
			m_strPort = (String)alParam.get(i++).m_objValue;
			
			String str;
			Datasource ds = m_strAlias.isEmpty() ? null : model.m_ws.GetDatasource(m_strAlias);
			if (ds == null && !m_strServer.isEmpty())
			{				
				str = String.format("{\"Type\":\"MGCMySQL\",\"Alias\":\"%s\",\"Server\":\"%s\",\"User\":\"%s\",\"Password\":\"%s\",\"DB\":\"%s\",\"Port\":\"%s\"}", 
						m_strAlias, m_strServer, m_strUser, m_strPassword, m_strDB, m_strPort);  
				ds = model.m_ws.OpenDatasource(str);
				if (ds != null)
				{
					int j = 0;
					for (j = m_alOutputPort.size(); j < ds.GetDatasetCount(); j++)
						m_alOutputPort.add(new Port(this));
					
					for (j = 0; j < ds.GetDatasetCount(); j++)
					{
						Dataset dataset = ds.GetDataset(j);
						Port port = m_alOutputPort.get(j);
						port.SetType(Port.Type.valueOf("Dataset" + dataset.GetType()));
						port.SetName(dataset.GetName() + "@" + ds.GetAlias());
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public boolean Execute()
	{
		Model2 model = ((Model2)m_model);
		if (m_strAlias.isEmpty())
			throw new AssertionError();
		Datasource ds = model.m_ws.GetDatasource(m_strAlias);
		if (ds == null)
		{
			if (m_strServer.isEmpty())
			{
				model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"主机名/IP\"参数为空。", GetNestedName(), GetID()));
				return false;
			}
			
			String str = String.format("{\"Type\":\"MGCMySQL\",\"Alias\":\"%s\",\"Server\":\"%s\",\"User\":\"%s\",\"Password\":\"%s\",\"DB\":\"%s\",\"Port\":\"%s\"}", 
					m_strAlias, m_strServer, m_strUser, m_strPassword, m_strDB, m_strPort);  
			ds = model.m_ws.OpenDatasource(str);
			if (ds == null)
				return false;
		}
		
		return true;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		Model2 model = ((Model2)m_model);
		Datasource ds = m_strAlias.isEmpty() ? null : model.m_ws.GetDatasource(m_strAlias);
		if (ds == null || i >= ds.GetDatasetCount())
			return 0;
		alParam.add(new Param(Param.Datasource, ds.GetAlias()));
		
		Dataset dataset = ds.GetDataset(i);
		dataset.Open();
		if (dataset instanceof DatasetRaster)
			((DatasetRaster)dataset).CalcExtreme();
		alParam.add(new Param(Param.Dataset, dataset.GetName()));
		return alParam.size();
	}

	protected String m_strAlias;
	protected String m_strServer;
	protected String m_strUser;
	protected String m_strPassword;
	protected String m_strDB;
	protected String m_strPort;
}
