//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleProjTransformer.java
// \brief ProjTransformer模块类
// \author 王庆飞
// \date 2016-7-18
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleProjTransformer extends ModuleBase
{
	public ModuleProjTransformer(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.Unknown));
		m_alOutputPort.add(new Port(this, Port.Type.Unknown));
		
		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_strOutputProj = "";
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("ProjTransformer%d", m_nID);
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "投影转换";
	}
	public String GetDescription()
	{
		return "投影转换数据集。";
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
			if (alParam.size() < 2)
				return false;
			if (i == 0)
			{
				m_strDSInput = (String)alParam.get(0).m_objValue;
				m_strDatasetInput = (String)alParam.get(1).m_objValue;
				
				portTo.SetType(portFrom.GetType());
				portTo.SetName(alParam.get(1).m_objValue + "@" + alParam.get(0).m_objValue);
				
				m_alOutputPort.get(0).SetType(portTo.GetType());
			}
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
        	String strDatasources = model.GetWritableDatasources(m_alOutputPort.get(0).GetType() == Port.Type.DatasetRaster);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param("OutputProj", m_strOutputProj, "输出投影", "proj4格式", "参数", Param.EditType.Proj));
			
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
			
			m_strOutputProj = (String)alParam.get(i++).m_objValue;
			
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
		if (m_strOutputProj.isEmpty())
		{
			model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"输出投影\"参数为空。", GetNestedName(), GetID()));
			return false;
		}
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("ProjTransformer", ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSInput + "\",\"Dataset\":\"" + m_strDatasetInput + "\"}";
        analyst.SetPropertyValue("Input", str);

        if (!analyst.SetPropertyValue("OutputProj", m_strOutputProj))
        {
        	model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"输出投影\"参数无效。", GetNestedName(), GetID()));
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
	
	String m_strOutputProj;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
