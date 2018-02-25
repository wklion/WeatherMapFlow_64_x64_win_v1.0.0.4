//-------------------------------------------------------------
// \project PluginExample3
// \file ModulePeriodStatistics.java
// \brief 站点时段 统计模块
// \author 王庆飞
// \date 2017-3-22
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample3;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.wmf.model.*;

import org.da.expressionj.expr.parser.EquationParser;
import org.da.expressionj.model.Equation;

public class ModulePeriodStatistics extends Module 
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
	
	public ModulePeriodStatistics(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new MyPort(this));
		m_alOutputPort.add(new Port(this, Port.Type.CSV));
		
		m_alExpressionItem = new ArrayList<ExpressionItem>();
		m_alExpressionItem.add(new ExpressionItem());
		
		m_eStatsMethod = StatsMethod.Sum;
    }

	public String GetGroupName()
	{
		return "JDBC";
	}
	public String GetName()
	{
		return "站点\n时段统计";
	}
	public String GetDescription()
	{
		return "输入格式支持JDBC.ResultSet或CSV格式，第一个输入端口为站点信息，其他输入端口为观测数据；输出为CSV格式。";
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
		case Count:
			return "个数";
		case MovingAvg:
			return "滑动平均";
		case MovingAvg_StartTime:
			return "滑动平均_稳定通过时间";
		case Accumulation:
			return "积温";
		case PersistenceCount:
			return "从当日开始的持续日数";
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
			
        	alParam.add(new Param("ExpressionItemCount", String.format("%d", m_alExpressionItem.size()), "算术表达式个数", "用于计算被统计变量", "参数", Param.EditType.ItemCount, "2")); //"2":ExpressionItem参数个数
			for (int i = 0; i < m_alExpressionItem.size(); i++)
			{
				ExpressionItem ei = m_alExpressionItem.get(i);
				String strGroups = String.format("参数/第%d个算术表达式", i + 1);
				alParam.add(new Param("Expression", ei.m_strExpression, "算术表达式", "", strGroups));  
				alParam.add(new Param("Condition", ei.m_strCondition, "条件", "", strGroups));
			}
			
			alParam.add(new Param("Model", m_modelCoeff != null ? m_modelCoeff.ToXML() : "", "系数模型", 
					"输入区站号、时间、系数名称，输出系数值。", "参数", Param.EditType.File, "模型文件(*.xml)|*.xml"));
			
			alParam.add(new Param("StationNumField", m_strStationNumField, "区站号字段", "", "参数/字段"));
			alParam.add(new Param("XField", m_strXField, "x字段", "", "参数/字段"));
        	alParam.add(new Param("YField", m_strYField, "y字段", "", "参数/字段"));
			alParam.add(new Param("TimeField", m_strTimeField, "时间字段", "", "参数/字段"));
			alParam.add(new Param("TimeFieldFormat", m_strTimeFieldFormat, "时间字段格式", "", "参数/字段"));
			alParam.add(new Param("StatsMethod", m_eStatsMethod.toString(), "统计方法", GetDescription(m_eStatsMethod), "参数", Param.EditType.Select, 
					"Sum|Avg|Max|Min|Count|MovingAvg|MovingAvg_StartTime|Accumulation|PersistenceCount"));
			
			boolean b = (m_eStatsMethod == StatsMethod.MovingAvg || m_eStatsMethod == StatsMethod.MovingAvg_StartTime || m_eStatsMethod == StatsMethod.Accumulation);
			alParam.add(new Param("MovingAvgCondition", m_strMovingAvgCondition, "滑动平均条件", "例如：t>=15，t为日平均气温", "参数/MovingAvg|MovingAvg_StartTime|Accumulation", b ? Param.EditType.Default : Param.EditType.ReadOnly));
			alParam.add(new Param("MovingAvgStepCount", String.format("%d", m_nMovingAvgStepCount), "滑动平均步数", "", "参数/MovingAvg|MovingAvg_StartTime|Accumulation", b ? Param.EditType.UInt : Param.EditType.ReadOnly));
			alParam.add(new Param("MovingAvgStartStep", String.format("%d", m_nMovingAvgStartStep), "滑动平均开始步", "", "参数/MovingAvg|MovingAvg_StartTime|Accumulation", b ? Param.EditType.Int : Param.EditType.ReadOnly));
			
			b = (m_eStatsMethod == StatsMethod.PersistenceCount);
			alParam.add(new Param("PersistenceCondition", m_strPersistenceCondition, "持续条件", "例如：Value>=1.0，Value为输出字段", "参数/PersistenceCount", b ? Param.EditType.Default : Param.EditType.ReadOnly));
			
			alParam.add(new Param("OutputField", m_strOutputField, "输出字段", "", "输出端"));
			
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
		if (alParam.get(nIndex).m_strName.equals("ExpressionItemCount"))
		{
			int i = 0, j = 0, nOldItemCount = Integer.parseInt((String)alParam.get(nIndex).m_objValue), nNewItemCount = Integer.parseInt((String)objValue);
			if (nOldItemCount < nNewItemCount)
			{
				j = nIndex + 1 + nOldItemCount * 2;
				for (i = nOldItemCount; i < nNewItemCount; i++)
				{
					String strGroups = String.format("参数/第%d个算术表达式", i + 1);
					alParam.add(j++, new Param("Expression", "", "算术表达式", "", strGroups));  
					alParam.add(j++, new Param("Condition", "", "条件", "", strGroups));
				}
			}
			else
			{
				j = nIndex + 1 + nNewItemCount * 2;
				for (i = nNewItemCount * 2; i < nOldItemCount * 2; i++)
					alParam.remove(j);
			}
			return true;
		}
		else if (alParam.get(nIndex).m_strName.equals("StatsMethod"))
		{
			StatsMethod e = StatsMethod.valueOf((String)objValue);
			alParam.get(nIndex).m_strDescription = GetDescription(e);
			
			boolean b = (e == StatsMethod.MovingAvg || e == StatsMethod.MovingAvg_StartTime || e == StatsMethod.Accumulation);  
			alParam.get(nIndex + 1).m_eEditType = b ? Param.EditType.Default : Param.EditType.ReadOnly;
			alParam.get(nIndex + 2).m_eEditType = b ? Param.EditType.UInt : Param.EditType.ReadOnly;
			alParam.get(nIndex + 3).m_eEditType = b ? Param.EditType.Int : Param.EditType.ReadOnly;
			
			b = (e == StatsMethod.PersistenceCount);  
			alParam.get(nIndex + 4).m_eEditType = b ? Param.EditType.Default : Param.EditType.ReadOnly;
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
			
			final int nExpressionItemCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_alExpressionItem.clear();
			while (m_alExpressionItem.size() < nExpressionItemCount)
			{
				ExpressionItem ei = new ExpressionItem();
				ei.m_strExpression = (String)alParam.get(i++).m_objValue;
				ei.m_strCondition = (String)alParam.get(i++).m_objValue;
				m_alExpressionItem.add(ei);
			}
			
			m_modelCoeff = Model.FromXML((String)alParam.get(i++).m_objValue, m_model.m_strFileName);
			
			m_strStationNumField = (String)alParam.get(i++).m_objValue;
			m_strXField = (String)alParam.get(i++).m_objValue;
			m_strYField = (String)alParam.get(i++).m_objValue;
			m_strTimeField = (String)alParam.get(i++).m_objValue;
			m_strTimeFieldFormat = (String)alParam.get(i++).m_objValue;
			m_eStatsMethod = StatsMethod.valueOf((String)alParam.get(i++).m_objValue);
			
			m_strMovingAvgCondition = (String)alParam.get(i++).m_objValue;
			m_nMovingAvgStepCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_nMovingAvgStartStep = Integer.parseInt((String)alParam.get(i++).m_objValue);
			
			m_strPersistenceCondition = (String)alParam.get(i++).m_objValue;
			
			m_strOutputField = (String)alParam.get(i++).m_objValue;
			//----------------------------------------------------------------------------------------------------
			if (m_modelCoeff != null)
				m_modelCoeff.m_moduleContainer = this;
			m_alOutputPort.get(0).SetName(m_strOutputField);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	public Object GetParam(String strName)
	{
		if (strName.equalsIgnoreCase("Model"))
		{
			return m_modelCoeff;
		}
		return null;
	}
	public boolean SetParam(String strName, Object objValue)
	{
		try
		{
			if (strName.equalsIgnoreCase("Model"))
			{
				m_modelCoeff = Model.FromXML(((Model)objValue).ToXML(), m_model.m_strFileName);
				if (m_modelCoeff == null)
					return false;				
				m_modelCoeff.m_moduleContainer = this;
				return true;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
	
	void GetVarName(Equation e, ArrayList<String> alVarName)
	{
		if (e == null)
			return;
		String str;
		int i = 0;
		Set<String> set = e.getVariables().keySet();
		Iterator<String> it = set.iterator();
		while (it.hasNext())
		{
			str = it.next();
			for (i = 0; i < alVarName.size(); i++)
			{
				if (alVarName.get(i).equalsIgnoreCase(str))
					break;
			}
			if (i == alVarName.size())
				alVarName.add(str);
		}
	}
	public boolean Execute()
	{
		try
		{
			int i = 0, j = 0, k = 0;
			String str;
			//------------------------------------------------------------------------------------------------------------------------------------
			//初始化表达式
			ArrayList<String> alVarName = new ArrayList<String>(); //被统计变量
			Equation eMovingAvgCondition = null, ePersistenceCondition = null;
			if (m_eStatsMethod == StatsMethod.MovingAvg || m_eStatsMethod == StatsMethod.MovingAvg_StartTime || m_eStatsMethod == StatsMethod.Accumulation)
			{
				eMovingAvgCondition = EquationParser.parse(m_strMovingAvgCondition);
				if (eMovingAvgCondition == null)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"滑动平均条件\"为null。", GetNestedName(), GetID()));
					return false;
				}
				if (m_nMovingAvgStepCount <= 0)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"滑动平均步数\"必须>0。", GetNestedName(), GetID()));
					return false;
				}
				if (m_nMovingAvgStartStep <= -m_nMovingAvgStepCount || m_nMovingAvgStartStep > 0)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"滑动平均开始步\"必须>=%d并且<=0。", GetNestedName(), GetID(), 1 - m_nMovingAvgStepCount));
					return false;
				}
				GetVarName(eMovingAvgCondition, alVarName);
				if (alVarName.size() != 1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块被统计变量个数!=1。", GetNestedName(), GetID()));
					return false;
				}
			}
			else
			{
				for (i = 0; i < m_alExpressionItem.size(); i++)
				{
					ExpressionItem ei = m_alExpressionItem.get(i);
					if (ei.m_strExpression.isEmpty())
						return false;
					ei.m_eExpression = EquationParser.parse(ei.m_strExpression);
					GetVarName(ei.m_eExpression, alVarName);
					ei.m_eCondition = !ei.m_strCondition.isEmpty() ? EquationParser.parse(ei.m_strCondition) : null;
					GetVarName(ei.m_eCondition, alVarName);
				}
				if (alVarName.size() == 0)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块没有被统计变量。", GetNestedName(), GetID()));
					return false;
				}
				
				if (m_eStatsMethod == StatsMethod.PersistenceCount)
				{
					ePersistenceCondition = EquationParser.parse(m_strPersistenceCondition);
					if (ePersistenceCondition == null)
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"持续条件\"为null。", GetNestedName(), GetID()));
						return false;
					}
					ArrayList<String> al = new ArrayList<String>();
					GetVarName(ePersistenceCondition, al);
					if (al.size() != 1 || !al.get(0).equalsIgnoreCase(m_strOutputField))
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"持续条件\"只能包含输出字段\"%s\"。", GetNestedName(), GetID(), m_strOutputField));
						return false;
					}
				}
			}
			
			//------------------------------------------------------------------------------------------------------------------------------------
			//获取站点信息
			int nStationNum = -1, x = -1, y = -1;
			MyPort mp = (MyPort)m_alInputPort.get(0);
			if (mp.m_rs == null && mp.m_list == null)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块没有站点信息。", GetNestedName(), GetID()));
				return false;
			}
			ResultSetMetaData rsmd = mp.m_rs != null ? mp.m_rs.getMetaData() : null;
			String[] strs = mp.m_list != null ? mp.m_list.get(0).split(",") : null;
			for (i = 0; i < (rsmd != null ? rsmd.getColumnCount() : strs.length); i++)
			{
				str = (rsmd != null ? rsmd.getColumnLabel(i + 1) : strs[i]);
				if (str.equalsIgnoreCase(m_strStationNumField))
					nStationNum = i;
				else if (str.equalsIgnoreCase(m_strXField))
					x = i;
				else if (str.equalsIgnoreCase(m_strYField))
					y = i;
			}			
			if (nStationNum == -1)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含区站号字段\"%s\"。", GetNestedName(), GetID(), mp.GetName(), m_strStationNumField));
				return false;
			}
			if (x == -1)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含x字段\"%s\"。", GetNestedName(), GetID(), mp.GetName(), m_strXField));
				return false;
			}
			if (y == -1)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含y字段\"%s\"。", GetNestedName(), GetID(), mp.GetName(), m_strYField));
				return false;
			}
			m_alStationInfo.clear();
			if (mp.m_rs != null)
			{
				mp.m_rs.beforeFirst();
				while (mp.m_rs.next())
				{
					m_alStationInfo.add(new StationInfo(mp.m_rs.getString(nStationNum + 1), mp.m_rs.getDouble(x + 1), mp.m_rs.getDouble(y + 1)));
				}
			}
			else if (mp.m_list != null)
			{
				for (i = 2; i < mp.m_list.size(); i++)
	        	{
	        		strs = mp.m_list.get(i).split(",");
	        		m_alStationInfo.add(new StationInfo(strs[nStationNum], Double.parseDouble(strs[x]), Double.parseDouble(strs[y])));
	        	}
			}
			Collections.sort(m_alStationInfo, new Comparator<StationInfo>()
			{
				public int compare(StationInfo si1, StationInfo si2)
				{
					return si1.m_strNum.compareToIgnoreCase(si2.m_strNum);
				}
			});
			//------------------------------------------------------------------------------------------------------------------------------------
			//获取观测数据
			SimpleDateFormat sdf = new SimpleDateFormat(m_strTimeFieldFormat);
			m_alObservationData.clear();
			ArrayList<String> alInputVarName = new ArrayList<String>(), alCoeffName = new ArrayList<String>();
			for (i = 1; i < m_alInputPort.size(); i++)
			{
				mp = (MyPort)m_alInputPort.get(i);
				if (mp.m_rs == null && mp.m_list == null)
					continue;
				rsmd = mp.m_rs != null ? mp.m_rs.getMetaData() : null;
				strs = mp.m_list != null ? mp.m_list.get(0).split(",") : null;
				if (alInputVarName.size() == 0)
				{
					for (j = 0; j < (rsmd != null ? rsmd.getColumnCount() : strs.length); j++)
					{
						str = (rsmd != null ? rsmd.getColumnLabel(j + 1) : strs[j]);
						if (str.equalsIgnoreCase(m_strStationNumField) || str.equalsIgnoreCase(m_strTimeField))
							continue;
						alInputVarName.add(str);
					}
					if (alInputVarName.size() == 0)
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块输入端变量个数为0。", GetNestedName(), GetID()));
						return false;
					}
					
					for (j = 0; j < alVarName.size(); j++)
					{
						for (k = 0; k < alInputVarName.size(); k++)
						{
							if (alInputVarName.get(k).equalsIgnoreCase(alVarName.get(j)))
								break;
						}
						if (k == alInputVarName.size())
						{
							alCoeffName.add(alVarName.get(j));
							alVarName.remove(j);
						}
					}
				}
				nStationNum = -1;
				int nTime = -1;
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
			//计算统计
	        double[] dCoeff = alCoeffName.size() > 0 ? new double[alCoeffName.size()] : null;
			
			m_llOutput.clear();
			if (m_eStatsMethod == StatsMethod.MovingAvg)
			{
				m_llOutput.add(String.format("%s,%s,%s", m_strStationNumField, m_strTimeField, m_strOutputField));
				m_llOutput.add("VARCHAR,VARCHAR,DOUBLE");
			}
			else if (m_eStatsMethod == StatsMethod.MovingAvg_StartTime)
			{
				m_llOutput.add(String.format("%s,%s,%s,%s,%s", m_strStationNumField, m_strXField, m_strYField, m_strTimeField, m_strOutputField));
				m_llOutput.add("VARCHAR,DOUBLE,DOUBLE,VARCHAR,DOUBLE");
			}
			else if (m_eStatsMethod == StatsMethod.Accumulation)
			{
				m_llOutput.add(String.format("%s,%s,%s,%s_Start,%s_End,%s", m_strStationNumField, m_strXField, m_strYField, m_strTimeField, m_strTimeField, m_strOutputField));
				m_llOutput.add("VARCHAR,DOUBLE,DOUBLE,VARCHAR,VARCHAR,DOUBLE");
			}
			else if (m_eStatsMethod == StatsMethod.PersistenceCount)
			{
				m_llOutput.add(String.format("%s,%s,%s,PersistenceCount", m_strStationNumField, m_strTimeField, m_strOutputField));
				m_llOutput.add("VARCHAR,VARCHAR,DOUBLE,INTEGER");
			}
			else
			{
				m_llOutput.add(String.format("%s,%s,%s,%s", m_strStationNumField, m_strXField, m_strYField, m_strOutputField));
				m_llOutput.add("VARCHAR,DOUBLE,DOUBLE,DOUBLE");
			}
			
			String strNumLast = "";
			nStationNum = 0;
			StationInfo si = null;
			double dStatsValue = 0.0;
			int nCount = 0, nStart = -1, nStartLast = -1;
			ArrayList<Double> alValue = new ArrayList<Double>(), alValueLast = new ArrayList<Double>(); 
			for (i = 0; i <= m_alObservationData.size(); i++)
			{
				ObservationData od = (i < m_alObservationData.size() ? m_alObservationData.get(i) : null);
				String strNum = (od != null ? od.m_strNum : "");
				if (!strNum.equals(strNumLast))
				{
					while (nStationNum < m_alStationInfo.size())
					{
						int n = m_alStationInfo.get(nStationNum).m_strNum.compareToIgnoreCase(strNum);
						if (n >= 0)
						{
							if (n == 0)
								si = m_alStationInfo.get(nStationNum);
							break;
						}
						++nStationNum;
					}
				}
					
				//输出统计结果
				if (!strNumLast.isEmpty() && !strNum.equals(strNumLast))
				{
					if (m_eStatsMethod == StatsMethod.MovingAvg || m_eStatsMethod == StatsMethod.MovingAvg_StartTime || m_eStatsMethod == StatsMethod.Accumulation)
					{
						if (alValue.size() > alValueLast.size())
						{
							nStartLast = nStart;
							alValueLast.clear();
							alValueLast.addAll(alValue);
						}
						nStart = -1;
						alValue.clear();
					
						if (m_eStatsMethod == StatsMethod.MovingAvg)
						{
							for (j = 0; j < alValueLast.size(); j++)
							{
								m_llOutput.add(String.format("%s,%s,%f", strNumLast, 
										sdf.format(m_alObservationData.get(nStartLast + j).m_dateTime), alValueLast.get(j)));
							}
						}
						else if (m_eStatsMethod == StatsMethod.MovingAvg_StartTime)
						{
							if (alValueLast.size() > 0)
							{
								m_llOutput.add(String.format("%s,%f,%f,%s,%d", strNumLast, (si != null ? si.m_x : 9999.0), (si != null ? si.m_y : 9999.0), 
										sdf.format(m_alObservationData.get(nStartLast).m_dateTime), m_alObservationData.get(nStartLast).m_dateTime.getTime()));
							}
						}
						else if (m_eStatsMethod == StatsMethod.Accumulation)
						{
							if (alValueLast.size() > 0)
							{
								dStatsValue = 0.0;
								for (j = 0; j < alValueLast.size(); j++)
									dStatsValue += m_alObservationData.get(nStartLast + j).m_dVar[0];
								m_llOutput.add(String.format("%s,%f,%f,%s,%s,%f", strNumLast, (si != null ? si.m_x : 9999.0), (si != null ? si.m_y : 9999.0), 
										sdf.format(m_alObservationData.get(nStartLast).m_dateTime), 
										sdf.format(m_alObservationData.get(nStartLast + alValueLast.size() - 1).m_dateTime), dStatsValue));
							}
						}
						nStartLast = -1;
						alValueLast.clear();
					}
					else if (m_eStatsMethod == StatsMethod.PersistenceCount)
					{
						BitSet bs = new BitSet();
						for (j = 0; j < alValue.size(); j++)
						{
							ePersistenceCondition.setValue(m_strOutputField, alValue.get(j));
							if (ePersistenceCondition.evalAsBoolean())
								bs.set(j);
						}
						for (j = 0; j < alValue.size(); j++)
						{
							nCount = 0;
							for (k = j; k < alValue.size(); k++)
							{
								if (!bs.get(k))
									break;
								++nCount;
							}
							m_llOutput.add(String.format("%s,%s,%f,%d", strNumLast, 
									sdf.format(m_alObservationData.get(nStart + j).m_dateTime), alValue.get(j), nCount));
						}
						
						nStart = -1;
						alValue.clear();
					}
					else
					{
						if (m_eStatsMethod == StatsMethod.Avg)
							dStatsValue /= nCount;						
						else if (m_eStatsMethod == StatsMethod.Count)
							dStatsValue = nCount;
						m_llOutput.add(String.format("%s,%f,%f,%f", strNumLast, (si != null ? si.m_x : 9999.0), (si != null ? si.m_y : 9999.0), dStatsValue));
						
						dStatsValue = 0.0;
						nCount = 0;
					}
				}
				
				if (od != null)
				{
					if (alCoeffName.size() > 0)
					{
						if (m_modelCoeff == null)
							throw new AssertionError();
						m_modelCoeff.m_dateStart = od.m_dateTime;
						m_modelCoeff.m_dateDataStart = od.m_dateTime;
						m_modelCoeff.m_dateDataEnd = od.m_dateTime;
						m_modelCoeff.SetParam("StationNum", od.m_strNum);
						m_modelCoeff.Execute();
						for (j = 0; j < alCoeffName.size(); j++)
							dCoeff[j] = (Double)m_modelCoeff.GetParam(alCoeffName.get(j));
					}
					
					double dValue = 0.0;
					if (m_eStatsMethod == StatsMethod.MovingAvg || m_eStatsMethod == StatsMethod.MovingAvg_StartTime || m_eStatsMethod == StatsMethod.Accumulation)
					{
						for (j = 0; j < m_nMovingAvgStepCount; j++)
						{
							k = i + m_nMovingAvgStartStep + j;
							if (k < 0 || k >= m_alObservationData.size())
								break;
							dValue += m_alObservationData.get(k).m_dVar[0];
						}
						if (j == m_nMovingAvgStepCount)
						{
							dValue /= m_nMovingAvgStepCount; 
							eMovingAvgCondition.setValue(alVarName.get(0), dValue);
							if (eMovingAvgCondition.evalAsBoolean())
							{
								if (alValue.size() == 0)
									nStart = i;
								alValue.add(dValue);
							}
							else
							{
								if (alValue.size() > alValueLast.size())
								{
									nStartLast = nStart;
									alValueLast.clear();
									alValueLast.addAll(alValue);
								}
								nStart = -1;
								alValue.clear();
							}
						}
					}
					else
					{
						for (j = 0; j < m_alExpressionItem.size(); j++)
						{
							ExpressionItem ei = m_alExpressionItem.get(j);
							for (k = 0; k < od.m_dVar.length; k++)
							{
								ei.m_eExpression.setValue(alVarName.get(k), od.m_dVar[k]);
								if (ei.m_eCondition != null)
									ei.m_eCondition.setValue(alVarName.get(k), od.m_dVar[k]);
							}
							
							for (k = 0; k < alCoeffName.size(); k++)
							{
								ei.m_eExpression.setValue(alCoeffName.get(k), dCoeff[k]);
								if (ei.m_eCondition != null)
									ei.m_eCondition.setValue(alCoeffName.get(k), dCoeff[k]);
							}
							
							if (ei.m_eCondition != null ? ei.m_eCondition.evalAsBoolean() : true)
							{
								dValue = ei.m_eExpression.evalAsFloat();
								break;
							}
						}
						
						if (m_eStatsMethod == StatsMethod.Sum || m_eStatsMethod == StatsMethod.Avg)
						{
							dStatsValue += dValue;
						}
						else if (m_eStatsMethod == StatsMethod.Max)
						{
							if (nCount == 0 || dValue > dStatsValue)
								dStatsValue = dValue;
						}
						else if (m_eStatsMethod == StatsMethod.Min)
						{
							if (nCount == 0 || dValue < dStatsValue)
								dStatsValue = dValue;
						}
						else if (m_eStatsMethod == StatsMethod.PersistenceCount)
						{
							if (alValue.size() == 0)
								nStart = i;
							alValue.add(dValue);
						}
						++nCount;
					}
					
					strNumLast = strNum;
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
	//---------------------------------------------------------------------------
	class ExpressionItem
	{
		ExpressionItem()
		{
			m_strExpression = "";
			m_strCondition = "";
		}
		
		String m_strExpression;
		String m_strCondition;
		
		Equation m_eExpression;
		Equation m_eCondition;
	}
	ArrayList<ExpressionItem> m_alExpressionItem;
	
	Model m_modelCoeff = null;

	String m_strStationNumField = "StationNum";
	String m_strXField = "x", m_strYField = "y";
	String m_strTimeField = "Time";
	String m_strTimeFieldFormat = "yyyy-MM-dd";
	
	enum StatsMethod
	{
		Sum,
		Avg,
		Max,
		Min,
		Count,
		MovingAvg, //滑动平均
		MovingAvg_StartTime, //滑动平均_稳定通过时间
		Accumulation, //积温
		PersistenceCount //从当日开始的持续日数
	}
	StatsMethod m_eStatsMethod;
	
	String m_strMovingAvgCondition = "t>=15";
	int m_nMovingAvgStepCount = 5;
	int m_nMovingAvgStartStep = 0;
	
	String m_strPersistenceCondition = "Value>=1.0";
	
	String m_strOutputField = "Value";
	LinkedList<String> m_llOutput = new LinkedList<String>();
	//---------------------------------------------------------------------------
	class StationInfo
	{
		StationInfo(String strNum, double x, double y)
		{
			m_strNum = strNum;
			m_x = x;
			m_y = y;
		}
		
		String m_strNum;
		double m_x, m_y;
	}
	ArrayList<StationInfo> m_alStationInfo = new ArrayList<StationInfo>();
	
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
