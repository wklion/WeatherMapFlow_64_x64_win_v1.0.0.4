//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleReclass.java
// \brief Reclass模块类
// \author 王庆飞
// \date 2016-7-19
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleReclass extends ModuleBase
{
	public ModuleReclass(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.DatasetRaster));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRaster));
		
		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_alReclassItem = new ArrayList<String>(); 
		
		m_strMissingValue = "Data";
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("Reclass%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "重分类";
	}
	public String GetDescription()
	{
		return "栅格重分类。";
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
				
				portTo.SetName(alParam.get(1).m_objValue + "@" + alParam.get(0).m_objValue);
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
			Model2 model = ((Model2)m_model);
			
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
        	String strDatasources = model.GetWritableDatasources(true);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param("ReclassItemCount", String.format("%d", m_alReclassItem.size()), "重分类项个数", "", "参数", Param.EditType.ItemCount, "1")); //"1":ReclassItem参数个数
			for (int i = 0; i < m_alReclassItem.size(); i++)
			{
				alParam.add(new Param("ReclassItem", m_alReclassItem.get(i), "重分类项" + String.format("%d", i), 
						i == 0 ? "\"OldStartValue OldEndValue NewValue\"为分段映射:[OldStartValue,OldEndValue)->NewValue或 \"OldValue NewValue\"为单值映射:OldValue->NewValue" : "", 
								"参数", Param.EditType.FloatArray));
			}
			
			alParam.add(new Param("MissingValue", m_strMissingValue, "缺失值", "", "参数", Param.EditType.Select, "Data|NoData"));
			
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
		if (alParam.get(nIndex).m_strName.equals("ReclassItemCount"))
		{
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			
			int i = 0, j = nIndex + 1, nOldItemCount = Integer.parseInt((String)alParam.get(nIndex).m_objValue), nNewItemCount = Integer.parseInt((String)objValue);
			for (i = 0; i < nOldItemCount; i++)
				alParam.remove(j);
			if (nNewItemCount > 0)
			{
				for (i = 0; i < nNewItemCount; i++)
				{
					alParam.add(j++, new Param("ReclassItem", "", "重分类项" + String.format("%d", i), 
							i == 0 ? "\"OldStartValue OldEndValue NewValue\"为分段映射:[OldStartValue,OldEndValue)->NewValue或 \"OldValue NewValue\"为单值映射:OldValue->NewValue" : "", 
									"参数", Param.EditType.FloatArray));
				}
				
				Datasource ds = m_strDSInput.isEmpty() ? null : ws.GetDatasource(m_strDSInput);
	        	if (ds != null)
	        	{
	        		DatasetRaster dr = (DatasetRaster)ds.GetDataset(m_strDatasetInput);
	        		if (dr != null)
	        		{
	        			double dMin = dr.GetMinValue(), dMax = dr.GetMaxValue(), dStep = (dMax - dMin) / nNewItemCount, d = dMin;
	        			for (i = 0; i < nNewItemCount; i++, d += dStep)
	        				alParam.get(nIndex + 1 + i).m_objValue = String.format("%f %f %d", d, d + dStep, i);
	        		}
	        	}
			}
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
			
			final int nReclassItemCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			if (alParam.size() - i - 3 != nReclassItemCount)
				throw new AssertionError();
			m_alReclassItem.clear();
			while (i < alParam.size() - 3)
				m_alReclassItem.add((String)alParam.get(i++).m_objValue);				
			
			m_strMissingValue = (String)alParam.get(i++).m_objValue;
			
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetInput + "@" + m_strDSInput);
			m_alOutputPort.get(0).SetName(m_strDatasetOutput + "@" + m_strDSOutput);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		catch (AssertionError ae)
		{
			ae.printStackTrace();
		}
		return i;	
	}
	
	public boolean Execute()
	{
		Model2 model = ((Model2)m_model);
		if (!model.CheckInput(this, 0, m_strDSInput, m_strDatasetInput, false))
			return false;
		if (m_alReclassItem.size() == 0)
		{
			model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"重分类项个数\"参数为0。", GetNestedName(), GetID()));
			return false;
		}
		String[] strs = m_alReclassItem.get(0).split(" ");
		if (strs.length != 2 && strs.length != 3)
		{
			model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"第0个重分类项格式错误。", GetNestedName(), GetID()));
			return false;
		}
		for (int i = 1; i < m_alReclassItem.size(); i++)
		{
			if (m_alReclassItem.get(i).split(" ").length != strs.length)
			{
				model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"第%d个重分类项格式错误。", GetNestedName(), GetID(), i));
				return false;
			}
		}
		
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("Reclass", ws);
		
		String str;
		str = "{\"Datasource\":\"" + m_strDSInput + "\",\"Dataset\":\"" + m_strDatasetInput + "\"}";
        analyst.SetPropertyValue("Input", str);
        
        for (int i = 0; i < m_alReclassItem.size(); i++)
        	analyst.AddPropertyValue("ReclassItem", m_alReclassItem.get(i));	
        
        analyst.SetPropertyValue("MissingValue", m_strMissingValue);

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
	
	ArrayList<String> m_alReclassItem;
	
	String m_strMissingValue;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
