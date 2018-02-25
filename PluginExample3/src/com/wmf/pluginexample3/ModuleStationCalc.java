//-------------------------------------------------------------
// \project PluginExample3
// \file ModuleStationCalc.java
// \brief 站点计算模块
// \author 王庆飞
// \date 2017-4-13
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample3;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.wmf.model.*;

import org.da.expressionj.expr.parser.EquationParser;
import org.da.expressionj.model.Equation;

public class ModuleStationCalc extends Module 
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
	
	public ModuleStationCalc(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new MyPort(this));
		m_alOutputPort.add(new Port(this, Port.Type.CSV));
		
		m_alExpressionItem = new ArrayList<ExpressionItem>();
		m_alExpressionItem.add(new ExpressionItem());
    }

	public String GetGroupName()
	{
		return "JDBC";
	}
	public String GetName()
	{
		return "站点\n代数运算";
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
			
        	alParam.add(new Param("ExpressionItemCount", String.format("%d", m_alExpressionItem.size()), "算术表达式个数", "", "参数", Param.EditType.ItemCount, "2")); //"2":ExpressionItem参数个数
			for (int i = 0; i < m_alExpressionItem.size(); i++)
			{
				ExpressionItem ei = m_alExpressionItem.get(i);
				String strGroups = String.format("参数/第%d个算术表达式", i + 1);
				alParam.add(new Param("Expression", ei.m_strExpression, "算术表达式", "", strGroups));  
				alParam.add(new Param("Condition", ei.m_strCondition, "条件", "", strGroups));
			}
			
			alParam.add(new Param("StationNumField", m_strStationNumField, "区站号字段", "", "参数"));
			alParam.add(new Param("XField", m_strXField, "x字段", "", "参数"));
        	alParam.add(new Param("YField", m_strYField, "y字段", "", "参数"));
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
			
			m_strStationNumField = (String)alParam.get(i++).m_objValue;
			m_strXField = (String)alParam.get(i++).m_objValue;
			m_strYField = (String)alParam.get(i++).m_objValue;
			m_strOutputField = (String)alParam.get(i++).m_objValue;
			
			m_alOutputPort.get(0).SetName(m_strOutputField);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
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
			//获取变量
			ArrayList<String> alVarName = new ArrayList<String>();
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
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块表达式中没有变量。", GetNestedName(), GetID()));
				return false;
			}
			//------------------------------------------------------------------------------------------------------------------------------------
			//获取站点数据
			ArrayList<StationData> alStationData = new ArrayList<StationData>();
			HashMap<String, Integer> hmStationToIndex = new HashMap<String, Integer>(); 
			for (i = 0; i < m_alInputPort.size(); i++)
			{
				MyPort mp = (MyPort)m_alInputPort.get(i);
				if (mp.m_rs == null && mp.m_list == null)
					continue;
				int nStationNum = -1, x = -1, y = -1;
				int[] nVarIndex = new int[alVarName.size()];
				for (j = 0; j < nVarIndex.length; j++)
					nVarIndex[j] = -1;
				ResultSetMetaData rsmd = mp.m_rs != null ? mp.m_rs.getMetaData() : null;
				String[] strs = mp.m_list != null ? mp.m_list.get(0).split(",") : null;
				for (j = 0; j < (rsmd != null ? rsmd.getColumnCount() : strs.length); j++)
				{
					str = (rsmd != null ? rsmd.getColumnLabel(j + 1) : strs[j]);
					if (str.equalsIgnoreCase(m_strStationNumField))
						nStationNum = j;
					else if (str.equalsIgnoreCase(m_strXField))
						x = j;
					else if (str.equalsIgnoreCase(m_strYField))
						y = j;
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
				for (j = 0; j < nVarIndex.length; j++)
				{
					if (nVarIndex[j] != -1)
						break;
				}
				if (j == nVarIndex.length)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含变量字段。", GetNestedName(), GetID(), mp.GetName()));
					return false;
				}
				
				if (mp.m_rs != null)
				{
					mp.m_rs.beforeFirst();
					while (mp.m_rs.next())
					{
						StationData sd = null;
						str = mp.m_rs.getString(nStationNum + 1);
						if (!hmStationToIndex.containsKey(str))
						{
							hmStationToIndex.put(str, alStationData.size());
							
							double[] dVar = new double[nVarIndex.length];
							for (k = 0; k < nVarIndex.length; k++)
								dVar[k] = -9999.0;
							sd = new StationData(str, (x != -1 ? mp.m_rs.getDouble(x + 1) : -9999.0), (y != -1 ? mp.m_rs.getDouble(y + 1) : -9999.0), dVar);
							alStationData.add(sd);
						}
						else
						{
							sd = alStationData.get(hmStationToIndex.get(str));
						}
						for (k = 0; k < nVarIndex.length; k++)
						{
							if (nVarIndex[k] != -1)
								sd.m_dVar[k] = mp.m_rs.getDouble(nVarIndex[k] + 1);
						}
					}
				}
				else if (mp.m_list != null)
				{
					for (j = 2; j < mp.m_list.size(); j++)
		        	{
						strs = mp.m_list.get(j).split(",");
						StationData sd = null;
						str = strs[nStationNum];
						if (!hmStationToIndex.containsKey(str))
						{
							hmStationToIndex.put(str, alStationData.size());
							
							double[] dVar = new double[nVarIndex.length];
							for (k = 0; k < nVarIndex.length; k++)
								dVar[k] = -9999.0;
							sd = new StationData(str, (x != -1 ? Double.parseDouble(strs[x]) : -9999.0), (y != -1 ? Double.parseDouble(strs[y]) : -9999.0), dVar);
							alStationData.add(sd);
						}
						else
						{
							sd = alStationData.get(hmStationToIndex.get(str));
						}
						for (k = 0; k < nVarIndex.length; k++)
						{
							if (nVarIndex[k] != -1)
								sd.m_dVar[k] = Double.parseDouble(strs[nVarIndex[k]]);
						}
		        	}
				}
			}
			//------------------------------------------------------------------------------------------------------------------------------------
			//计算，输出
			m_llOutput.clear();
			m_llOutput.add(String.format("%s,%s,%s,%s", m_strStationNumField, m_strXField, m_strYField, m_strOutputField));
			m_llOutput.add("VARCHAR,DOUBLE,DOUBLE,DOUBLE");
			for (i = 0; i < alStationData.size(); i++)
			{
				StationData sd = alStationData.get(i);
				for (j = 0; j < m_alExpressionItem.size(); j++)
				{
					ExpressionItem ei = m_alExpressionItem.get(j);
					for (k = 0; k < alVarName.size(); k++)
					{
						if (ei.m_eCondition != null)
							ei.m_eCondition.setValue(alVarName.get(k), sd.m_dVar[k]);
						ei.m_eExpression.setValue(alVarName.get(k), sd.m_dVar[k]);
					}
					
					if (ei.m_eCondition != null ? ei.m_eCondition.evalAsBoolean() : true)
					{
						m_llOutput.add(String.format("%s,%f,%f,%f", sd.m_strNum, sd.m_x, sd.m_y, ei.m_eExpression.evalAsFloat()));
						//System.out.println(m_llOutput.getLast());
						break;
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
	
	String m_strStationNumField = "StationNum";
	String m_strXField = "x", m_strYField = "y";

	String m_strOutputField = "Value";
	LinkedList<String> m_llOutput = new LinkedList<String>();
	//---------------------------------------------------------------------------
	class StationData
	{
		StationData(String strNum, double x, double y, double[] dVar)
		{
			m_strNum = strNum;
			m_x = x;
			m_y = y;
			m_dVar = dVar;
		}
		
		String m_strNum;
		double m_x, m_y;
		double[] m_dVar;
	}
}
