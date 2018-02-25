//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleOpenDS.java
// \brief 打开数据源模块类
// \author 王庆飞
// \date 2016-5-10
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

public class ModuleOpenDS extends ModuleBase
{
	public ModuleOpenDS(int nID)
	{
		super(nID);
		
		m_alOutputPort.add(new Port(this));

		m_strType = "Micaps";
		m_strAlias = String.format("Datasource%d", m_nID);
		m_strServer = "";
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "打开\n数据源";
	}
	public String GetDescription()
	{
		return "打开一个数据源。";
	}
	
	public boolean OnAttachModel(Model model)
	{
		if (!super.OnAttachModel(model))
			return false;
		m_model = model;
		Execute();
		return true;
	}
	
	String GetFilter(String strType)
	{
		if (strType.equals("AIGrid"))
			return "Arc/Info Binary Grid(*.*)|*.*|所有文件(*.*)|*.*";
		else if (strType.equals("AWX"))
			return "AWX卫星云图文件(*.AWX)|*.AWX|所有文件(*.*)|*.*";
		else if (strType.equals("ESRI Shapefile"))
			return "ESRI Shapefile(*.shp)|*.shp|所有文件(*.*)|*.*";
		else if (strType.equals("GPF"))
			return "GPF云图文件(*.gpf)|*.gpf|所有文件(*.*)|*.*";
		else if (strType.equals("GTiff"))
			return "GTiff文件(*.tif)|*.tif|所有文件(*.*)|*.*";
		else if (strType.equals("GrADS"))
			return "GrADS文件(*.ctl)|*.ctl|所有文件(*.*)|*.*";
		else if (strType.equals("grib_api"))
			return "grib文件(*.grb1;*.grb2;*.grib1;*.grib2;*.grib)|*.grb1;*.grb2;*.grib1;*.grib2;*.grib|所有文件(*.*)|*.*";
		else if (strType.equals("MGCnetCDF"))
			return "netCDF文件(*.nc)|*.nc|所有文件(*.*)|*.*";
		else if (strType.equals("RadarBase"))
			return "雷达基数据文件(*.bin)|*.bin|所有文件(*.*)|*.*";
		else if (strType.equals("SWAN"))
			return "SWAN数据文件(*.bin)|*.bin|所有文件(*.*)|*.*";
		return strType + "文件(*.*)|*.*";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		int nOffset = super.GetParam(alParam);
		
		alParam.add(new Param("Type", m_strType, "类型", "", "", Param.EditType.Select, "AIGrid|AWX|ESRI Shapefile|GPF|GTiff|GrADS|grib_api|Micaps|MGCnetCDF|RadarBase|RadarProduct|SWAN"));
		alParam.add(new Param("Alias", m_strAlias, "别名", "", ""));
		alParam.add(new Param("Server", m_strServer, "文件路径", "", "", Param.EditType.File, GetFilter(m_strType)));
		
		Model.GetAlias(alParam, m_alAlias, nOffset);
		
		return alParam.size();
	}
	public boolean OnParamChanged(ArrayList<Param> alParam, int nIndex, Object objValue)
	{
		if (alParam.get(nIndex).m_strName.equals("Type"))
		{
			alParam.get(2).m_strOptionalValues = GetFilter((String)objValue);
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
			Model2 model = (Model2)m_model;
			if (!m_strAlias.isEmpty())
				model.m_ws.CloseDatasource(m_strAlias);
			
			m_strType = (String)alParam.get(i++).m_objValue;
			m_strAlias = (String)alParam.get(i++).m_objValue;
			m_strServer = (String)alParam.get(i++).m_objValue;
			
			String str;
			Datasource ds = m_strAlias.isEmpty() ? null : model.m_ws.GetDatasource(m_strAlias);
			if (ds == null && !m_strServer.isEmpty())
			{
				String strServer = model.FormatDate(m_strServer, false);
				if (!new File(strServer).isAbsolute())
					strServer = new File(model.m_strFileName).getParent() + "/" + strServer;
				
				str = String.format("{\"Type\":\"%s\",\"Alias\":\"%s\",\"Server\":\"%s\"}", m_strType, m_strAlias, strServer.replace('\\', '/'));  
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
				model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"文件路径\"参数为空。", GetNestedName(), GetID()));
				return false;
			}
			String strServer = model.FormatDate(m_strServer, false);
			if (!new File(strServer).isAbsolute())
				strServer = new File(m_model.m_strFileName).getParent() + "/" + strServer;
			
			String str = String.format("{\"Type\":\"%s\",\"Alias\":\"%s\",\"Server\":\"%s\"}", m_strType, m_strAlias, strServer.replace('\\', '/'));  
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

	protected String m_strType;
	protected String m_strAlias;
	protected String m_strServer;
}
