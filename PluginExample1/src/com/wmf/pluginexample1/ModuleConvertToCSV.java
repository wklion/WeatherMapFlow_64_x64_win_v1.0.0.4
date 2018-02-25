//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleConvertToCSV.java
// \brief 转换到CSV模块
// \author 王庆飞
// \date 2017-7-24
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.wmf.model.*;

public class ModuleConvertToCSV extends Module 
{
	public ModuleConvertToCSV(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new DataTablePort(this));
		m_alOutputPort.add(new Port(this, Port.Type.CSV));
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "转换到CSV";
	}
	public String GetDescription()
	{
		return "输入格式支持JDBC.ResultSet或CSV格式，合并输出为CSV格式，支持行或列合并。";
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
			if (alParam.size() != 1)
				return false;
			if (alParam.get(0).m_objValue != null && !(alParam.get(0).m_objValue instanceof ResultSet) && !(alParam.get(0).m_objValue instanceof List<?>))
				return false;
			portTo.SetType(portFrom.GetType());
			DataTablePort dtp = (DataTablePort)portTo;
			if (alParam.get(0).m_objValue instanceof ResultSet)
				dtp.m_rs = (ResultSet)alParam.get(0).m_objValue;
			else if (alParam.get(0).m_objValue instanceof List<?>)
				dtp.m_list = (List<String>)alParam.get(0).m_objValue;
			
			while (i >= m_alInputPort.size() - 1)
				InsertPort(-1, new DataTablePort(this), true);
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
			if (i >= 0 && i < m_alInputPort.size() - 1)
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
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
			for (int i = 0; i < m_alInputPort.size() - 1; i++)
			{
				DataTablePort dtp = (DataTablePort)m_alInputPort.get(i);
				alParam.add(new Param("Name", dtp.GetName(), "名称", "", "输入端"));
			}
			
			ArrayList<String> alFieldName = new ArrayList<String>();
			((DataTablePort)m_alInputPort.get(0)).GetFieldName(alFieldName);
			String strFields = "";
			for (int i = 0; i < alFieldName.size(); i++)
			{
				if (i > 0)
					strFields += "|";
				strFields += alFieldName.get(i);
			}
			
			alParam.add(new Param("StationNumField", m_strStationNumField, "区站号字段", "", "参数", Param.EditType.SelectOrInput, strFields));
			alParam.add(new Param("TimeField", m_strTimeField, "时间字段", "", "参数", Param.EditType.SelectOrInput, strFields));
        	
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
			while (alParam.get(i).m_strName.equals("Name"))
			{
				DataTablePort dtp = (DataTablePort)m_alInputPort.get(j);
				dtp.SetName((String)alParam.get(i++).m_objValue);
				
				++j;
				if (j == m_alInputPort.size())
					m_alInputPort.add(new DataTablePort(this));
			}
			
			m_strStationNumField = (String)alParam.get(i++).m_objValue;
			m_strTimeField = (String)alParam.get(i++).m_objValue;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public boolean SetParam(String strName, Object objValue)
	{
		try
		{
			if (strName.equalsIgnoreCase("LoopedCount"))
			{
				if (m_alInputPort.size() != 2)
					return false;
				DataTablePort dtp = (DataTablePort)m_alInputPort.get(0);
				if (dtp.m_list == null)
					return false;
				m_nLoopedCount = (int)Double.parseDouble((String)objValue);
				if (m_nLoopedCount == 1)
				{
					m_alOutput.clear();
					m_alOutput.addAll(dtp.m_list);
				}
				else
				{
					int i = 0;
					Iterator<String> it = dtp.m_list.iterator();
					while (it.hasNext())
					{
						String str = it.next();
						if (i >= 2)
							m_alOutput.add(str);
						++i;
					}
				}
				return true;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
	
	public boolean Execute()
	{
		try
		{
			if (m_alInputPort.size() <= 1)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块没有输入数据。", GetNestedName(), GetID()));
				return false;
			}
			else if (m_alInputPort.size() == 2)
			{
				DataTablePort dtp = (DataTablePort)m_alInputPort.get(0);
				if (dtp.m_list != null)
				{
					if (m_nLoopedCount != 0)
					{
						OnParamChanged();
						return true;
					}
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块单个CSV不需要转换到CSV。", GetNestedName(), GetID()));
					return false;
				}
			}
			//------------------------------------------------------------------------------------------------------------------------------------
			int i = 0, j = 0, k = 0;
			String str;
			ArrayList<ArrayList<String>> alalFieldName = new ArrayList<ArrayList<String>>(); 
			for (i = 0; i < m_alInputPort.size() - 1; i++)
			{
				DataTablePort dtp = (DataTablePort)m_alInputPort.get(i);
				if (dtp.m_rs == null && dtp.m_list == null)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"没有输入数据。", GetNestedName(), GetID(), dtp.GetName()));
					return false;
				}
				ArrayList<String> al = new ArrayList<String>();
				dtp.GetFieldName(al);
				alalFieldName.add(al);
			}
			
			ArrayList<String> alFieldName = new ArrayList<String>(alalFieldName.get(0));
			
			ArrayList<String> alFieldType = new ArrayList<String>();
			((DataTablePort)m_alInputPort.get(0)).GetFieldType(alFieldType);
			
			for (i = 1; i < alalFieldName.size(); i++)
			{
				ArrayList<String> alft = new ArrayList<String>();
				((DataTablePort)m_alInputPort.get(i)).GetFieldType(alft);
				
				ArrayList<String> alfn = alalFieldName.get(i);
				for (j = 0; j < alfn.size(); j++)
				{
					str = alfn.get(j);
					for (k = 0; k < alFieldName.size(); k++)
					{
						if (alFieldName.get(k).equalsIgnoreCase(str))
							break;
					}
					if (k == alFieldName.size())
					{
						alFieldName.add(alfn.get(j));
						alFieldType.add(alft.get(j));
					}
				}
			}
			//------------------------------------------------------------------------------------------------------------------------------------
			m_alOutput.clear();
			str = "";
			for (i = 0; i < alFieldName.size(); i++)
			{
				if (i > 0)
					str += ",";
				str += alFieldName.get(i);
			}
			m_alOutput.add(str);
			
			str = "";
			for (i = 0; i < alFieldType.size(); i++)
			{
				if (i > 0)
					str += ",";
				str += alFieldType.get(i);
			}
			m_alOutput.add(str);
			
			if (alFieldName.size() == alalFieldName.get(0).size()) //只合并行
			{
				for (i = 0; i < m_alInputPort.size() - 1; i++)
				{
					DataTablePort dtp = (DataTablePort)m_alInputPort.get(i);
					if (dtp.m_rs == null && dtp.m_list == null)
						throw new AssertionError();
					int[] nVarIndex = new int[alFieldName.size()];
					if (i == 0)
					{
						for (j = 0; j < nVarIndex.length; j++)
							nVarIndex[j] = j;
					}
					else
					{
						ArrayList<String> alfn = alalFieldName.get(i);
						for (j = 0; j < alFieldName.size(); j++)
						{
							str = alFieldName.get(j);
							for (k = 0; k < alfn.size(); k++)
							{
								if (alfn.get(k).equalsIgnoreCase(str))
								{
									nVarIndex[j] = k;
									break;
								}
							}
						}	
					}
					
					if (dtp.m_rs != null)
					{
						dtp.m_rs.beforeFirst();
						while (dtp.m_rs.next())
						{
							str = "";
							for (k = 0; k < nVarIndex.length; k++)
							{
								if (k > 0)
									str += ",";
								str += dtp.m_rs.getString(nVarIndex[k] + 1);
							}
							m_alOutput.add(str);
						}
					}
					else if (dtp.m_list != null)
					{
						for (j = 2; j < dtp.m_list.size(); j++)
			        	{
							String[] strs = dtp.m_list.get(j).split(",");
			        		str = "";
							for (k = 0; k < nVarIndex.length; k++)
							{
								if (k > 0)
									str += ",";
								str += strs[nVarIndex[k]];
							}
							m_alOutput.add(str);
			        	}
					}
				}
			}
			else
			{
				if (m_strStationNumField.isEmpty() && m_strTimeField.isEmpty())
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块未指定匹配字段。", GetNestedName(), GetID()));
					return false;
				}
				HashMap<String, Integer> hmIndex = new HashMap<String, Integer>(); 
				for (i = 0; i < m_alInputPort.size() - 1; i++)
				{
					DataTablePort dtp = (DataTablePort)m_alInputPort.get(i);
					if (dtp.m_rs == null && dtp.m_list == null)
						throw new AssertionError();
					int nStationNum = -1, nTime = -1;
					ArrayList<String> alfn = alalFieldName.get(i);
					if (!m_strStationNumField.isEmpty())
					{
						for (j = 0; j < alfn.size(); j++)
						{
							str = alfn.get(j);
							if (str.equalsIgnoreCase(m_strStationNumField))
								nStationNum = j;
						}
						if (nStationNum == -1)
						{
							m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含区站号字段\"%s\"。", GetNestedName(), GetID(), dtp.GetName(), m_strStationNumField));
							return false;
						}
					}
					if (!m_strTimeField.isEmpty())
					{
						for (j = 0; j < alfn.size(); j++)
						{
							str = alfn.get(j);
							if (str.equalsIgnoreCase(m_strTimeField))
								nTime = j;
						}
						if (nTime == -1)
						{
							m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含时间字段\"%s\"。", GetNestedName(), GetID(), dtp.GetName(), m_strTimeField));
							return false;
						}
					}
					
					int[] nVarIndex = new int[alFieldName.size()];
					for (j = 0; j < nVarIndex.length; j++)
						nVarIndex[j] = -1;
					for (j = 0; j < alFieldName.size(); j++)
					{
						str = alFieldName.get(j);
						for (k = 0; k < alfn.size(); k++)
						{
							if (alfn.get(k).equalsIgnoreCase(str))
							{
								nVarIndex[j] = k;
								break;
							}
						}
					}

					if (dtp.m_rs != null)
					{
						dtp.m_rs.beforeFirst();
						while (dtp.m_rs.next())
						{
							str = "";
							if (nStationNum != -1)
								str += dtp.m_rs.getString(nStationNum + 1);
							if (nTime != -1)
								str += dtp.m_rs.getString(nTime + 1);
							
							if (!hmIndex.containsKey(str))
							{
								hmIndex.put(str, m_alOutput.size());
								
								str = "";
								for (k = 0; k < nVarIndex.length; k++)
								{
									if (k > 0)
										str += ",";
									if (nVarIndex[k] == -1)
										str += "-9999";
									else
										str += dtp.m_rs.getString(nVarIndex[k] + 1);
								}
								m_alOutput.add(str);
							}
							else
							{
								int nIndex = hmIndex.get(str);
								String[] strsOutput = m_alOutput.get(nIndex).split(",");
								for (k = 0; k < nVarIndex.length; k++)
								{
									if (nVarIndex[k] != -1)
										strsOutput[k] = dtp.m_rs.getString(nVarIndex[k] + 1);
								}
								str = "";
								for (k = 0; k < strsOutput.length; k++)
								{
									if (k > 0)
										str += ",";
									str += strsOutput[k];
								}
								m_alOutput.set(nIndex, str);
							}
						}
					}
					else if (dtp.m_list != null)
					{
						for (j = 2; j < dtp.m_list.size(); j++)
			        	{
							String[] strs = dtp.m_list.get(j).split(",");
							
							str = "";
							if (nStationNum != -1)
								str += strs[nStationNum];
							if (nTime != -1)
								str += strs[nTime];
							
							if (!hmIndex.containsKey(str))
							{
								hmIndex.put(str, m_alOutput.size());
								
								str = "";
								for (k = 0; k < nVarIndex.length; k++)
								{
									if (k > 0)
										str += ",";
									if (nVarIndex[k] == -1)
										str += "-9999";
									else
										str += strs[nVarIndex[k]];
								}
								m_alOutput.add(str);
							}
							else
							{
								int nIndex = hmIndex.get(str);
								String[] strsOutput = m_alOutput.get(nIndex).split(",");
								for (k = 0; k < nVarIndex.length; k++)
								{
									if (nVarIndex[k] != -1)
										strsOutput[k] = strs[nVarIndex[k]];
								}
								str = "";
								for (k = 0; k < strsOutput.length; k++)
								{
									if (k > 0)
										str += ",";
									str += strsOutput[k];
								}
								m_alOutput.set(nIndex, str);
							}
			        	}
					}
				}
			}
			
			OnParamChanged();
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		if (i == 0)
			alParam.add(new Param("CSV", m_alOutput));
		
		return alParam.size();
	}
	//---------------------------------------------------------------------------	
	String m_strStationNumField = "StationNum";
	String m_strTimeField = "Time";

	ArrayList<String> m_alOutput = new ArrayList<String>();
	//---------------------------------------------------------------------------
	int m_nLoopedCount = 0;
}
