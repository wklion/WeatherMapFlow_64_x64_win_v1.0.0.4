//-------------------------------------------------------------
// \project PluginExample3
// \file ConnectionObject.java
// \brief 数据库连接对象
// \author 王庆飞
// \date 2017-10-27
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample3;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.wmf.model.*;

import java.util.ArrayList;

public class ConnectionObject extends GlobalObject 
{
	public ConnectionObject() 
	{
		super();
    }
	
	public boolean Init()
	{
		m_cpds = new ComboPooledDataSource();
		
		return true;
	}
	
	public void UnInit()
	{
		try
		{
			if (m_cpds != null)
			{
				m_cpds.close();
				m_cpds = null;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		super.UnInit();
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			alParam.add(new Param("Driver", m_cpds.getDriverClass(), "Driver", "", ""));
			
        	alParam.add(new Param("Url", m_cpds.getJdbcUrl(), "Url", "", ""));
        	alParam.add(new Param("User", m_cpds.getUser(), "用户名", "", ""));
        	alParam.add(new Param("Password", m_cpds.getPassword(), "密码", "", ""));
        	
        	alParam.add(new Param("MaxIdleTime", m_cpds.getMaxIdleTime(), "最大空闲时间", "单位秒", ""));
        	alParam.add(new Param("MaxPoolSize", m_cpds.getMaxPoolSize(), "最大连接数", "", ""));
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
			m_cpds.setDriverClass((String)alParam.get(i++).m_objValue);
			
			m_cpds.setJdbcUrl((String)alParam.get(i++).m_objValue);
			m_cpds.setUser((String)alParam.get(i++).m_objValue);
			m_cpds.setPassword((String)alParam.get(i++).m_objValue);
			
			m_cpds.setMaxIdleTime(Integer.parseInt((String)alParam.get(i++).m_objValue));
			m_cpds.setMaxPoolSize(Integer.parseInt((String)alParam.get(i++).m_objValue));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public int GetOutputParam(ArrayList<Param> alParam)
	{
		try
		{
			alParam.add(new Param("Connection", m_cpds.getConnection()));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return alParam.size();
	}
		
	ComboPooledDataSource m_cpds = null;
}
