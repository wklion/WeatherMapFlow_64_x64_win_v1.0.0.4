//-------------------------------------------------------------
// \project PluginExample3
// \file ModuleQuery.java
// \brief 查询模块
// \author 王庆飞
// \date 2016-9-12
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample3;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.wmf.model.*;

import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;

public class ModuleQuery extends Module 
{
	public ModuleQuery(int nID) 
	{
		super(nID);
		
		m_alOutputPort.add(new Port(this, Port.Type.JDBCResultSet));
		
		m_strName = "查询";
    }
	void Close()
	{
		if (m_rs != null)
		{
			try
			{
				m_rs.close();
			}
			catch (SQLException ex)
			{
			}
			m_rs = null;
		}
		if (m_ps != null)
		{
			try
			{
				m_ps.close();
			}
			catch (SQLException ex)
			{
			}
			m_ps = null;
		}
		if (m_connection != null)
		{
			try
			{
				m_connection.close();
			}
			catch (SQLException ex)
			{
			}
			m_connection = null;
		}
	}
	public void Destroy()
	{
		Close();
		super.Destroy();
	}

	public String GetGroupName()
	{
		return "JDBC";
	}
	public String GetName()
	{
		return m_strName;
	}
	public String GetDescription()
	{
		return "数据库查询。";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
			String str = "";
			final HashMap<String, GlobalObject> hm = Model.GetGlobalObjects();
			Iterator<Map.Entry<String, GlobalObject>> it = hm.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry<String, GlobalObject> e = it.next();
				if (e.getValue() instanceof ConnectionObject)
				{
					if (!str.isEmpty())
						str += "|";
					str += e.getKey();
				}
			}
			
			alParam.add(new Param("ConnectionName", m_strConnectionName, "数据库连接名称", "", "", Param.EditType.SelectOrInput, str));
        	alParam.add(new Param("Sql", m_strSql, "Sql", "", "", Param.EditType.MultilineText));
        	alParam.add(new Param("Name", m_strName, "名称", "", "", Param.EditType.MultilineText));
        	
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
			m_strConnectionName = (String)alParam.get(i++).m_objValue;
			m_strSql = (String)alParam.get(i++).m_objValue;
			m_strName = (String)alParam.get(i++).m_objValue;
			
			m_alOutputPort.get(0).SetName(m_strSql);
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
			GlobalObject go = Model.GetGlobalObject(m_strConnectionName);
			if (go == null)
				return false;
			ArrayList<Param> alParam = new ArrayList<Param>();
			go.GetOutputParam(alParam);
			if (alParam.size() == 0 || alParam.get(0).m_objValue == null || !(alParam.get(0).m_objValue instanceof Connection))
				return false;
			
			Close();
			m_connection = (Connection)alParam.get(0).m_objValue;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String str = m_strSql.replace("[DataStartTime]", sdf.format(m_model.m_dateDataStart));
			str = str.replace("[DataEndTime]", sdf.format(m_model.m_dateDataEnd));
			m_ps = m_connection.prepareStatement(str, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			if (m_ps == null)
				return false;
			m_rs = m_ps.executeQuery();
			
			OnParamChanged();
			
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		if (i == 0)
		{
			alParam.add(new Param("ResultSet", m_rs));
		}
		
		return alParam.size();
	}
	
	protected String m_strConnectionName = "";
	protected String m_strSql = "";
	protected String m_strName;
	
	Connection m_connection = null;
	PreparedStatement m_ps = null;
	ResultSet m_rs = null;
}
