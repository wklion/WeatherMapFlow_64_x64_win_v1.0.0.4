//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleRasterizer.java
// \brief 栅格化模块类
// \author 王庆飞
// \date 2016-10-31
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleRasterizer extends ModuleBase
{
	public ModuleRasterizer(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.Unknown));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRaster));
		
		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_rcBounds = new Rectangle2D.Double();
		m_strCellSize = "0.05 0.05";
		m_strCellValueType = "Single";
		m_strField = "";
		m_dNoDataValue = -9999;
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("Rasterizer%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "栅格化";
	}
	public String GetDescription()
	{
		return "输入线、面数据集，生成栅格数据集。";
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
			if (portFrom.GetType() != Port.Type.DatasetLine && portFrom.GetType() != Port.Type.DatasetRegion)
				return false;
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() < 2)
				return false;
			m_strDSInput = (String)alParam.get(0).m_objValue;
			m_strDatasetInput = (String)alParam.get(1).m_objValue;
			
			portTo.SetType(portFrom.GetType());
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
			Workspace ws = model.m_ws;
			
			Datasource ds = m_strDSInput.isEmpty() ? null : ws.GetDatasource(m_strDSInput);
        	if (ds != null)
        	{
        		DatasetVector dv = (DatasetVector)ds.GetDataset(m_strDatasetInput);    
        		if (dv != null)
        		{
					if (m_rcBounds.isEmpty())
						m_rcBounds = dv.GetBounds();
        		}
        	}
        	
        	String strOptionalValues = model.GetFields(m_strDSInput, m_strDatasetInput, false);
			if (m_strField.isEmpty() && !strOptionalValues.isEmpty())
				m_strField = strOptionalValues.split("\\|")[0];
        	
        	String strDatasources = model.GetWritableDatasources(m_alOutputPort.get(0).GetType() == Port.Type.DatasetRaster);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param(Param.Bounds, String.format("%f %f %f %f", m_rcBounds.getMinX(), m_rcBounds.getMinY(), m_rcBounds.getMaxX(), m_rcBounds.getMaxY()), 
					"栅格化范围", "left bottom right top", "参数", Param.EditType.FixedFloatArray));
			alParam.add(new Param(Param.CellSize, m_strCellSize, "插值分辨率", "x y", "参数", Param.EditType.FixedFloatArray));		
			alParam.add(new Param(Param.ValueType, m_strCellValueType, "栅格值类型", "", "参数", Param.EditType.Select, "UInt8|Int8|UInt16|Int16|UInt32|Int32|Single|Double"));
			alParam.add(new Param(Param.Field, m_strField, "属性值字段", "", "参数", Param.EditType.SelectOrInput, strOptionalValues));
			alParam.add(new Param("NoDataValue", String.format("%f", m_dNoDataValue), "无效值", "", "参数", Param.EditType.Float));
						
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
			m_strDSInput = (String)alParam.get(i++).m_objValue;
			m_strDatasetInput = (String)alParam.get(i++).m_objValue;
			
			String[] strs = ((String)alParam.get(i++).m_objValue).split(" ");
			m_rcBounds = new Rectangle2D.Double(Double.parseDouble(strs[0]), Double.parseDouble(strs[1]), 
					Double.parseDouble(strs[2]) - Double.parseDouble(strs[0]), Double.parseDouble(strs[3]) - Double.parseDouble(strs[1]));
			m_strCellSize = (String)alParam.get(i++).m_objValue;
			m_strCellValueType = (String)alParam.get(i++).m_objValue;
			m_strField = (String)alParam.get(i++).m_objValue;
			m_dNoDataValue = Double.parseDouble((String)alParam.get(i++).m_objValue);
			
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
		
		Analyst analyst = Analyst.CreateInstance("Rasterizer", ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSInput + "\",\"Dataset\":\"" + m_strDatasetInput + "\"}";
        analyst.SetPropertyValue("Input", str);

        str = String.format("{\"left\":%f,\"bottom\":%f,\"right\":%f,\"top\":%f}", m_rcBounds.getMinX(), m_rcBounds.getMinY(), m_rcBounds.getMaxX(), m_rcBounds.getMaxY());
        analyst.SetPropertyValue("Bounds", str);
        
        analyst.SetPropertyValue("CellSize", m_strCellSize); //栅格分辨率
        analyst.SetPropertyValue("CellValueType", m_strCellValueType);
        
        analyst.SetPropertyValue("Field", m_strField);
        analyst.SetPropertyValue("NoDataValue", String.format("%f", m_dNoDataValue)); 
            	        
        str = "{\"Datasource\":\"" + m_strDSOutput + "\",\"Dataset\":\"" + m_strDatasetOutput + "\"}";
        analyst.SetPropertyValue("Raster", str);

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
	
	Rectangle2D m_rcBounds;
	String m_strCellSize;
	String m_strCellValueType;
	String m_strField;
	double m_dNoDataValue;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
