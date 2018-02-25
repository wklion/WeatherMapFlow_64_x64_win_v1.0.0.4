//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleLoop.java
// \brief 循环模块类
// \author 王庆飞
// \date 2016-7-25
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.wmf.model.*;

public class ModuleLoop extends ModuleNestedModel
{
	public ModuleLoop(int nID) 
	{
		super(nID);
		
		m_strOptionalParamNames = "";
		m_alLoopItem = new ArrayList<LoopItem>(); 
		m_alLoopItem.add(new LoopItem());
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return m_strName + "\n循环";
	}
	public String GetDescription()
	{
		return "循环执行指定模型，支持多层循环，每层循环支持多个参数。";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			
			alParam.add(new Param("LoopItemCount", String.format("%d", m_alLoopItem.size()), "循环项数", "", "", Param.EditType.ItemCount, "4")); //"4":LoopItem参数个数
			for (int i = 0; i < m_alLoopItem.size(); i++)
			{
				LoopItem li = m_alLoopItem.get(i);
				String strGroups = String.format("第%d循环项", i + 1);
				alParam.add(new Param("Layer", String.format("%d", li.m_nLayer), "层号", i == 0 ? "从1开始，由外层向内层" : "", strGroups, Param.EditType.UInt));
				alParam.add(new Param("ParamName", li.m_strParamName, "参数名", "", strGroups, Param.EditType.SelectOrInput, m_strOptionalParamNames));  
				alParam.add(new Param("EqualStep", li.m_bEqualStep ? "true" : "false", "是否等步长", "", strGroups, Param.EditType.Select, "true|false"));
				
				String str = "";
				if (li.m_values != null)
				{
					for (int j = 0; j < li.m_values.length; j++)
					{
						if (!str.isEmpty())
							str += " ";
						str += li.m_values[j];
					}
				}
				alParam.add(new Param("Values", str, "循环值", i == 0 ? "等步长:开始值 结束值 步长;不等步长:空格分隔的字符串数组" : "", strGroups,
						li.m_bEqualStep ? Param.EditType.FloatArray : Param.EditType.Default));
			}
			
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
		if (alParam.get(nIndex).m_strName.equals("LoopItemCount"))
		{
			int i = 0, j = 0, nOldItemCount = Integer.parseInt((String)alParam.get(nIndex).m_objValue), nNewItemCount = Integer.parseInt((String)objValue);
			if (nOldItemCount < nNewItemCount)
			{
				j = nIndex + 1 + nOldItemCount * 4;
				Object objLayer = nOldItemCount > 0 ? alParam.get(nIndex + 1 + (nOldItemCount - 1) * 4).m_objValue : "1";
				for (i = nOldItemCount; i < nNewItemCount; i++)
				{
					String strGroups = String.format("第%d循环项", i + 1);
					alParam.add(j++, new Param("Layer", objLayer, "层号", i == 0 ? "从1开始，由外层向内层" : "", strGroups, Param.EditType.UInt));
					alParam.add(j++, new Param("ParamName", "", "参数名", "", strGroups, Param.EditType.SelectOrInput, m_strOptionalParamNames));  
					alParam.add(j++, new Param("EqualStep", "true", "是否等步长", "", strGroups, Param.EditType.Select, "true|false"));
					alParam.add(j++, new Param("Values", "0 0 0", "循环值", i == 0 ? "等步长:开始值 结束值 步长;不等步长:列举值,空格分隔" : "", strGroups));
				}
			}
			else
			{
				j = nIndex + 1 + nNewItemCount * 4;
				for (i = nNewItemCount * 4; i < nOldItemCount * 4; i++)
					alParam.remove(j);
			}
			return true;
		}
		else if (alParam.get(nIndex).m_strName.equals("EqualStep"))
		{
			if (objValue.equals("true"))
				alParam.get(nIndex + 1).m_eEditType = Param.EditType.FloatArray;
			else
				alParam.get(nIndex + 1).m_eEditType = Param.EditType.Default;
			return true;
		}
		return super.OnParamChanged(alParam, nIndex, objValue);
	}
	void GetParamName(ArrayList<Param> al, ArrayList<String> alParamName)
	{
		int i = 0, j = 0;
		for (i = 0; i < al.size(); i++)
		{
			Param p = al.get(i);
			if (p.m_eEditType == Param.EditType.Int || p.m_eEditType == Param.EditType.UInt || p.m_eEditType == Param.EditType.Float)
			{
				for (j = 0; j < alParamName.size(); j++)
				{
					if (alParamName.get(j).equals(p.m_strName))
						break;
				}
				if (j == alParamName.size())
					alParamName.add(p.m_strName);
			}
		}
	}
	//用于 XML->参数 等
	public int SetParam(final ArrayList<Param> alParam)
	{
		int i = super.SetParam(alParam);
		try
		{
			m_strOptionalParamNames = "";
			if (m_modelNested != null)
			{
				ArrayList<String> alParamName = new ArrayList<String>();
				
				ArrayList<Param> al = new ArrayList<Param>();
				m_modelNested.GetParam(al);
				GetParamName(al, alParamName);
				
				LinkedList<Module> ll = m_modelNested.GetModules();
				Iterator<Module> it = ll.iterator();
				while (it.hasNext())
				{
					Module module = it.next();
					al.clear();
					module.GetParam(al);
					GetParamName(al, alParamName);
				}
				
				for (int j = 0; j < alParamName.size(); j++)
				{
					if (!m_strOptionalParamNames.isEmpty())
						m_strOptionalParamNames += "|";
					m_strOptionalParamNames += alParamName.get(j);
				}
			}
			
			final int nLoopItemCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			if (alParam.size() - i != nLoopItemCount * 4)
				throw new AssertionError();
			m_alLoopItem.clear();
			while (i < alParam.size())
			{
				LoopItem li = new LoopItem();
				li.m_nLayer = Integer.parseInt((String)alParam.get(i++).m_objValue);
				li.m_strParamName = (String)alParam.get(i++).m_objValue;
				li.m_bEqualStep = Boolean.parseBoolean((String)alParam.get(i++).m_objValue);				
				li.m_values = ((String)alParam.get(i++).m_objValue).split(" ");
				m_alLoopItem.add(li);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		catch (AssertionError ae)
		{
			ae.printStackTrace();
		}
		return i;	
	}
	
	public boolean Execute()
	{
		if (m_modelNested == null)
		{
			m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"模型\"参数为null。", GetNestedName(), GetID()));
			return false;
		}
		if (m_alLoopItem.size() == 0)
		{
			m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"循环项数\"参数为0。", GetNestedName(), GetID()));
			return false;
		}
		int i = 0, j = 0, k = 0;
		ArrayList<Integer> alLayerStart = new ArrayList<Integer>(); //每层循环起始索引
		for (i = 0; i < m_alLoopItem.size(); i++)
		{
			LoopItem li = m_alLoopItem.get(i);
			String strWho = String.format("\"%s-%d\"模块第%d循环项：", GetNestedName(), GetID(), i + 1);
			
			if (i == 0)
			{
				if (li.m_nLayer == 1)
				{
					alLayerStart.add(i);
				}
				else
				{
					m_model.OutputLog(Model.LogLevel.Error, strWho + "\"层号\"参数必须从1开始。");
					return false;	
				}
			}
			else
			{
				if (li.m_nLayer == m_alLoopItem.get(i - 1).m_nLayer + 1)
				{
					alLayerStart.add(i);
				}
				else if (li.m_nLayer != m_alLoopItem.get(i - 1).m_nLayer)	
				{							
					m_model.OutputLog(Model.LogLevel.Error, strWho + "\"层号\"参数必须连续。");
					return false;
				}
			}
			
			if (li.m_strParamName.isEmpty())
			{
				m_model.OutputLog(Model.LogLevel.Error, strWho + "\"参数名\"参数为空。");
				return false;
			}
			if (li.m_values == null || li.m_values.length == 0)
			{
				m_model.OutputLog(Model.LogLevel.Error, strWho + "\"循环值\"参数为空。");
				return false;
			}
			if (li.m_bEqualStep)
			{
				if (li.m_values.length != 3)
				{
					m_model.OutputLog(Model.LogLevel.Error, strWho + "等步长必须包括：开始值 结束值 步长。");
					return false;
				}
				for (j = 0; j < li.m_values.length; j++)
				{
					if (!li.m_values[j].matches("^(-?\\d{1,15})(\\.\\d{1,15})?([Ee]-?\\d{1,3})?$"))
					{
						m_model.OutputLog(Model.LogLevel.Error, strWho + "开始值 结束值 步长必须为数值型。");
						return false;
					}
				}
				double dStep = Double.parseDouble(li.m_values[2]);
				if (Math.abs(dStep) < 1e-10)
				{
					m_model.OutputLog(Model.LogLevel.Error, strWho + "步长为0。");
					return false;
				}
			}
		}
		
		int nLoopCount = 1;
		for (i = 0; i < alLayerStart.size(); i++)
		{
			int nEnd = (i == (alLayerStart.size() - 1) ? m_alLoopItem.size() : alLayerStart.get(i + 1)), nCount = 0;
			for (j = alLayerStart.get(i); j < nEnd; j++)
			{
				LoopItem li = m_alLoopItem.get(j);
				if (li.m_bEqualStep)
					k = (int)((Double.parseDouble(li.m_values[1]) - Double.parseDouble(li.m_values[0])) / Double.parseDouble(li.m_values[2]) + 1);	
				else
					k = li.m_values.length;
				if (nCount == 0)
					nCount = k;
				else
				{
					if (k != nCount)
					{
						String strWho = String.format("\"%s-%d\"模块第%d循环项：", GetNestedName(), GetID(), j + 1);
						m_model.OutputLog(Model.LogLevel.Error, strWho + "同层循环数必须相等。");
						return false;
					}
				}
			}
			nLoopCount *= nCount;	
		}
		
		boolean bResult = true;
		
		m_modelNested.m_dateStart = m_model.m_dateStart;
		m_modelNested.m_dateDataStart = m_model.m_dateDataStart;
		m_modelNested.m_dateDataEnd = m_model.m_dateDataEnd;
		
		Set<ModelListener> setListener = m_model.GetListeners();
		Iterator<ModelListener> it = setListener.iterator();  
		while (it.hasNext())
			m_modelNested.AddListener(it.next());
		
		try
		{
			double[] arrParamValue = new double[m_alLoopItem.size()];
			for (i = 0; i < m_alLoopItem.size(); i++)
			{
				LoopItem li = m_alLoopItem.get(i);
				arrParamValue[i] = li.m_bEqualStep ? Double.parseDouble(li.m_values[0]) : 0;
			}
			
			ArrayList<Module> alOutputModule = new ArrayList<Module>();
			for (i = 0; i < m_alOutputPort.size(); i++)
			{
				LinkedList<Pipe> llPipe = m_alOutputPort.get(i).GetAttachedPipeList();
				Iterator<Pipe> itPipe = llPipe.iterator();
				while (itPipe.hasNext())
				{
					Pipe pipe = itPipe.next();
					Module m = pipe.GetOutgoingPort().GetModule();
					for (j = 0; j < alOutputModule.size(); j++)
					{
						if (m == alOutputModule.get(j))
							break;
					}
					if (j == alOutputModule.size())
						alOutputModule.add(m);
				}
			}
			
			int nTop = 0;
			LinkedList<Module> llModule = m_modelNested.GetModules();
			i = 0;
			while (nTop >= 0)
			{
				LoopItem liTop = m_alLoopItem.get(alLayerStart.get(nTop));
				double dTop = arrParamValue[alLayerStart.get(nTop)];
				int nEnd = (nTop == (alLayerStart.size() - 1) ? m_alLoopItem.size() : alLayerStart.get(nTop + 1));
				if ((liTop.m_bEqualStep && dTop <= Double.parseDouble(liTop.m_values[1])) || 
						(!liTop.m_bEqualStep && dTop < liTop.m_values.length)) //循环条件
				{
					for (j = alLayerStart.get(nTop); j < nEnd; j++)
					{
						liTop = m_alLoopItem.get(j);
						String strValue = liTop.m_bEqualStep ? String.format("%f", arrParamValue[j]) : liTop.m_values[(int)arrParamValue[j]];
						boolean bValid = false;
						if (liTop.m_strParamName.equalsIgnoreCase("StartTime"))
						{
							Calendar c = new GregorianCalendar();
							c.setTime(m_model.m_dateStart);
							c.add(Calendar.HOUR, (int)Double.parseDouble(strValue));
							m_modelNested.m_dateStart = c.getTime();
							m_modelNested.m_dateDataStart = m_modelNested.m_dateDataEnd = m_modelNested.m_dateStart; 
							bValid = true;
						}
						else
						{
							if (m_modelNested.SetParam(liTop.m_strParamName, strValue))
								bValid = true; 
							Iterator<Module> itModule = llModule.iterator();
							while (itModule.hasNext())
							{
								Module module = itModule.next();
								if (module.SetParam(liTop.m_strParamName, strValue))
									bValid = true; 
							}
						}
						if (!bValid)
						{
							m_model.OutputLog(Model.LogLevel.Warning, String.format("\"%s-%d\"模块SetParam(%s, %s)失败。", GetNestedName(), GetID(), 
									liTop.m_strParamName, strValue));
						}
					}
					
					if (nTop == alLayerStart.size() - 1)
					{
						m_modelNested.SetCommand(m_model.GetCommand());
						if (!m_modelNested.Execute())
						{
							String str = "";
							for (j = 0; j < m_alLoopItem.size(); j++)
							{
								LoopItem li = m_alLoopItem.get(j);
								if (!str.isEmpty())
									str += ",";
								str += li.m_strParamName + ":";
								str += li.m_bEqualStep ? String.format("%f", arrParamValue[j]) : li.m_values[(int)arrParamValue[j]]; 
							}
							m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块循环执行模型失败(%s)。", GetNestedName(), GetID(), str));
						}
						SetProgress((float)(++i) / (float)nLoopCount);
						
						String str = String.format("%d", i);
						for (j = 0; j < alOutputModule.size(); j++)
							alOutputModule.get(j).SetParam("LoopedCount", str);
					}
					
					if (nTop < alLayerStart.size() - 1)
					{
						++nTop;
						nEnd = (nTop == (alLayerStart.size() - 1) ? m_alLoopItem.size() : alLayerStart.get(nTop + 1));
						for (j = alLayerStart.get(nTop); j < nEnd; j++)
						{
							liTop = m_alLoopItem.get(j);
							arrParamValue[j] = liTop.m_bEqualStep ? Double.parseDouble(liTop.m_values[0]) : 0;
						}
					}
					else
					{
						for (j = alLayerStart.get(nTop); j < nEnd; j++)
						{
							liTop = m_alLoopItem.get(j);
							arrParamValue[j] += (liTop.m_bEqualStep ? Double.parseDouble(liTop.m_values[2]) : 1.0);
						}
					}
				}
				else
				{
					--nTop;
					if (nTop >= 0)
					{
						nEnd = (nTop == (alLayerStart.size() - 1) ? m_alLoopItem.size() : alLayerStart.get(nTop + 1)); 
						for (j = alLayerStart.get(nTop); j < nEnd; j++)
						{
							liTop = m_alLoopItem.get(j);
							arrParamValue[j] += (liTop.m_bEqualStep ? Double.parseDouble(liTop.m_values[2]) : 1.0);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			bResult = false;
		}
		
		it = setListener.iterator();
		while (it.hasNext())
			m_modelNested.RemoveListener(it.next());
		
		return bResult;
	}
	//--------------------------------------------------------------------------------
	String m_strOptionalParamNames;
	//--------------------------------------------------------------------------------
	class LoopItem
	{
		LoopItem()
		{
			m_nLayer = 1;
			m_strParamName = "";
			m_bEqualStep = true;
			m_values = new String[]{"0", "0", "0"};
		}
		
		int m_nLayer;
		String m_strParamName;
		boolean m_bEqualStep;
		String[] m_values;
	}
	ArrayList<LoopItem> m_alLoopItem;
}
