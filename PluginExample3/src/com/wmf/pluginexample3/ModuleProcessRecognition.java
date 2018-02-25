//-------------------------------------------------------------
// \project PluginExample3
// \file ModuleProcessRecognition.java
// \brief 过程识别模块
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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.wmf.model.*;
import com.wmf.pluginexample1.*;

import java.text.SimpleDateFormat;

import org.da.expressionj.expr.parser.EquationParser;
import org.da.expressionj.model.Equation;

public class ModuleProcessRecognition extends Module 
{
	public ModuleProcessRecognition(int nID) 
	{
		super(nID);
		
		m_alInputPort.add(new DataTablePort(this));
		m_alOutputPort.add(new Port(this, Port.Type.CSV));
    }

	public String GetGroupName()
	{
		return "JDBC";
	}
	public String GetName()
	{
		return "过程识别";
	}
	public String GetDescription()
	{
		return "识别寒害过程等；输入格式支持JDBC.ResultSet或CSV格式，输出为CSV格式。";
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
			
			alParam.add(new Param("StationNumField", m_strStationNumField, "区站号字段", "", "参数"));
			alParam.add(new Param("TimeField", m_strTimeField, "时间字段", "", "参数"));
			alParam.add(new Param("TimeFieldFormat", m_strTimeFieldFormat, "时间字段格式", "", "参数"));
			alParam.add(new Param("StartCondition", m_strStartCondition, "开始条件", "例如：(t<=15)&&(s<=2)，t为日平均气温，s为日照时数", "参数"));
			alParam.add(new Param("PersistenceCondition", m_strPersistenceCondition, "持续条件", "例如：count>=5，可以为空", "参数"));
        	
			alParam.add(new Param("Model", m_modelCoeff != null ? m_modelCoeff.ToXML() : "", "系数模型", 
					"输入区站号、时间、系数名称，输出系数值。", "参数", Param.EditType.File, "模型文件(*.xml)|*.xml"));
			
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
			m_strTimeFieldFormat = (String)alParam.get(i++).m_objValue;
			m_strStartCondition = (String)alParam.get(i++).m_objValue;			
			m_strPersistenceCondition = (String)alParam.get(i++).m_objValue;
			
			m_modelCoeff = Model.FromXML((String)alParam.get(i++).m_objValue, m_model.m_strFileName);
			if (m_modelCoeff != null)
				m_modelCoeff.m_moduleContainer = this;
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
			ArrayList<String> alVarName = new ArrayList<String>();
			
			Equation eStartCondition = EquationParser.parse(m_strStartCondition);
			if (eStartCondition == null)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"开始条件\"为null。", GetNestedName(), GetID()));
				return false;
			}
			GetVarName(eStartCondition, alVarName);
			Equation ePersistenceCondition = (!m_strPersistenceCondition.isEmpty() ? EquationParser.parse(m_strPersistenceCondition) : null);
			//------------------------------------------------------------------------------------------------------------------------------------
			//获取观测数据
			SimpleDateFormat sdf = new SimpleDateFormat(m_strTimeFieldFormat);
			m_alObservationData.clear();
			
			ArrayList<String> alInputVarName = new ArrayList<String>(), alCoeffName = new ArrayList<String>();
			ArrayList<String> alInputVarType = new ArrayList<String>();
			for (i = 0; i < m_alInputPort.size(); i++)
			{
				DataTablePort dtp = (DataTablePort)m_alInputPort.get(i);
				if (dtp.m_rs == null && dtp.m_list == null)
					continue;
				ResultSetMetaData rsmd = dtp.m_rs != null ? dtp.m_rs.getMetaData() : null;
				String[] strs = dtp.m_list != null ? dtp.m_list.get(0).split(",") : null;
				if (alInputVarName.size() == 0)
				{
					dtp.GetFieldName(alInputVarName);
					dtp.GetFieldType(alInputVarType);
					for (j = alInputVarName.size() - 1; j >= 0; j--)
					{
						str = alInputVarName.get(j);
						if (str.equalsIgnoreCase(m_strStationNumField) || str.equalsIgnoreCase(m_strTimeField))
						{
							alInputVarName.remove(j);
							alInputVarType.remove(j);
						}
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
				int nStationNum = -1, nTime = -1;
				int[] nVarIndex = new int[alInputVarName.size()];
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
						for (k = 0; k < alInputVarName.size(); k++)
						{
							if (alInputVarName.get(k).equalsIgnoreCase(str))
							{
								nVarIndex[k] = j;
								break;
							}
						}
					}
				}	
				if (nStationNum == -1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含区站号字段\"%s\"。", GetNestedName(), GetID(), dtp.GetName(), m_strStationNumField));
					return false;
				}
				if (nTime == -1)
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含时间字段\"%s\"。", GetNestedName(), GetID(), dtp.GetName(), m_strTimeField));
					return false;
				}
				for (j = 0; j < nVarIndex.length; j++)
				{
					if (nVarIndex[j] == -1)
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"%s\"不包含被统计变量字段\"%s\"。", GetNestedName(), GetID(), dtp.GetName(), alInputVarName.get(j)));
						return false;
					}
				}
				if (dtp.m_rs != null)
				{
					dtp.m_rs.beforeFirst();
					while (dtp.m_rs.next())
					{
						String[] strVar = new String[nVarIndex.length];
						for (k = 0; k < nVarIndex.length; k++)
							strVar[k] = dtp.m_rs.getString(nVarIndex[k] + 1);
						m_alObservationData.add(new ObservationData(dtp.m_rs.getString(nStationNum + 1), sdf.parse(dtp.m_rs.getString(nTime + 1)), strVar));
					}
				}
				else if (dtp.m_list != null)
				{
					for (j = 2; j < dtp.m_list.size(); j++)
		        	{
		        		strs = dtp.m_list.get(j).split(",");
		        		String[] strVar = new String[nVarIndex.length];
						for (k = 0; k < nVarIndex.length; k++)
							strVar[k] = strs[nVarIndex[k]];
		        		m_alObservationData.add(new ObservationData(strs[nStationNum], sdf.parse(strs[nTime]), strVar));
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
			m_llOutput.clear();
			str = m_strStationNumField + "," + m_strTimeField;
			for (i = 0; i < alInputVarName.size(); i++)
				str += "," + alInputVarName.get(i);
			str += ",ProcessFlag";
			m_llOutput.add(str);
			str = "VARCHAR,VARCHAR";
			for (i = 0; i < alInputVarType.size(); i++)
				str += "," + alInputVarType.get(i);
			str += ",TINYINT";
			m_llOutput.add(str);
			
			//根据开始条件识别
			BitSet bsProcessFlag = new BitSet();
			for (i = 0; i < m_alObservationData.size(); i++)
			{
				ObservationData od = m_alObservationData.get(i);
				for (j = 0; j < alVarName.size(); j++)
					eStartCondition.setValue(alVarName.get(j), Double.parseDouble(od.m_strVar[j]));
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
						eStartCondition.setValue(alCoeffName.get(j), m_modelCoeff.GetParam(alCoeffName.get(j)));
				}
				if (eStartCondition.evalAsBoolean())
					bsProcessFlag.set(i);
			}

			//根据持续条件识别
			if (ePersistenceCondition != null)
			{
				ObservationData odLast = null;
				int nStart = -1, nCount = 0;
				for (i = 0; i <= m_alObservationData.size(); i++)
				{
					ObservationData od = (i < m_alObservationData.size() ? m_alObservationData.get(i) : null);
					
					if (odLast != null)
					{
						if (od == null || !odLast.m_strNum.equals(od.m_strNum) || !bsProcessFlag.get(i))
						{
							ePersistenceCondition.setValue("count", nCount);
							if (!ePersistenceCondition.evalAsBoolean())
							{
								for (j = 0; j < nCount; j++)
									bsProcessFlag.set(nStart + j, false);
							}
							odLast = null;
							nStart = -1;
							nCount = 0;
						}
					}
					
					if (od != null && bsProcessFlag.get(i))
					{
						if (nStart == -1)
							nStart = i;
						++nCount;
						odLast = od;
					}
				}
			}
			
			//输出结果
			for (i = 0; i < m_alObservationData.size(); i++)
			{
				ObservationData od = m_alObservationData.get(i);
				str = od.m_strNum + "," + sdf.format(od.m_dateTime);
				for (j = 0; j < od.m_strVar.length; j++)
					str += "," + od.m_strVar[j];
				str += "," + (bsProcessFlag.get(i) ? "1" : "0");
				m_llOutput.add(str);
				//System.out.println(m_llOutput.getLast());
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
	String m_strStartCondition = "";
	String m_strPersistenceCondition = "";
	
	Model m_modelCoeff = null;
	
	LinkedList<String> m_llOutput = new LinkedList<String>();
	//---------------------------------------------------------------------------
	class ObservationData
	{
		ObservationData(String strNum, Date dateTime, String[] strVar)
		{
			m_strNum = strNum;
			m_dateTime = dateTime;
			m_strVar = strVar;
		}
		
		String m_strNum;
		Date m_dateTime;
		String[] m_strVar;
	}
	ArrayList<ObservationData> m_alObservationData = new ArrayList<ObservationData>();
}
