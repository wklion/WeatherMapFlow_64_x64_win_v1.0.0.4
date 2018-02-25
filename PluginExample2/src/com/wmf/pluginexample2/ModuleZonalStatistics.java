//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleZonalStatistics.java
// \brief ZonalStatistics模块类
// \author 王庆飞
// \date 2016-5-13
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleZonalStatistics extends ModuleBase
{
	public ModuleZonalStatistics(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.DatasetRegion));
		m_alInputPort.add(new Port(this, Port.Type.DatasetRaster));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRegion));
		
		m_strDSZone = "";
		m_strDatasetZone = "";
		m_strDSRaster = "";
		m_strDatasetRaster = "";
		
		m_strStatisticsType = "Mean";
		m_strJoinFields = "";
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("ZonalStatistics%d", m_nID);
    }

	public String GetGroupName()
	{
		return "统计分析";
	}
	public String GetName()
	{
		return "分区统计";
	}
	public String GetDescription()
	{
		return "分区统计栅格值。";
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
			if (portFrom.GetType() != portTo.GetType())
				return false;
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() < 2)
				return false;
			if (i == 0)
			{
				m_strDSZone = (String)alParam.get(0).m_objValue;
				m_strDatasetZone = (String)alParam.get(1).m_objValue;
			}
			else if (i == 1)
			{
				m_strDSRaster = (String)alParam.get(0).m_objValue;
				m_strDatasetRaster = (String)alParam.get(1).m_objValue;
			}
			portTo.SetName(alParam.get(1).m_objValue + "@" + alParam.get(0).m_objValue);
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
        	alParam.add(new Param(Param.Datasource, m_strDSZone, "数据源", "数据源别名", "输入端/区域", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetZone, "数据集", "数据集名称", "输入端/区域", Param.EditType.ReadOnly));
			
			alParam.add(new Param(Param.Datasource, m_strDSRaster, "数据源", "数据源别名", "输入端/被统计栅格", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetRaster, "数据集", "数据集名称", "输入端/被统计栅格", Param.EditType.ReadOnly));
			
			alParam.add(new Param("StatisticsType", m_strStatisticsType, "统计类型", "", "", Param.EditType.MultiSelect, "Mean|Min|Max|Sum"));
			alParam.add(new Param("JoinFields", m_strJoinFields, "连接字段", "", "", Param.EditType.MultiSelect, model.GetFields(m_strDSZone, m_strDatasetZone, true)));
			
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
			m_strDSZone = (String)alParam.get(i++).m_objValue;
			m_strDatasetZone = (String)alParam.get(i++).m_objValue;
			m_strDSRaster = (String)alParam.get(i++).m_objValue;
			m_strDatasetRaster = (String)alParam.get(i++).m_objValue;
			
			m_strStatisticsType = (String)alParam.get(i++).m_objValue;
			m_strJoinFields = (String)alParam.get(i++).m_objValue;
			
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetZone + "@" + m_strDSZone);
			m_alInputPort.get(1).SetName(m_strDatasetRaster + "@" + m_strDSRaster);
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
		Model2 model = ((Model2)m_model);
		if (!model.CheckInput(this, 0, m_strDSZone, m_strDatasetZone, false))
			return false;
		if (!model.CheckInput(this, 1, m_strDSRaster, m_strDatasetRaster, false))
			return false;
		if (m_strStatisticsType.isEmpty())
		{
			model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"统计类型\"参数为空。", GetNestedName(), GetID()));
			return false;
		}
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("ZonalStatistics", ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSZone + "\",\"Dataset\":\"" + m_strDatasetZone + "\"}";
        analyst.SetPropertyValue("Zone", str);
        str = "{\"Datasource\":\"" + m_strDSRaster + "\",\"Dataset\":\"" + m_strDatasetRaster + "\"}";
        analyst.SetPropertyValue("Raster", str);

        str = m_strStatisticsType.replace("|", " ");
        analyst.SetPropertyValue("StatisticsType", str);
        str = m_strJoinFields.replace("|", " ");
        analyst.SetPropertyValue("JoinFields", str);

        str = "{\"Datasource\":\"" + m_strDSOutput + "\",\"Dataset\":\"" + m_strDatasetOutput + "\"}";
        analyst.SetPropertyValue("Output", str);

        boolean bResult = analyst.Execute();
        analyst.Destroy();
        
		return bResult;
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
		
	String m_strDSZone;
	String m_strDatasetZone;
	String m_strDSRaster;
	String m_strDatasetRaster;
	
	String m_strStatisticsType;
	String m_strJoinFields;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
