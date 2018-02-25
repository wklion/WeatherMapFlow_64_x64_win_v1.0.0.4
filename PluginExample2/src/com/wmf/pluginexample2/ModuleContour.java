//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleContour.java
// \brief Contour模块类
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

public class ModuleContour extends ModuleBase
{
	public ModuleContour(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.DatasetRaster));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetLine));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetLine));
		
		m_strDSRaster = "";
		m_strDatasetRaster = "";
		
		m_strValues = "";
		m_nSmoothness = 3;
		
		m_strDSContour = "";
		m_strDatasetContour = String.format("Contour%d", m_nID);
		m_strDSValidBorder = "";
		m_strDatasetValidBorder = String.format("ValidBorder%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "提取\n等值线";
	}
	public String GetDescription()
	{
		return "从栅格数据集提取等值线。";
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
			m_strDSRaster = (String)alParam.get(0).m_objValue;
			m_strDatasetRaster = (String)alParam.get(1).m_objValue;
			
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
			
			Model2 model = ((Model2)m_model);
			String strDatasources = model.GetWritableDatasources(false);
        	if (m_strDSContour.isEmpty() && !strDatasources.isEmpty())
        		m_strDSContour = strDatasources.split("\\|")[0];
        	if (m_strDSValidBorder.isEmpty() && !strDatasources.isEmpty())
        		m_strDSValidBorder = strDatasources.split("\\|")[0];
			//-------------------------------------------------------------------------------------------------------------------
			Workspace ws = model.m_ws;
			Datasource ds = m_strDSRaster.isEmpty() ? null : ws.GetDatasource(m_strDSRaster);
        	if (ds != null)
        	{
        		DatasetRaster dr = (DatasetRaster)ds.GetDataset(m_strDatasetRaster);
        		if (dr != null)
        		{
					if (m_strValues.isEmpty())
					{
	                    double v = dr.GetMinValue(), dStep = (dr.GetMaxValue() - dr.GetMinValue()) / 10;
	                    for (int j = 0; j < 10; j++)
	                    {
	                    	m_strValues += String.format("%f", v);
	                        if (j < 10)
	                        	m_strValues += " ";
	                        v += dStep;
	                    }
					}
        		}
        	}
        	//-------------------------------------------------------------------------------------------------------------------
			alParam.add(new Param(Param.Datasource, m_strDSRaster, "数据源", "栅格数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetRaster, "数据集", "栅格数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param("Values", m_strValues, "提取值", "", "参数", Param.EditType.FloatArray));
			alParam.add(new Param("Smoothness", String.format("%d", m_nSmoothness), "光滑度", "", "参数", Param.EditType.UInt));
	
			alParam.add(new Param(Param.Datasource, m_strDSContour, "数据源", "数据源别名", "输出端/等值线", Param.EditType.SelectOrInput, strDatasources));
			alParam.add(new Param(Param.Dataset, m_strDatasetContour, "数据集", "数据集名称", "输出端/等值线"));
			alParam.add(new Param(Param.Datasource, m_strDSValidBorder, "数据源", "数据源别名", "输出端/有效边界", Param.EditType.SelectOrInput, strDatasources));
			alParam.add(new Param(Param.Dataset, m_strDatasetValidBorder, "数据集", "数据集名称", "输出端/有效边界"));
			
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
			m_strDSRaster = (String)alParam.get(i++).m_objValue;
			m_strDatasetRaster = (String)alParam.get(i++).m_objValue;
			
			m_strValues = (String)alParam.get(i++).m_objValue;
			m_nSmoothness = Integer.parseInt((String)alParam.get(i++).m_objValue);
			
			m_strDSContour = (String)alParam.get(i++).m_objValue;
			m_strDatasetContour = (String)alParam.get(i++).m_objValue;
			m_strDSValidBorder = (String)alParam.get(i++).m_objValue;
			m_strDatasetValidBorder = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetRaster + "@" + m_strDSRaster);
			m_alOutputPort.get(0).SetName(m_strDatasetContour + "@" + m_strDSContour);
			m_alOutputPort.get(1).SetName(m_strDatasetValidBorder + "@" + m_strDSValidBorder);
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
		if (!model.CheckInput(this, 0, m_strDSRaster, m_strDatasetRaster, false))
			return false;
		if (m_strValues.isEmpty())
		{
			model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"提取值\"参数为空。", GetNestedName(), GetID()));
			return false;
		}
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("Contour", ws);
		String str = "{\"Datasource\":\"" + m_strDSRaster + "\",\"Dataset\":\"" + m_strDatasetRaster + "\"}";
        analyst.SetPropertyValue("Raster", str);

        analyst.SetPropertyValue("Values", m_strValues);
        analyst.SetPropertyValue("Smoothness", String.format("%d", m_nSmoothness));

        str = "{\"Datasource\":\"" + m_strDSContour + "\",\"Dataset\":\"" + m_strDatasetContour + "\"}";
        analyst.SetPropertyValue("Contour", str);
        str = "{\"Datasource\":\"" + m_strDSValidBorder + "\",\"Dataset\":\"" + m_strDatasetValidBorder + "\"}";
        analyst.SetPropertyValue("ValidBorder", str);

        boolean bResult = analyst.Execute();
        analyst.Destroy();
        
		return bResult;
	}
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		if (i == 0)
		{
			alParam.add(new Param(Param.Datasource, m_strDSContour));
			alParam.add(new Param(Param.Dataset, m_strDatasetContour));
		}
		else if (i == 1)
		{
			alParam.add(new Param(Param.Datasource, m_strDSValidBorder));
			alParam.add(new Param(Param.Dataset, m_strDatasetValidBorder));
		}
		
		return alParam.size();
	}
	
	String m_strDSRaster;
	String m_strDatasetRaster;
	
	String m_strValues;
	int m_nSmoothness;
	
	String m_strDSContour;
	String m_strDatasetContour;
	String m_strDSValidBorder;
	String m_strDatasetValidBorder;
}
