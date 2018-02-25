//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleImportPoint.java
// \brief 导入点模块类
// \author 王庆飞
// \date 2016-5-10
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.wmf.model.*;
import com.weathermap.objects.*;

import java.awt.geom.Rectangle2D;

public class ModuleImportPoint extends ModuleBase
{
	public ModuleImportPoint(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.Unknown));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetPoint));

		m_strDSOutput = "";
		m_strDatasetOutput = String.format("ImportPoint%d", m_nID);
    }

	public String GetGroupName()
	{
		return "数据导入";
	}
	public String GetName()
	{
		return "导入点";
	}
	public String GetDescription()
	{
		return "从JDBC.ResultSet或CSV格式导入生成点数据集。";
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
			
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() != 1)
				return false;
			if (alParam.get(0).m_objValue != null && !(alParam.get(0).m_objValue instanceof ResultSet) && !(alParam.get(0).m_objValue instanceof List<?>))
				return false;
			if (alParam.get(0).m_objValue instanceof ResultSet)
				m_rs = (ResultSet)alParam.get(0).m_objValue;
			else if (alParam.get(0).m_objValue instanceof List<?>)
				m_list = (List<String>)alParam.get(0).m_objValue;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
			Model2 model = ((Model2)m_model);

        	String strDatasources = model.GetWritableDatasources(false);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	alParam.add(new Param("XField", m_strXField, "x字段", "", "参数"));
        	alParam.add(new Param("YField", m_strYField, "y字段", "", "参数"));
        	
			alParam.add(new Param(Param.Datasource, m_strDSOutput, "数据源", "数据源别名", "输出端", Param.EditType.SelectOrInput, strDatasources));
			alParam.add(new Param(Param.Dataset, m_strDatasetOutput, "数据集", "数据集名称", "输出端"));
			
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
			m_strXField = (String)alParam.get(i++).m_objValue;
			m_strYField = (String)alParam.get(i++).m_objValue;
			
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alOutputPort.get(0).SetName(m_strDatasetOutput + "@" + m_strDSOutput);
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
			if (m_rs == null && m_list == null)
				return false;
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			
			Datasource ds = ws.GetDatasource(m_strDSOutput);
			if (ds == null)
				return false;
			DatasetVector dv = ds.CreateDatasetVector("{\"Name\":\"" + m_strDatasetOutput + "\",\"Type\":\"Point\"}");
			dv.SetProjection("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
			
			int i = 0, j = 0, x = 0, y = 0;
			String[] strNames = null;
			if (m_rs != null)
			{
				ResultSetMetaData rsmd = m_rs.getMetaData();
				strNames = new String[rsmd.getColumnCount()];
				for (i = 0; i < rsmd.getColumnCount(); i++)
				{
					String strName = rsmd.getColumnLabel(i + 1);
					strNames[i] = strName;
					if (strName.equalsIgnoreCase(m_strXField))
						x = i;
					else if (strName.equalsIgnoreCase(m_strYField))
						y = i;
					else
					{
						String strType = "Text";
						switch (rsmd.getColumnType(i + 1))
						{
						case Types.TINYINT:
						case Types.SMALLINT:
							strType = "Int16";
							break;
						case Types.INTEGER:
						case Types.BIGINT:
							strType = "Int32";
							break;		
						case Types.FLOAT:
						case Types.REAL:
							strType = "Single";
						case Types.DOUBLE:
						case Types.DECIMAL:
						case Types.NUMERIC:
							strType = "Double";
							break;
						default:
							break;
						}
						dv.AddField("{\"Name\":\"" + strName + "\",\"Type\":\"" + strType + "\",\"ForeignName\":\"\"}");
					}
				}
			}
			else
			{
				strNames = m_list.get(0).split(",");
				String[] strTypes = m_list.get(1).split(",");
				for (i = 0; i < strNames.length; i++)
				{
					String strName = strNames[i];
					if (strName.equalsIgnoreCase(m_strXField))
						x = i;
					else if (strName.equalsIgnoreCase(m_strYField))
						y = i;
					else
					{
						String strType = "Text";
						if (strTypes[i].equalsIgnoreCase("TINYINT") || strTypes[i].equalsIgnoreCase("SMALLINT"))
							strType = "Int16";
						else if (strTypes[i].equalsIgnoreCase("INTEGER") || strTypes[i].equalsIgnoreCase("BIGINT"))
							strType = "Int32";
						else if (strTypes[i].equalsIgnoreCase("FLOAT") || strTypes[i].equalsIgnoreCase("REAL"))
							strType = "Single";
						else if (strTypes[i].equalsIgnoreCase("DOUBLE") || strTypes[i].equalsIgnoreCase("DECIMAL") || strTypes[i].equalsIgnoreCase("NUMERIC"))
							strType = "Double";
						dv.AddField("{\"Name\":\"" + strName + "\",\"Type\":\"" + strType + "\",\"ForeignName\":\"\"}");
					}
				}
			}
			
			Recordset rs = dv.Query(null, null);
	        Rectangle2D rcBounds = new Rectangle2D.Double(), rc;
	        if (m_rs != null)
	        {
				m_rs.beforeFirst();
				while (m_rs.next())
				{
					GeoPoint gp = new GeoPoint(m_rs.getDouble(x + 1), m_rs.getDouble(y + 1));
	                rc = gp.GetBounds();
	                if (rs.GetRecordCount() == 0)
	                    rcBounds = rc;
	                else
	                	Rectangle2D.union(rc, rcBounds, rcBounds);
	
	                rs.AddNew(gp);
	                for (j = 0; j < strNames.length; j++)
	                {
	                	if (j == x || j == y)
	                		continue;
	                	rs.SetFieldValue(strNames[j], m_rs.getString(j + 1));
	                }
	                rs.Update();
	                gp.Destroy();
				}
	        }
	        else
	        {
	        	for (i = 2; i < m_list.size(); i++)
	        	{
	        		String[] strs = m_list.get(i).split(",");
	        		
	        		GeoPoint gp = new GeoPoint(Double.parseDouble(strs[x]), Double.parseDouble(strs[y]));
	                rc = gp.GetBounds();
	                if (rs.GetRecordCount() == 0)
	                    rcBounds = rc;
	                else
	                	Rectangle2D.union(rc, rcBounds, rcBounds);
	
	                rs.AddNew(gp);
	                for (j = 0; j < strNames.length; j++)
	                {
	                	if (j == x || j == y)
	                		continue;
	                	rs.SetFieldValue(strNames[j], strs[j]);
	                }
	                rs.Update();
	                gp.Destroy();
	        	}
	        }
	        
			rs.Destroy();
			dv.SetBounds(rcBounds);			
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
		alParam.add(new Param(Param.Datasource, m_strDSOutput));
		alParam.add(new Param(Param.Dataset, m_strDatasetOutput));
		return alParam.size();
	}

	ResultSet m_rs = null;
	List<String> m_list = null;
	
	String m_strXField = "x", m_strYField = "y";
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
