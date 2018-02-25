//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleOverlay.java
// \brief Overlay模块类
// \author 王庆飞
// \date 2016-7-21
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleOverlay extends ModuleBase
{
	public ModuleOverlay(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.DatasetRegion));
		m_alInputPort.add(new Port(this));
		m_alOutputPort.add(new Port(this));
		
		m_strDSA = "";
		m_strDatasetA = "";
		m_strDSB = "";
		m_strDatasetB = "";
		
		m_strType = "Intersect";
		m_strAJoinFields = "";
		m_strBJoinFields = "";
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("Overlay%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "叠加分析";
	}
	public String GetDescription()
	{
		return "面数据集和点、线、面数据集叠加分析，生成点、线、面数据集。";
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
			if (i == 0)
			{
				if (portFrom.GetType() != portTo.GetType())
					return false;
			}
			else if (i == 1)
			{
				if (portFrom.GetType() != Port.Type.DatasetPoint && portFrom.GetType() != Port.Type.DatasetLine && portFrom.GetType() != Port.Type.DatasetRegion)
					return false;
			}
			
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() < 2)
				return false;
			if (i == 0)
			{
				m_strDSA = (String)alParam.get(0).m_objValue;
				m_strDatasetA = (String)alParam.get(1).m_objValue;
			}
			else if (i == 1)
			{
				m_strDSB = (String)alParam.get(0).m_objValue;
				m_strDatasetB = (String)alParam.get(1).m_objValue;
				
				portTo.SetType(portFrom.GetType());
				m_alOutputPort.get(0).SetType(m_alInputPort.get(1).GetType());
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
			alParam.add(new Param(Param.Datasource, m_strDSA, "数据源", "数据源别名", "输入端/A", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetA, "数据集", "数据集名称", "输入端/A", Param.EditType.ReadOnly));			
			alParam.add(new Param(Param.Datasource, m_strDSB, "数据源", "数据源别名", "输入端/B", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetB, "数据集", "数据集名称", "输入端/B", Param.EditType.ReadOnly));
			
			alParam.add(new Param("Type", m_strType, "类型", "", "参数", Param.EditType.Select, "Intersect"));
			alParam.add(new Param("AJoinFields", m_strAJoinFields, "A连接字段", "", "参数", Param.EditType.MultiSelect, model.GetFields(m_strDSA, m_strDatasetA, true)));
			alParam.add(new Param("BJoinFields", m_strBJoinFields, "B连接字段", "", "参数", Param.EditType.MultiSelect, model.GetFields(m_strDSB, m_strDatasetB, true)));
			
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
			m_strDSA = (String)alParam.get(i++).m_objValue;
			m_strDatasetA = (String)alParam.get(i++).m_objValue;
			m_strDSB = (String)alParam.get(i++).m_objValue;
			m_strDatasetB = (String)alParam.get(i++).m_objValue;
			
			m_strType = (String)alParam.get(i++).m_objValue;
			m_strAJoinFields = (String)alParam.get(i++).m_objValue;
			m_strBJoinFields = (String)alParam.get(i++).m_objValue;
			
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetA + "@" + m_strDSA);
			m_alInputPort.get(1).SetName(m_strDatasetB + "@" + m_strDSB);
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
		if (!model.CheckInput(this, 0, m_strDSA, m_strDatasetA, false))
			return false;
		if (!model.CheckInput(this, 1, m_strDSB, m_strDatasetB, false))
			return false;
		
		Workspace ws = model.m_ws;
		Analyst analyst = Analyst.CreateInstance(m_strType, ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSA + "\",\"Dataset\":\"" + m_strDatasetA + "\"}";
		analyst.SetPropertyValue("A", str);

        str = "{\"Datasource\":\"" + m_strDSB + "\",\"Dataset\":\"" + m_strDatasetB + "\"}";
        analyst.SetPropertyValue("B", str);
        
        analyst.SetPropertyValue("AJoinFields", m_strAJoinFields.replace("|", " "));
        analyst.SetPropertyValue("BJoinFields", m_strBJoinFields.replace("|", " "));

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
		
	String m_strDSA;
	String m_strDatasetA;
	String m_strDSB;
	String m_strDatasetB;
	
	String m_strType;
	String m_strAJoinFields;
	String m_strBJoinFields;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
