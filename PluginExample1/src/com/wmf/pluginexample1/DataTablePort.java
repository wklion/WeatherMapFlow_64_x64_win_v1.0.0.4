//-------------------------------------------------------------
// \project PluginExample1
// \file DataTablePort.java
// \brief 数据表端口
// \author 王庆飞
// \date 2017-7-31
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.wmf.model.*;

/**
 * 如果输入端或输出端可动态增删端口，需要派生端口类，否则Undo可能出错。
 */
public class DataTablePort extends Port
{
	public DataTablePort(Module module)
	{
		super(module);
    }
	public DataTablePort(Module module, Type eType)
	{
		super(module, eType);
    }
	public Port Clone()
	{
		DataTablePort mp = new DataTablePort(null, m_eType);
		mp.m_strName = m_strName;
		mp.m_rs = m_rs;
		mp.m_list = m_list;
		return mp;
	}
	
	public void GetFieldName(ArrayList<String> alFieldName)
	{
		try
		{
			if (m_rs == null && m_list == null)
				return;
			int i = 0;
			String str;
			ResultSetMetaData rsmd = m_rs != null ? m_rs.getMetaData() : null;
			String[] strs = m_list != null ? m_list.get(0).split(",") : null;
			for (i = 0; i < (rsmd != null ? rsmd.getColumnCount() : strs.length); i++)
			{
				str = (rsmd != null ? rsmd.getColumnLabel(i + 1) : strs[i]);
				alFieldName.add(str);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void GetFieldType(ArrayList<String> alFieldType)
	{
		try
		{
			if (m_rs == null && m_list == null)
				return;
			int i = 0;
			String str;
			if (m_rs != null)
			{
				ResultSetMetaData rsmd = m_rs.getMetaData();
				for (i = 0; i < rsmd.getColumnCount(); i++)
				{
					switch (rsmd.getColumnType(i + 1))
					{
					case Types.TINYINT:
						str = "TINYINT";
						break;
					case Types.SMALLINT:
						str = "SMALLINT";
						break;
					case Types.INTEGER:
						str = "INTEGER";
						break;
					case Types.BIGINT:
						str = "BIGINT";
						break;		
					case Types.FLOAT:
						str = "FLOAT";
						break;
					case Types.REAL:
						str = "REAL";
						break;
					case Types.DOUBLE:
						str = "DOUBLE";
						break;
					case Types.DECIMAL:
						str = "DECIMAL";
						break;
					case Types.NUMERIC:
						str = "NUMERIC";
						break;
					default:
						str = "VARCHAR";
						break;
					}
					alFieldType.add(str);
				}
			}
			else
			{
				String[] strs = m_list.get(1).split(",");
				for (i = 0; i < strs.length; i++)
					alFieldType.add(strs[i]);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public ResultSet m_rs = null;
	public List<String> m_list = null;
}
	
	
