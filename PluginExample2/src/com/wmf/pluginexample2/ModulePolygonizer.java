//-------------------------------------------------------------
// \project PluginExample2
// \file ModulePolygonizer.java
// \brief Polygonizer模块类
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

public class ModulePolygonizer extends ModuleBase
{
	public ModulePolygonizer(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new DatasetPort(this, Port.Type.DatasetLine));
		m_alInputPort.add(new DatasetPort(this));
		m_alInputPort.add(new DatasetPort(this, Port.Type.DatasetRaster));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRegion));
		
		m_strField = "";
		m_dMinValue = 0.0;
		m_dMaxValue = 0.0;
		m_dNoDataValue = -9999;
		m_bCalcValue = true;
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("Polygonizer%d", m_nID);
    }

	public String GetGroupName()
	{
		return "拓扑分析";
	}
	public String GetName()
	{
		return "拓扑构面";
	}
	public String GetDescription()
	{
		return "线数据集拓扑构面。";
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
			if (portTo.GetType() == Port.Type.Unknown)
			{
				if (portFrom.GetType() != Port.Type.DatasetLine)
					return false;
			}
			else
			{
				if (portFrom.GetType() != portTo.GetType())
					return false;
			}
				
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() < 2)
				return false;
			DatasetPort dp = (DatasetPort)portTo;
			dp.m_strDS = (String)alParam.get(0).m_objValue;
			dp.m_strDataset = (String)alParam.get(1).m_objValue;				
			if (portTo.GetType() == Port.Type.Unknown)
			{
				portTo.SetType(Port.Type.DatasetLine);
				while (i >= m_alInputPort.size() - 2)
					InsertPort(m_alInputPort.size() - 1, new DatasetPort(this), true);
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
	public boolean OnDetach(Port portFrom, Port portTo)
	{
		if (!super.OnDetach(portFrom, portTo))
			return false;
		try
		{
			int i = FindPort(portTo, true);
			if (i == -1)
				return true;
			if (i > 0 && i < m_alInputPort.size() - 2)
			{
				RemovePort(i, true);
			}
			else if (i == m_alInputPort.size() - 1)
			{
				((DatasetPort)portTo).m_strDS = "";
				((DatasetPort)portTo).m_strDataset = "";
			}
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
			Model2 model = ((Model2)m_model);
			
			DatasetPort dp = (DatasetPort)m_alInputPort.get(0);
			String strOptionalValues = model.GetFields(dp.m_strDS, dp.m_strDataset, false);
			if (m_strField.isEmpty() && !strOptionalValues.isEmpty())
				m_strField = strOptionalValues.split("\\|")[0];
			
        	String strDatasources = model.GetWritableDatasources(false);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	for (int i = 0; i < m_alInputPort.size() - 1; i++)
			{
        		dp = (DatasetPort)m_alInputPort.get(i);
				alParam.add(new Param(Param.Datasource, dp.m_strDS, "数据源", "数据源别名", "输入端/线数据", Param.EditType.ReadOnly));
				alParam.add(new Param(Param.Dataset, dp.m_strDataset, "数据集", "数据集名称", "输入端/线数据", Param.EditType.ReadOnly));
			}
			
        	dp = (DatasetPort)m_alInputPort.get(m_alInputPort.size() - 1);
			alParam.add(new Param(Param.Datasource, dp.m_strDS, "数据源", "栅格数据源别名", "输入端/参考栅格", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, dp.m_strDataset, "数据集", "栅格数据集名称", "输入端/参考栅格", Param.EditType.ReadOnly));
			
			alParam.add(new Param("Field", m_strField, "字段名", "", "参数", Param.EditType.SelectOrInput, strOptionalValues));
			dp = (DatasetPort)m_alInputPort.get(m_alInputPort.size() - 1);
			alParam.add(new Param("MinValue", String.format("%f", m_dMinValue), "最小值", "", "参数", dp.m_strDataset.isEmpty() ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("MaxValue", String.format("%f", m_dMaxValue), "最大值", "", "参数", dp.m_strDataset.isEmpty() ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("NoDataValue", String.format("%f", m_dNoDataValue), "无效值", "", "参数", dp.m_strDataset.isEmpty() ? Param.EditType.Float : Param.EditType.ReadOnly));
			alParam.add(new Param("CalcValue", m_bCalcValue ? "true" : "false", "是否计算值", "", "参数", Param.EditType.Select, "false|true"));
	
			alParam.add(new Param(Param.Datasource, m_strDSOutput, "数据源", "数据源别名", "输出端/拓扑面", Param.EditType.SelectOrInput, strDatasources));
			alParam.add(new Param(Param.Dataset, m_strDatasetOutput, "数据集", "数据集名称", "输出端/拓扑面"));
			
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
			int j = 0;
			while (i < alParam.size() - 7)
			{
				if (j == m_alInputPort.size())
					m_alInputPort.add(new DatasetPort(this));
				
				DatasetPort dp = (DatasetPort)m_alInputPort.get(j);
				dp.m_strDS = (String)alParam.get(i++).m_objValue;
				dp.m_strDataset = (String)alParam.get(i++).m_objValue;
				dp.SetName(dp.m_strDataset + "@" + dp.m_strDS);

				++j;
			}
			
			m_strField = (String)alParam.get(i++).m_objValue;
			m_dMinValue = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dMaxValue = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dNoDataValue = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_bCalcValue = Boolean.parseBoolean((String)alParam.get(i++).m_objValue);
			
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
		Model2 model = ((Model2)m_model);
//		for (int i = 0; i < m_alInputPort.size(); i++)
//		{
//			DatasetPort dp = (DatasetPort)m_alInputPort.get(i);
//			if (dp.GetType() == Port.Type.Unknown)
//				continue;
//			if (!model.CheckInput(this, i, dp.m_strDS, dp.m_strDataset, i == m_alInputPort.size() - 1))
//				return false;
//		}
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("Polygonizer", ws);
		
		String str;
		for (int i = 0; i < m_alInputPort.size() - 2; i++)
		{
			DatasetPort dp = (DatasetPort)m_alInputPort.get(i);
			str = "{\"Datasource\":\"" + dp.m_strDS + "\",\"Dataset\":\"" + dp.m_strDataset + "\"}";
	        analyst.AddPropertyValue("Input", str);
		}
		
		DatasetPort dp = (DatasetPort)m_alInputPort.get(m_alInputPort.size() - 1);
		str = "{\"Datasource\":\"" + dp.m_strDS + "\",\"Dataset\":\"" + dp.m_strDataset + "\"}";
        analyst.SetPropertyValue("Ref", str);

        double dMinValue = m_dMinValue, dMaxValue = m_dMaxValue, dNoDataValue = m_dNoDataValue; 
    	Datasource ds = dp.m_strDS.isEmpty() ? null : ws.GetDatasource(dp.m_strDS);
    	if (ds != null)
    	{
    		DatasetRaster dr = (DatasetRaster)ds.GetDataset(dp.m_strDataset);
    		if (dr != null)
    		{
				dMinValue = dr.GetMinValue();
				dMaxValue = dr.GetMaxValue();
				dNoDataValue = dr.GetNoDataValue();
    		}
    	}
    	
        str = String.format("\"Name\":\"%s\",\"MinValue\":%f,\"MaxValue\":%f,\"NoDataValue\":%f,\"CalcValue\":%s", 
        	m_strField, dMinValue, dMaxValue, dNoDataValue, m_bCalcValue ? "true" : "false");
        analyst.SetPropertyValue("Field", "{" + str + "}");

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
	
	String m_strField;
	double m_dMinValue, m_dMaxValue, m_dNoDataValue;
	boolean m_bCalcValue;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
