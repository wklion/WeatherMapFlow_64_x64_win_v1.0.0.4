//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleBuffer.java
// \brief Buffer模块类
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

public class ModuleBuffer extends ModuleBase
{
	public ModuleBuffer(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.Unknown));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRegion));
		
		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_strDistanceUnit = "DEGREE";
		m_strDistance = "";
		m_strLineSide = "Full";
		m_strLineEndType = "Round";
		m_strLineJoinType = "Round";
		m_nInterpolation = 6;
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("Buffer%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "缓冲区分析";
	}
	public String GetDescription()
	{
		return "输入点、线、面数据集，生成缓冲区面数据集。";
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
			if (portFrom.GetType() != Port.Type.DatasetPoint && portFrom.GetType() != Port.Type.DatasetLine && portFrom.GetType() != Port.Type.DatasetRegion)
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
        	String strDatasources = model.GetWritableDatasources(m_alOutputPort.get(0).GetType() == Port.Type.DatasetRaster);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param("DistanceUnit", m_strDistanceUnit, "距离单位", "", "参数", Param.EditType.Select, "MM|CM|DM|M|KM|DEGREE"));
			alParam.add(new Param("Distance", m_strDistance, "距离", "支持算术表达式", "参数", Param.EditType.Expression, model.GetFields(m_strDSInput, m_strDatasetInput, true)));
			alParam.add(new Param("LineSide", m_strLineSide, "边类型", "双边|左边|右边，只用于线缓冲", "参数", Param.EditType.Select, "Full|Left|Right"));
			alParam.add(new Param("LineEndType", m_strLineEndType, "线端类型", "平头|圆头|方头，只用于线缓冲", "参数", Param.EditType.Select, "Flat|Round|Square"));
			alParam.add(new Param("LineJoinType", m_strLineJoinType, "线连接类型", "圆角|斜角|斜切，用于线、面缓冲", "参数", Param.EditType.Select, "Round|Miter|Bevel"));
			alParam.add(new Param("Interpolation", String.format("%d", m_nInterpolation), "插值点数", "90度圆弧插值点数", "参数", Param.EditType.UInt));
			
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
			
			m_strDistanceUnit = (String)alParam.get(i++).m_objValue;
			m_strDistance = (String)alParam.get(i++).m_objValue;
			m_strLineSide = (String)alParam.get(i++).m_objValue;
			m_strLineEndType = (String)alParam.get(i++).m_objValue;
			m_strLineJoinType = (String)alParam.get(i++).m_objValue;
			m_nInterpolation = Integer.parseInt((String)alParam.get(i++).m_objValue);
			
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
		if (m_strDistance.isEmpty())
		{
			model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"距离\"参数为空。", GetNestedName(), GetID()));
			return false;
		}
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("Buffer", ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSInput + "\",\"Dataset\":\"" + m_strDatasetInput + "\"}";
        analyst.SetPropertyValue("Input", str);

        str = "{\"Unit\":\"" + m_strDistanceUnit + "\",\"Value\":\"" + m_strDistance + "\"}";
        if (!analyst.SetPropertyValue("Distance", str))
        {
        	model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"距离\"参数语法错误。", GetNestedName(), GetID()));
        	analyst.Destroy();
        	return false;
        }
        
        if (m_alInputPort.get(0).GetType() == Port.Type.DatasetLine)
        {
        	analyst.SetPropertyValue("LineSide", m_strLineSide);
        	analyst.SetPropertyValue("LineEndType", m_strLineEndType);
        }
        if (m_alInputPort.get(0).GetType() == Port.Type.DatasetLine || m_alInputPort.get(0).GetType() == Port.Type.DatasetRegion)
        {
        	analyst.SetPropertyValue("LineJoinType", m_strLineJoinType);
        }
        analyst.SetPropertyValue("Interpolation", String.format("%d", m_nInterpolation));
        
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
	
	String m_strDistanceUnit;
	String m_strDistance;
	String m_strLineSide;
	String m_strLineEndType;
	String m_strLineJoinType;
	int m_nInterpolation;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
