//-------------------------------------------------------------
// \project PluginExample3
// \file ModuleProcessStatistics.java
// \brief 过程统计模块
// \author 王庆飞
// \date 2017-4-7
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample3;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.wmf.model.*;

import java.text.SimpleDateFormat;

public class ModuleProcessStatistics extends Module 
{
	/**
	 * 如果输入端或输出端可动态增删端口，需要派生端口类，否则Undo可能出错。
	 */
	class MyPort extends Port
	{
		public MyPort(Module module)
		{
			super(module);
	    }
		public MyPort(Module module, Type eType)
		{
			super(module, eType);
	    }
		public Port Clone()
		{
			MyPort mp = new MyPort(null, m_eType);
			mp.m_strName = m_strName;
			mp.m_rs = m_rs;
			mp.m_list = m_list;
			return mp;
		}
		
		ResultSet m_rs = null;
		List<String> m_list = null;
	}
	
	public ModuleProcessStatistics(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new MyPort(this));
		m_alOutputPort.add(new Port(this, Port.Type.CSV));
    }

	public String GetGroupName()
	{
		return "JDBC";
	}
	public String GetName()
	{
		return "过程统计";
	}
	public String GetDescription()
	{
		return "输入格式支持JDBC.ResultSet或CSV格式，输出为CSV格式。";
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
			MyPort mp = (MyPort)portTo;
			if (alParam.get(0).m_objValue instanceof ResultSet)
				mp.m_rs = (ResultSet)alParam.get(0).m_objValue;
			else if (alParam.get(0).m_objValue instanceof List<?>)
				mp.m_list = (List<String>)alParam.get(0).m_objValue;
			
			while (i >= m_alInputPort.size() - 1)
				InsertPort(-1, new MyPort(this), true);
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
	
	String GetDescription(StatsMethod eStatsMethod)
	{
		switch (eStatsMethod)
		{
		case Sum:
			return "累加";
		case Avg:
			return "平均";
		case Max:
			return "极大值";
		case Min:
			return "极小值";
		case Drop:
			return "降幅：邻近过程前极大值减过程中极小值。";
		case PersistenceCount:
			return "持续日数";
		}
		return "";
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
				MyPort mp = (MyPort)m_alInputPort.get(i);
				alParam.add(new Param("Name", mp.GetName(), "名称", "", "输入端"));
			}
			
			alParam.add(new Param("StationNumField", m_strStationNumField, "区站号字段", "", "参数"));
			alParam.add(new Param("TimeField", m_strTimeField, "时间字段", "", "参数"));
			alParam.add(new Param("TimeFieldFormat", m_strTimeFieldFormat, "时间字段格式", "", "参数"));
			alParam.add(new Param("StatsMethod", m_eStatsMethod.toString(), "统计方法", GetDescription(m_eStatsMethod), "参数", Param.EditType.Select, 
					"Sum|Avg|Max|Min|Drop|PersistenceCount"));
			alParam.add(new Param("StatsField", m_strStatsField, "统计字段", "", "参数", 
					(m_eStatsMethod != StatsMethod.PersistenceCount ? Param.EditType.Default : Param.EditType.ReadOnly)));
        	
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
		if (alParam.get(nIndex).m_strName.equals("StatsMethod"))
		{
			StatsMethod e = StatsMethod.valueOf((String)objValue);
			alParam.get(nIndex).m_strDescription = GetDescription(e);
			
			alParam.get(nIndex + 1).m_eEditType = (e != StatsMethod.PersistenceCount ? Param.EditType.Default : Param.EditType.ReadOnly);
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
			int j = 0;
			while (alParam.get(i).m_strName.equals("Name"))
			{
				MyPort mp = (MyPort)m_alInputPort.get(j);
				mp.SetName((String)alParam.get(i++).m_objValue);
				
				++j;
				if (j == m_alInputPort.size())
					m_alInputPort.add(new MyPort(this));
			}
			
			m_strStationNumField = (String)alParam.get(i++).m_objValue;
			m_strTimeField = (String)alParam.get(i++).m_objValue;
			m_strTimeFieldFormat = (String)alParam.get(i++).m_objValue;
			m_eStatsMethod = StatsMethod.valueOf((String)alParam.get(i++).m_objValue);
			m_strStatsField = (String)alParam.get(i++).m_objValue;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public boolean Execute()
	{
		try
		{
			int i = 0, j = 0, k = 0;
			String str;
			//------------------------------------------------------------------------------------------------------------------------------------
			//获取观测数据
			SimpleDateFormat sdf = new SimpleDateFormat(m_strTimeFieldFormat);
			m_alObservationData.clear();
			
			ArrayList<String> alVarName = new ArrayList<String>();
			for (i = 0; i < m_alInputPort.size(); i++)
			{
				MyPort mp = (MyPort)m_alInputPort.get(i);
				if (mp.m_rs == null && mp.m_list == null)
					continue;
				ResultSetMetaData rsmd = mp.m_rs != null ? mp.m_rs.getMetaData() : null;
				String[] strs = mp.m_list != null ? mp.m_list.get(0).split(",") : null;
				if (alVarName.size() == 0)
				{
					for (j = 0; j < (rsmd != null ? rsmd.getColumnCount() : strs.length); j++)
					{
						str = (rsmd != null ? rsmd.getColumnLabel(j + 1) : strs[j]);
						if (str.equalsIgnoreCase(m_strStationNumField) || str.equalsIgnoreCase(m_strTimeField) || str.equalsIgnoreCase("ProcessFlag"))
							continue;
						alVarName.add(str);
					}
					if (alVarName.size() == 0)
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块输入端变量个数为0。", GetNestedName(), GetID()));
						return false;
					}
				}
				int nStationNum = -1, nTime = -1, nProcessFlag = -1;
				int[] nVarIndex = new int[alVarName.size()];
				for (j = 0; j < nVarIndex.length; j++)
					nVarIndex[j] = -1;
				for (j = 0; j < (rsmd != null ? rsmd.getColumnCount() : strs.length); j++)
				{
					str = (rsmd != null ? rsmd.getColumnLabel(j + 1) : strs[j]);
					if (str.equalsIgnoreCase(m_strStationNumField))
						nStationNum = j;
					else if (str.equalsIgnoreCase(m_strTimeField))
						nTime = j;
					else if (str.equalsIgnoreCase("ProcessFlag"))
						nProcessFlag = j;
					else
					{
						for (k = 0; k < alVarName.size(); k++)
						{
							if (alVarName.get(k).equalsIgnoreCase(str))
							{
								nVarIndex[k] = j;
								break;
							}
						}
					}
				}	
				if (nStationNum == -1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含区站号字段\"%s\"。", GetNestedName(), GetID(), mp.GetName(), m_strStationNumField));
					return false;
				}
				if (nTime == -1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含时间字段\"%s\"。", GetNestedName(), GetID(), mp.GetName(), m_strTimeField));
					return false;
				}
				if (nProcessFlag == -1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含字段\"ProcessFlag\"。", GetNestedName(), GetID(), mp.GetName()));
					return false;
				}
				for (j = 0; j < nVarIndex.length; j++)
				{
					if (nVarIndex[j] == -1)
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含被统计变量字段\"%s\"。", GetNestedName(), GetID(), mp.GetName(), alVarName.get(j)));
						return false;
					}
				}
				if (mp.m_rs != null)
				{
					mp.m_rs.beforeFirst();
					while (mp.m_rs.next())
					{
						String[] strVar = new String[nVarIndex.length];
						for (k = 0; k < nVarIndex.length; k++)
							strVar[k] = mp.m_rs.getString(nVarIndex[k] + 1);
						m_alObservationData.add(new ObservationData(mp.m_rs.getString(nStationNum + 1), sdf.parse(mp.m_rs.getString(nTime + 1)), strVar, mp.m_rs.getByte(nProcessFlag + 1)));
					}
				}
				else if (mp.m_list != null)
				{
					for (j = 2; j < mp.m_list.size(); j++)
		        	{
		        		strs = mp.m_list.get(j).split(",");
		        		String[] strVar = new String[nVarIndex.length];
						for (k = 0; k < nVarIndex.length; k++)
							strVar[k] = strs[nVarIndex[k]];
		        		m_alObservationData.add(new ObservationData(strs[nStationNum], sdf.parse(strs[nTime]), strVar, Byte.parseByte(strs[nProcessFlag])));
		        	}
				}
			}
			
			Collections.sort(m_alObservationData, new Comparator<ObservationData>()
			{
				public int compare(ObservationData od1, ObservationData od2)
				{
					int n = od1.m_strNum.compareToIgnoreCase(od2.m_strNum);
					if (n == 0)
						n = od1.m_dateTime.compareTo(od2.m_dateTime);
					return n;
				}
			});
			//------------------------------------------------------------------------------------------------------------------------------------
			int nStatsField = -1;
			if (m_eStatsMethod != StatsMethod.PersistenceCount)
			{
				for (i = 0; i < alVarName.size(); i++)
				{
					if (alVarName.get(i).equalsIgnoreCase(m_strStatsField))
					{
						nStatsField = i;
						break;
					}
				}
				if (nStatsField == -1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块不包含统计字段\"%s\"。", GetNestedName(), GetID(), m_strStatsField));
					return false;
				}
			}
			
			if (m_eStatsMethod == StatsMethod.Drop)
			{
				m_llOutput.clear();
				str = m_strStationNumField + "," + m_strTimeField + ",Drop";
				m_llOutput.add(str);
				str = "VARCHAR,VARCHAR,DOUBLE";
				m_llOutput.add(str);
				
				ObservationData odLast = null;
				int nStart = -1;
				double dMin = 0.0;
				for (i = 0; i <= m_alObservationData.size(); i++)
				{
					ObservationData od = (i < m_alObservationData.size() ? m_alObservationData.get(i) : null);
					
					if (odLast != null)
					{
						if (od == null || !odLast.m_strNum.equals(od.m_strNum) || od.m_nProcessFlag == 0)
						{
							j = nStart - 1;
							if (j >= 0 && m_alObservationData.get(j).m_nProcessFlag == 0)
							{
								double dMax = Double.parseDouble(m_alObservationData.get(j).m_strVar[nStatsField]);
								--j;
								while (j >= 0 && m_alObservationData.get(j).m_nProcessFlag == 0)
								{
									double d = Double.parseDouble(m_alObservationData.get(j).m_strVar[nStatsField]);;
									if (d > dMax)
										dMax = d;
									--j;
								}
								
								str = odLast.m_strNum + "," + sdf.format(m_alObservationData.get(nStart).m_dateTime);
								str += "," + String.format("%f", dMax - dMin);
								m_llOutput.add(str);
								//System.out.println(m_llOutput.getLast());
								
								odLast = null;
								nStart = -1;
							}
						}
					}
					
					if (od != null && od.m_nProcessFlag == 1)
					{
						if (nStart == -1)
						{
							nStart = i;
							dMin = Double.parseDouble(od.m_strVar[nStatsField]);
						}
						else
						{
							double d = Double.parseDouble(od.m_strVar[nStatsField]);
							if (d < dMin)
								dMin = d;
						}
						odLast = od;
					}
				}
			}
			else
			{
				m_llOutput.clear();
				if (m_eStatsMethod == StatsMethod.PersistenceCount)
					str = m_strStationNumField + "," + m_strTimeField + ",PersistenceCount";
				else
					str = m_strStationNumField + "," + m_strTimeField + "," + m_eStatsMethod.toString() + "(" + m_strStatsField + ")";
				m_llOutput.add(str);
				if (m_eStatsMethod == StatsMethod.PersistenceCount)
					str = "VARCHAR,VARCHAR,INTEGER";
				else
					str = "VARCHAR,VARCHAR,DOUBLE";
				m_llOutput.add(str);
				
				ObservationData odLast = null;
				int nStart = -1, nCount = 0;
				double dStatsValue = 0.0;
				for (i = 0; i <= m_alObservationData.size(); i++)
				{
					ObservationData od = (i < m_alObservationData.size() ? m_alObservationData.get(i) : null);
					
					if (odLast != null)
					{
						if (od == null || !odLast.m_strNum.equals(od.m_strNum) || od.m_nProcessFlag == 0)
						{
							str = odLast.m_strNum + "," + sdf.format(m_alObservationData.get(nStart).m_dateTime);
							if (m_eStatsMethod == StatsMethod.PersistenceCount)
							{
								str += "," + String.format("%d", nCount);
							}
							else
							{
								if (m_eStatsMethod == StatsMethod.Avg)
									dStatsValue /= nCount;
								str += "," + String.format("%f", dStatsValue);
								dStatsValue = 0.0;
							}
							m_llOutput.add(str);
							//System.out.println(m_llOutput.getLast());
							
							odLast = null;
							nStart = -1;
							nCount = 0;
						}
					}
					
					if (od != null && od.m_nProcessFlag == 1)
					{
						if (nStart == -1)
							nStart = i;
						if (m_eStatsMethod == StatsMethod.Sum || m_eStatsMethod == StatsMethod.Avg)
						{
							dStatsValue += Double.parseDouble(od.m_strVar[nStatsField]);
						}
						else if (m_eStatsMethod == StatsMethod.Max)
						{
							double d = Double.parseDouble(od.m_strVar[nStatsField]);
							if (nCount == 0 || d > dStatsValue)
								dStatsValue = d;
						}
						else if (m_eStatsMethod == StatsMethod.Min)
						{
							double d = Double.parseDouble(od.m_strVar[nStatsField]);
							if (nCount == 0 || d < dStatsValue)
								dStatsValue = d;
						}
						++nCount;
						odLast = od;
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
			alParam.add(new Param("CSV", m_llOutput));
		
		return alParam.size();
	}
	
	String m_strStationNumField = "StationNum";
	String m_strTimeField = "Time";
	String m_strTimeFieldFormat = "yyyy-MM-dd";

	enum StatsMethod
	{
		Sum,
		Avg,
		Max,
		Min,
		Drop, //降幅
		PersistenceCount, //持续日数
	}
	StatsMethod m_eStatsMethod = StatsMethod.Drop;
	
	String m_strStatsField = "";
	
	LinkedList<String> m_llOutput = new LinkedList<String>();
	//---------------------------------------------------------------------------
	class ObservationData
	{
		ObservationData(String strNum, Date dateTime, String[] strVar, byte nProcessFlag)
		{
			m_strNum = strNum;
			m_dateTime = dateTime;
			m_strVar = strVar;
			m_nProcessFlag = nProcessFlag;
		}
		
		String m_strNum;
		Date m_dateTime;
		String[] m_strVar;
		byte m_nProcessFlag;
	}
	ArrayList<ObservationData> m_alObservationData = new ArrayList<ObservationData>();
}
