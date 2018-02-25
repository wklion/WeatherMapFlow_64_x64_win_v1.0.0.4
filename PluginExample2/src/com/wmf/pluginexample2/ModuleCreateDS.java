//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleCreateDS.java
// \brief 创建数据源模块类
// \author 王庆飞
// \date 2016-7-13
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

public class ModuleCreateDS extends ModuleBase
{
	public ModuleCreateDS(int nID)
	{
		super(nID);
		
		m_strType = "Memory";
		m_strAlias = String.format("Datasource%d", m_nID);
		m_strServer = "";
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "创建\n数据源";
	}
	public String GetDescription()
	{
		return "创建一个数据源。";
	}
	
	String GetFilter(String strType)
	{
		if (strType.equals("ESRI Shapefile"))
			return "ESRI Shapefile(*.shp)|*.shp";
		else if (strType.equals("GTiff"))
			return "GTiff文件(*.tif)|*.tif";
		else if (strType.equals("grib_api"))
			return "grib文件(*.grb1;*.grb2;*.grib1;*.grib2;*.grib)|*.grb1;*.grb2;*.grib1;*.grib2;*.grib";
		return strType + "文件(*.*)|*.*";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		int nOffset = super.GetParam(alParam);
		
		alParam.add(new Param("Type", m_strType, "类型", "", "", Param.EditType.Select, "ESRI Shapefile|GTiff|grib_api|Memory"));
		alParam.add(new Param("Alias", m_strAlias, "别名", "", ""));
		alParam.add(new Param("Server", m_strServer, "文件路径", "", "", m_strType.equals("Memory") ? Param.EditType.ReadOnly : Param.EditType.File, GetFilter(m_strType)));
		
		Model.GetAlias(alParam, m_alAlias, nOffset);
		
		return alParam.size();
	}
	public boolean OnParamChanged(ArrayList<Param> alParam, int nIndex, Object objValue)
	{
		if (alParam.get(nIndex).m_strName.equals("Type"))
		{
			alParam.get(2).m_strOptionalValues = GetFilter((String)objValue);
			alParam.get(2).m_eEditType = objValue.equals("Memory") ? Param.EditType.ReadOnly : Param.EditType.File;
			return true;
		}
		return super.OnParamChanged(alParam, nIndex, objValue);
	}
	//用于 XML->参数 等
	public int SetParam(final ArrayList<Param> alParam)
	{
		int i = super.SetParam(alParam);
		try
		{
			m_strType = (String)alParam.get(i++).m_objValue;
			m_strAlias = (String)alParam.get(i++).m_objValue;
			m_strServer = (String)alParam.get(i++).m_objValue;
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
		if (ds != null)
			model.m_ws.CloseDatasource(m_strAlias);
					
		String strServer = m_strServer;
		if (!m_strType.equalsIgnoreCase("Memory"))
		{
			if (strServer.isEmpty())
			{
				model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"文件路径\"参数为空。", GetNestedName(), GetID()));
				return false;
			}			
			strServer = model.FormatDate(strServer, false);
			if (!new File(strServer).isAbsolute())
				strServer = new File(m_model.m_strFileName).getParent() + "/" + strServer;
			File file = new File(strServer); 
			if (file.exists())
			{
				file.delete();
				if (m_strType.equalsIgnoreCase("ESRI Shapefile"))
				{
					String str = file.getName();
					int nEnd = str.lastIndexOf('.');
					if (nEnd != -1)
					{
						str = file.getParent() + "/" + str.substring(0, nEnd);
						new File(str + ".dbf").delete();
						new File(str + ".prj").delete();
						new File(str + ".shx").delete();
					}
				}
			}
			else
			{
				File fileDir = new File(file.getParent());
				if (!fileDir.exists())
					fileDir.mkdirs();
			}
		}
		
		String str = String.format("{\"Type\":\"%s\",\"Alias\":\"%s\",\"Server\":\"%s\"}", m_strType, m_strAlias, strServer.replace('\\', '/'));  
		ds = model.m_ws.CreateDatasource(str);
		if (ds == null)
			return false;
		
		return true;
	}

	String m_strType;
	String m_strAlias;
	String m_strServer;
}
