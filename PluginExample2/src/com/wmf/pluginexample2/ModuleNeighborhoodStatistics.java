//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleNeighborhoodStatistics.java
// \brief NeighborhoodStatistics模块类
// \author 王庆飞
// \date 2016-4-26
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleNeighborhoodStatistics extends ModuleBase
{
	public ModuleNeighborhoodStatistics(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.DatasetRaster));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRaster));
		
		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_strType = "Circle";
		m_dInnerRadius = 5.0;
		m_dOuterRadius = 10.0;
		m_dRadius = 5.0;
		m_dWidth = 10.0;
		m_dHeight = 10.0;
		m_dStartAngle = 0.0;
		m_dEndAngle = 120.0;
		m_strUnit = "Cell";
		
		m_strStatisticsType = "Mean";
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("NeighborhoodStatistics%d", m_nID);
    }

	public String GetGroupName()
	{
		return "邻域统计分析";
	}
	public String GetName()
	{
		return "栅格邻域统计";
	}
	public String GetDescription()
	{
		return "栅格邻域统计分析。";
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
			m_strDSInput = (String)alParam.get(0).m_objValue;
			m_strDatasetInput = (String)alParam.get(1).m_objValue;
			
			portTo.SetName(alParam.get(1).m_objValue + "@" + alParam.get(0).m_objValue);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	String GetDescription(String strType)
	{
		if (strType.equals("Annulus"))
			return "环面";
		else if (strType.equals("Circle"))
			return "圆";
		else if (strType.equals("Rectangle"))
			return "矩形";
		else if (strType.equals("Wedge"))
			return "楔形";
		return "";
	}
	String GetUnitDescription(String strUnit)
	{
		if (strUnit.equals("Cell"))
			return "栅格像元大小的倍数";
		else if (strUnit.equals("Map"))
			return "栅格数据集坐标单位";
		return "";
	}
	String GetStatisticsTypeDescription(String strStatisticsType)
	{
		if (strStatisticsType.equals("Mean"))
			return "平均值";
		else if (strStatisticsType.equals("Min"))
			return "最小值";
		else if (strStatisticsType.equals("Max"))
			return "最大值";
		else if (strStatisticsType.equals("Sum"))
			return "求和";
		return "";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
			Model2 model = ((Model2)m_model);
			String strDatasources = model.GetWritableDatasources(true);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
			alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param("Type", m_strType, "邻域类型", GetDescription(m_strType), "参数", Param.EditType.Select, "Annulus|Circle|Rectangle|Wedge"));
			
			alParam.add(new Param("InnerRadius", String.format("%f", m_dInnerRadius), "内半径", "", "参数/Annulus", m_strType.equals("Annulus") ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("OuterRadius", String.format("%f", m_dOuterRadius), "外半径", "", "参数/Annulus", m_strType.equals("Annulus") ? Param.EditType.Float : Param.EditType.ReadOnly));
			
			alParam.add(new Param("Radius", String.format("%f", m_dRadius), "半径", "", "参数/Circle", m_strType.equals("Circle") ? Param.EditType.Float : Param.EditType.ReadOnly));
			
			alParam.add(new Param("Width", String.format("%f", m_dWidth), "宽", "", "参数/Rectangle", m_strType.equals("Rectangle") ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("Height", String.format("%f", m_dHeight), "高", "", "参数/Rectangle", m_strType.equals("Rectangle") ? Param.EditType.Float : Param.EditType.ReadOnly));
			
			alParam.add(new Param("Radius", String.format("%f", m_dRadius), "半径", "", "参数/Wedge", m_strType.equals("Wedge") ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("StartAngle", String.format("%f", m_dStartAngle), "起始角", "", "参数/Wedge", m_strType.equals("Wedge") ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("EndAngle", String.format("%f", m_dEndAngle), "结束角", "", "参数/Wedge", m_strType.equals("Wedge") ? Param.EditType.Float : Param.EditType.ReadOnly));
			
			alParam.add(new Param("Unit", m_strUnit, "邻域单位", GetUnitDescription(m_strUnit), "参数", Param.EditType.Select, "Cell|Map"));
			
			alParam.add(new Param("StatisticsType", m_strStatisticsType, "统计类型", GetStatisticsTypeDescription(m_strStatisticsType), "参数", Param.EditType.Select, "Mean|Min|Max|Sum"));
	
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
	public boolean OnParamChanged(ArrayList<Param> alParam, int nIndex, Object objValue)
	{
		if (alParam.get(nIndex).m_strName.equals("Type"))
		{
			alParam.get(nIndex).m_strDescription = GetDescription((String)objValue);
			alParam.get(nIndex + 1).m_eEditType = objValue.equals("Annulus") ? Param.EditType.Float : Param.EditType.ReadOnly;
			alParam.get(nIndex + 2).m_eEditType = alParam.get(nIndex + 1).m_eEditType;
			alParam.get(nIndex + 3).m_eEditType = objValue.equals("Circle") ? Param.EditType.Float : Param.EditType.ReadOnly;
			alParam.get(nIndex + 4).m_eEditType = objValue.equals("Rectangle") ? Param.EditType.Float : Param.EditType.ReadOnly;
			alParam.get(nIndex + 5).m_eEditType = alParam.get(nIndex + 4).m_eEditType;
			alParam.get(nIndex + 6).m_eEditType = objValue.equals("Wedge") ? Param.EditType.Float : Param.EditType.ReadOnly;
			alParam.get(nIndex + 7).m_eEditType = alParam.get(nIndex + 6).m_eEditType;
			alParam.get(nIndex + 8).m_eEditType = alParam.get(nIndex + 6).m_eEditType;
			return true;
		}
		else if (alParam.get(nIndex).m_strName.equals("StatisticsType"))
		{
			alParam.get(nIndex).m_strDescription = GetStatisticsTypeDescription((String)objValue);
			return true;
		}
		else if (alParam.get(nIndex).m_strName.equals("Unit"))
		{
			alParam.get(nIndex).m_strDescription = GetUnitDescription((String)objValue);
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
			m_strDSInput = (String)alParam.get(i++).m_objValue;
			m_strDatasetInput = (String)alParam.get(i++).m_objValue;
			
			m_strType = (String)alParam.get(i++).m_objValue;
			
			m_dInnerRadius = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dOuterRadius = Double.parseDouble((String)alParam.get(i++).m_objValue);
			
			m_dRadius = Double.parseDouble((String)alParam.get(i++).m_objValue);
			
			m_dWidth = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dHeight = Double.parseDouble((String)alParam.get(i++).m_objValue);
			
			m_dRadius = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dStartAngle = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dEndAngle = Double.parseDouble((String)alParam.get(i++).m_objValue);
			
			m_strUnit = (String)alParam.get(i++).m_objValue;
			
			m_strStatisticsType = (String)alParam.get(i++).m_objValue;
			
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetInput + "@" + m_strDSInput);
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
		if (!model.CheckInput(this, 0, m_strDSInput, m_strDatasetInput, false))
			return false;
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("NeighborhoodStatistics", ws);
        String str = "{\"Datasource\":\"" + m_strDSInput + "\",\"Dataset\":\"" + m_strDatasetInput + "\"}";
        analyst.SetPropertyValue("Input", str);
        
        if (m_strType.equalsIgnoreCase("Annulus"))
        {
	        str = String.format("{\"Type\":\"%s\",\"InnerRadius\":%f,\"OuterRadius\":%f,\"Unit\":\"%s\"}", m_strType, m_dInnerRadius, m_dOuterRadius, m_strUnit);
        }
        else if (m_strType.equalsIgnoreCase("Circle"))
        {
        	str = String.format("{\"Type\":\"%s\",\"Radius\":%f,\"Unit\":\"%s\"}", m_strType, m_dRadius, m_strUnit);
        }
        else if (m_strType.equalsIgnoreCase("Rectangle"))
        {
        	str = String.format("{\"Type\":\"%s\",\"Width\":%f,\"Height\":%f,\"Unit\":\"%s\"}", m_strType, m_dWidth, m_dHeight, m_strUnit);
        }
        else if (m_strType.equalsIgnoreCase("Wedge"))
        {
        	str = String.format("{\"Type\":\"%s\",\"Radius\":%f,\"StartAngle\":%f,\"EndAngle\":%f,\"Unit\":\"%s\"}", m_strType, m_dRadius, m_dStartAngle, m_dEndAngle, m_strUnit);
        }
        analyst.SetPropertyValue("Neighborhood", str);

        analyst.SetPropertyValue("StatisticsType", m_strStatisticsType);

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
	
	String m_strDSInput;
	String m_strDatasetInput;
	
	String m_strType;
	double m_dInnerRadius, m_dOuterRadius;
	double m_dRadius;
	double m_dWidth, m_dHeight;
	double m_dStartAngle, m_dEndAngle;
	String m_strUnit;
	
	String m_strStatisticsType;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
