//-------------------------------------------------------------
// \project PluginExample3
// \file ModulePersistenceFilter.java
// \brief 持续性过滤器模块
// \author 王庆飞
// \date 2017-4-5
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
import com.wmf.model.Port.Type;

import java.text.SimpleDateFormat;

import org.da.expressionj.expr.parser.EquationParser;
import org.da.expressionj.model.Equation;

public class ModulePersistenceFilter extends Module 
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
	
	public ModulePersistenceFilter(int nID) 
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
		return "持续性\n过滤器";
	}
	public String GetDescription()
	{
		return "根据持续条件过滤记录集；输入格式支持JDBC.ResultSet或CSV格式，输出为CSV格式。";
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
			alParam.add(new Param("TimeStep", String.format("%d", m_nTimeStep), "时间步长", "小时", "参数", Param.EditType.UInt));
			alParam.add(new Param("Condition", m_strCondition, "持续条件", "", "参数"));
        	
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
				MyPort mp = (MyPort)m_alInputPort.get(j);
				mp.SetName((String)alParam.get(i++).m_objValue);
				
				++j;
				if (j == m_alInputPort.size())
					m_alInputPort.add(new MyPort(this));
			}
			
			m_strStationNumField = (String)alParam.get(i++).m_objValue;
			m_strTimeField = (String)alParam.get(i++).m_objValue;
			m_strTimeFieldFormat = (String)alParam.get(i++).m_objValue;
			m_nTimeStep = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_strCondition = (String)alParam.get(i++).m_objValue;
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
						if (str.equalsIgnoreCase(m_strStationNumField) || str.equalsIgnoreCase(m_strTimeField))
							continue;
						alVarName.add(str);
					}
					if (alVarName.size() == 0)
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块输入端变量个数为0。", GetNestedName(), GetID()));
						return false;
					}
				}
				int nStationNum = -1, nTime = -1;
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
						double[] dVar = new double[nVarIndex.length];
						for (k = 0; k < nVarIndex.length; k++)
							dVar[k] = mp.m_rs.getDouble(nVarIndex[k] + 1);
						m_alObservationData.add(new ObservationData(mp.m_rs.getString(nStationNum + 1), sdf.parse(mp.m_rs.getString(nTime + 1)), dVar));
					}
				}
				else if (mp.m_list != null)
				{
					for (j = 2; j < mp.m_list.size(); j++)
		        	{
		        		strs = mp.m_list.get(j).split(",");
		        		double[] dVar = new double[nVarIndex.length];
						for (k = 0; k < nVarIndex.length; k++)
							dVar[k] = Double.parseDouble(strs[nVarIndex[k]]);
		        		m_alObservationData.add(new ObservationData(strs[nStationNum], sdf.parse(strs[nTime]), dVar));
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
			if (m_nTimeStep <= 0)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"时间步长\"必须>0。", GetNestedName(), GetID()));
				return false;
			}
			Equation eCondition = EquationParser.parse(m_strCondition);
			if (eCondition == null)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"持续条件\"为null。", GetNestedName(), GetID()));
				return false;
			}
			
			m_llOutput.clear();
			str = m_strStationNumField + "," + m_strTimeField;
			for (i = 0; i < alVarName.size(); i++)
				str += "," + alVarName.get(i);
			m_llOutput.add(str);
			str = "VARCHAR,VARCHAR";
			for (i = 0; i < alVarName.size(); i++)
				str += ",DOUBLE";
			m_llOutput.add(str);
			
			ObservationData odLast = null;
			int nStart = -1, nCount = 0;
			for (i = 0; i <= m_alObservationData.size(); i++)
			{
				ObservationData od = (i < m_alObservationData.size() ? m_alObservationData.get(i) : null);
				
				//输出
				if (odLast != null)
				{
					if (od == null || !odLast.m_strNum.equals(od.m_strNum) || (od.m_dateTime.getTime() - odLast.m_dateTime.getTime()) / 3600000 > m_nTimeStep)
					{
						eCondition.setValue("count", nCount);
						if (eCondition.evalAsBoolean())
						{
							for (j = 0; j < nCount; j++)
							{
								ObservationData odTemp = m_alObservationData.get(nStart + j);
								str = odTemp.m_strNum + "," + sdf.format(odTemp.m_dateTime);
								for (k = 0; k < odTemp.m_dVar.length; k++)
									str += String.format(",%f", odTemp.m_dVar[k]);
								m_llOutput.add(str);
								//System.out.println(m_llOutput.getLast());
							}
						}	
						odLast = null;
						nStart = -1;
						nCount = 0;
					}
				}
				
				if (od != null)
				{
					if (nStart == -1)
						nStart = i;
					++nCount;
					odLast = od; 
				}
			}
			
			OnParamChanged();
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		catch (Error error)
		{
			error.printStackTrace();
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
	int m_nTimeStep = 24;
	String m_strCondition = "count>=5";
	
	LinkedList<String> m_llOutput = new LinkedList<String>();
	//---------------------------------------------------------------------------
	class ObservationData
	{
		ObservationData(String strNum, Date dateTime, double[] dVar)
		{
			m_strNum = strNum;
			m_dateTime = dateTime;
			m_dVar = dVar;
		}
		
		String m_strNum;
		Date m_dateTime;
		double[] m_dVar;
	}
	ArrayList<ObservationData> m_alObservationData = new ArrayList<ObservationData>();
}
