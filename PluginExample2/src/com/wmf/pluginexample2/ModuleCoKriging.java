//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleCoKriging.java
// \brief CoKriging模块类
// \author 王庆飞
// \date 2017-6-7
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.io.File;
import java.util.ArrayList;

import org.codehaus.jettison.json.JSONObject;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleCoKriging extends ModuleKriging
{
	public ModuleCoKriging(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new DatasetPort(this, Port.Type.DatasetRaster));
		
		m_strDatasetRaster = String.format("CoKriging%d", m_nID);
    }

	public String GetName()
	{
		return "协同克里金\n插值";
	}
	public String GetDescription()
	{
		return "协同克里金插值站点到格点。";
	}
	
	public boolean OnAttach(Port portFrom, Port portTo)
	{
		try
		{
			if (!super.OnAttach(portFrom, portTo))
				return false;
			int i = FindPort(portFrom, false);
			if (i != -1)
				return true;
			i = FindPort(portTo, true);
			if (i == -1)
				return false;
			if (i == m_alInputPort.size() - 1)
				InsertPort(-1, new DatasetPort(this, Port.Type.DatasetRaster), true);
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
			if (i >= 1 && i < m_alInputPort.size() - 1)
			{
				RemovePort(i, true);
			}
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}
	
	public int GetParam(ArrayList<Param> alParam)
	{
		int nOffset = super.GetParam(alParam);
		
		for (int i = 1; i < m_alInputPort.size(); i++)
		{
			alParam.get(i * 2).m_strGroups = String.format("输入端/辅助变量%d", i);
			alParam.get(i * 2 + 1).m_strGroups = String.format("输入端/辅助变量%d", i);
		}
		
		return nOffset;
	}
	
	public int SetParam(final ArrayList<Param> alParam)
	{
		int i = super.SetParam(alParam);
		
		for (int j = 1; j < m_alInputPort.size(); j++)
			m_alInputPort.get(j).SetType(Port.Type.DatasetRaster);
		
		return i;
	}
	
	public boolean Execute()
	{
		try
		{
			if (m_alInputPort.size() <= 2)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块辅助变量个数为0。", GetNestedName(), GetID()));
				return false;
			}
			DatasetPort dp = null;
			Model2 model = ((Model2)m_model);
			for (int i = 0; i < m_alInputPort.size() - 1; i++)
			{
				dp = (DatasetPort)m_alInputPort.get(i);
				if (!model.CheckInput(this, i, dp.m_strDS, dp.m_strDataset, i == m_alInputPort.size() - 1))
					return false;
			}
			Workspace ws = model.m_ws;
			
			Analyst analyst = Analyst.CreateInstance("CoKriging", ws);
			dp = (DatasetPort)m_alInputPort.get(0);
	        String str = "{\"Datasource\":\"" + dp.m_strDS + "\",\"Dataset\":\"" + dp.m_strDataset + "\"}";
	        analyst.SetPropertyValue("Point", str); //站点数据
	
	        analyst.SetPropertyValue("Field", m_strField); //插值字段
	        
	        for (int i = 1; i < m_alInputPort.size() - 1; i++)
	        {
	        	dp = (DatasetPort)m_alInputPort.get(i);
	        	analyst.AddPropertyValue("Variable", "{\"Datasource\":\"" + dp.m_strDS + "\",\"Dataset\":\"" + dp.m_strDataset + "\"}"); //添加辅助变量
	        }
	
	        str = String.format("{\"left\":%f,\"bottom\":%f,\"right\":%f,\"top\":%f}", m_rcBounds.getMinX(), m_rcBounds.getMinY(), m_rcBounds.getMaxX(), m_rcBounds.getMaxY());
	        analyst.SetPropertyValue("Bounds", str); //插值范围
	
	        analyst.SetPropertyValue("CellSize", m_strCellSize); //栅格分辨率
	        analyst.SetPropertyValue("CellValueType", m_strCellValueType); //栅格值类型
	
	        analyst.SetPropertyValue("SearchMode", m_strSearchMode); 
	        if (m_strSearchMode.equalsIgnoreCase("RadiusVariable"))
	        {
		        str = String.format("{\"PointCount\":%d,\"MaxRadius\":%d}", m_nPointCount, m_nMaxRadius);
		        analyst.SetPropertyValue("RadiusVariable", str);
	        }
	        else if (m_strSearchMode.equalsIgnoreCase("RadiusFixed"))
	        {
		        str = String.format("{\"Radius\":%d,\"MinPointCount\":%d}", m_nRadius, m_nMinPointCount);
		        analyst.SetPropertyValue("RadiusFixed", str);
	        }
	
	        analyst.SetPropertyValue("SemiVariogramModel", m_strSemiVariogramModel);
	        analyst.SetPropertyValue("Range", String.format("%f", m_dRange));
	        analyst.SetPropertyValue("Nugget", String.format("%f", m_dNugget));
	        analyst.SetPropertyValue("Sill", String.format("%f", m_dSill));
	        analyst.SetPropertyValue("a", String.format("%f", m_a));
	        analyst.SetPropertyValue("b", String.format("%f", m_b));
	
	        analyst.SetPropertyValue("CrossValidation", m_bCrossValidation ? "true" : "false"); //是否交叉验证，默认值为false
	        
	        String strResidualFileName = m_strResidualFileName;
			if (!new File(strResidualFileName).isAbsolute())
				strResidualFileName = new File(m_model.m_strFileName).getParent() + "/" + strResidualFileName;
			analyst.SetPropertyValue("ResidualFileName", strResidualFileName); //残差文件名
	
	        str = "{\"Datasource\":\"" + m_strDSRaster + "\",\"Dataset\":\"" + m_strDatasetRaster + "\"}";
	        analyst.SetPropertyValue("Raster", str);
	
	        boolean bResult = analyst.Execute();
	        if (bResult)
	        {
	        	if (m_bCrossValidation)
	        	{
	        		m_model.OutputLog(Model.LogLevel.Info, String.format("\"%s-%d\"模块\"交叉验证结果：", GetNestedName(), GetID()));
	        		str = analyst.GetPropertyValue("CrossValidationResult");
	        		JSONObject jsonObj = new JSONObject(str);
	        		m_model.OutputLog(Model.LogLevel.Info, "平均误差：" + jsonObj.getString("ME"));
	        		m_model.OutputLog(Model.LogLevel.Info, "平均绝对误差：" + jsonObj.getString("MAE"));
	        		m_model.OutputLog(Model.LogLevel.Info, "均方根误差：" + jsonObj.getString("RMSE"));
	        		m_model.OutputLog(Model.LogLevel.Info, "决定系数：" + jsonObj.getString("R2"));
	        	}
	        }
	        analyst.Destroy();
	        
			return bResult;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
}
