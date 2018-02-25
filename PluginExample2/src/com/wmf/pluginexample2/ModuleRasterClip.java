//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleRasterClip.java
// \brief RasterClip模块类
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

public class ModuleRasterClip extends ModuleBase
{
	public ModuleRasterClip(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.DatasetRaster));
		m_alInputPort.add(new Port(this, Port.Type.DatasetRegion));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRaster));
		
		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_strDSClipRegion = "";
		m_strDatasetClipRegion = "";
		m_strWhereClipRegion = "";
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("RasterClip%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "栅格裁剪";
	}
	public String GetDescription()
	{
		return "用面数据集裁剪栅格数据集。";
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
				m_strDSInput = (String)alParam.get(0).m_objValue;
				m_strDatasetInput = (String)alParam.get(1).m_objValue;
			}
			else if (i == 1)
			{
				m_strDSClipRegion = (String)alParam.get(0).m_objValue;
				m_strDatasetClipRegion = (String)alParam.get(1).m_objValue;
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
        	String strDatasources = model.GetWritableDatasources(true);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端/被裁剪栅格", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端/被裁剪栅格", Param.EditType.ReadOnly));
			
			alParam.add(new Param(Param.Datasource, m_strDSClipRegion, "数据源", "数据源别名", "输入端/裁剪面", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetClipRegion, "数据集", "数据集名称", "输入端/裁剪面", Param.EditType.ReadOnly));
			alParam.add(new Param("Where", m_strWhereClipRegion, "过滤条件", "例如：[NAME]='山西省'", "输入端/裁剪面", Param.EditType.Expression, 
					model.GetFields(m_strDSClipRegion, m_strDatasetClipRegion, true)));
			
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
			m_strDSClipRegion = (String)alParam.get(i++).m_objValue;
			m_strDatasetClipRegion = (String)alParam.get(i++).m_objValue;
			m_strWhereClipRegion = (String)alParam.get(i++).m_objValue;
			
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetInput + "@" + m_strDSInput);
			m_alInputPort.get(1).SetName(m_strDatasetClipRegion + "@" + m_strDSClipRegion);
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
		if (!model.CheckInput(this, 1, m_strDSClipRegion, m_strDatasetClipRegion, false))
			return false;
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("RasterClip", ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSInput + "\",\"Dataset\":\"" + m_strDatasetInput + "\"}";
        analyst.SetPropertyValue("Input", str);

        str = "{\"Datasource\":\"" + m_strDSClipRegion + "\",\"Dataset\":\"" + m_strDatasetClipRegion + "\",\"Where\":\"" + m_strWhereClipRegion + "\"}";
        if (!analyst.SetPropertyValue("ClipRegion", str))
        {
        	model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块的第1个输入端口\"过滤条件\"参数语法错误。", GetNestedName(), GetID()));
        	analyst.Destroy();
        	return false;
        }

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
	
	String m_strDSClipRegion;
	String m_strDatasetClipRegion;
	String m_strWhereClipRegion;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
