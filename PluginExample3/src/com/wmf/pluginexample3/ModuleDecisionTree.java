//-------------------------------------------------------------
// \project PluginExample3
// \file ModuleDecisionTree.java
// \brief 决策树模块
// \author 王庆飞
// \date 2017-7-5
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample3;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.wmf.model.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ModuleDecisionTree extends Module 
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
	
	public ModuleDecisionTree(int nID) 
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
		return "决策树";
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
			
			alParam.add(new Param("FileName", m_strFileName, "决策树文件", "", "", Param.EditType.File, "决策树文件(*.xml)|*.xml|所有文件(*.*)|*.*"));
			
			alParam.add(new Param("StationNumField", m_strStationNumField, "区站号字段", "", "参数/字段"));
			alParam.add(new Param("XField", m_strXField, "x字段", "", "参数/字段"));
        	alParam.add(new Param("YField", m_strYField, "y字段", "", "参数/字段"));
						
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
			
			m_strFileName = (String)alParam.get(i++).m_objValue;
			
			m_strStationNumField = (String)alParam.get(i++).m_objValue;
			m_strXField = (String)alParam.get(i++).m_objValue;
			m_strYField = (String)alParam.get(i++).m_objValue;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	void LoadChildNode(Element e, Node node)
	{
		List<Element> listChild = e.elements();
		Iterator<Element> itChild = listChild.iterator();
		while (itChild.hasNext())
		{
			Element eChild = itChild.next();
			Node nodeChild = new Node(eChild.attributeValue("Variant"), Double.parseDouble(eChild.attributeValue("Value")));
			nodeChild.m_nodeParent = node;
			node.m_alChildNode.add(nodeChild);
			
			LoadChildNode(eChild, nodeChild);
		}
	}
	
	double Decide(double[] dValue, Node node)
	{
		if (node.m_alChildNode.size() == 0)
			return node.m_dValue;
		int i = 0, nVar = -1;
		Node nodeChild = node.m_alChildNode.get(0);
		for (i = 0; i < m_alVariant.size(); i++)
		{
			Variant var = m_alVariant.get(i);
			if (var.m_strName.equals(nodeChild.m_strVariant))
			{
				nVar = i;
				break;
			}
		}
		if (nVar == -1)
			throw new AssertionError(); 
		else if (nVar == m_alVariant.size() - 1)
			return nodeChild.m_dValue;
		
		for (i = 0; i < node.m_alChildNode.size(); i++)
		{
			nodeChild = node.m_alChildNode.get(i);
			if (nodeChild.m_dValue == dValue[nVar])
				return Decide(dValue, nodeChild);
		}
		
		return m_nodeRoot.m_dValue;
	}

	public boolean Execute()
	{
		try
		{
			int i = 0, j = 0, k = 0;
			String str;
			//--------------------------------------------------------------------------------------------------------------
			//加载决策树
			SAXReader saxr = new SAXReader();
			String strFileName = m_strFileName;
			if (!new File(strFileName).isAbsolute())
				strFileName = new File(m_model.m_strFileName).getParent() + "/" + m_strFileName;
			Document doc = saxr.read(new InputStreamReader(new FileInputStream(strFileName), "gb2312"));
			
			m_alVariant.clear();
			List<Element> listElement = doc.selectNodes("DecisionTree/Variants/Variant");
			Iterator<Element> it = listElement.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				Variant var = new Variant(e.attributeValue("Name"));
				
				List<Element> listGrade = e.elements("Grade");
				Iterator<Element> itGrade = listGrade.iterator();
				while (itGrade.hasNext())
				{
					Element eGrade = itGrade.next();
					Grade grade = new Grade(eGrade.attributeValue("Range"), Double.parseDouble(eGrade.attributeValue("Value")));
					var.m_alGrade.add(grade);
				}
				
				m_alVariant.add(var);
			}
			
			Element eRoot = (Element)doc.selectSingleNode("DecisionTree/Node");
			m_nodeRoot = new Node(eRoot.attributeValue("Variant"), Double.parseDouble(eRoot.attributeValue("Value")));
			LoadChildNode(eRoot, m_nodeRoot);
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
				int[] nVarIndex = new int[m_alVariant.size() - 1];
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
						for (k = 0; k < m_alVariant.size() - 1; k++)
						{
							if (m_alVariant.get(k).m_strName.equalsIgnoreCase(str))
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
			//决策
			m_llOutput.clear();
			m_llOutput.add(String.format("%s,%s,%s,%s", m_strStationNumField, m_strXField, m_strYField, m_alVariant.get(m_alVariant.size() - 1).m_strName));
			m_llOutput.add("VARCHAR,DOUBLE,DOUBLE,DOUBLE");
			for (i = 0; i < alStationData.size(); i++)
			{
				StationData sd = alStationData.get(i);
				double[] dValue = new double[sd.m_dVar.length];
				for (j = 0; j < dValue.length; j++)
				{
					double d = m_nodeRoot.m_dValue;
					Variant var = m_alVariant.get(j);
					for (k = 0; k < var.m_alGrade.size(); k++)
					{
						Grade grade = var.m_alGrade.get(k);
						if (grade.IsInRange(sd.m_dVar[j]))
						{
							d = grade.m_dValue;
							break;
						}
					}
					dValue[j] = d;
				}
				m_llOutput.add(String.format("%s,%f,%f,%f", sd.m_strNum, sd.m_x, sd.m_y, Decide(dValue, m_nodeRoot)));
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
	//---------------------------------------------------------------------------
	protected String m_strFileName;
	
	String m_strStationNumField = "StationNum";
	String m_strXField = "x", m_strYField = "y";
	
	LinkedList<String> m_llOutput = new LinkedList<String>();
	//---------------------------------------------------------------------------
	class Grade
	{	
		static final int LEFT_CLOSED = 0x01;
		static final int RIGHT_CLOSED = 0x02;
		
		Grade(String strRange, double dValue) throws Exception
		{
			m_nFlag = 0;
			m_dLower = Double.MIN_VALUE;
			m_dUpper = Double.MAX_VALUE;
			if (!strRange.isEmpty())
			{
				char ch = strRange.charAt(0);
				if (ch != '(' && ch != '[')
					throw new ParseException("区间必须以\'(\'或\'[\'开始", -1);
				if (ch == '[')
					m_nFlag |= LEFT_CLOSED;
				ch = strRange.charAt(strRange.length() - 1);
				if (ch != ')' && ch != ']')
					throw new ParseException("区间必须以\')\'或\']\'结束", -1);
				if (ch == ']')
					m_nFlag |= RIGHT_CLOSED;
				int i = strRange.indexOf(',');
				if (i == -1)
					throw new ParseException("区间必须包含\',\'", -1);
				
				String str = strRange.substring(1, i);
				if (!str.equals("*"))
					m_dLower = Double.parseDouble(strRange.substring(1, i));
				str = strRange.substring(i + 1, strRange.length() - 1);
				if (!str.equals("*"))
					m_dUpper = Double.parseDouble(str);
			}

			m_dValue = dValue;
		}
		boolean IsInRange(double dVar)
		{
			if (dVar < m_dLower || ((m_nFlag & LEFT_CLOSED) == 0 && dVar == m_dLower))
				return false;
			if (dVar > m_dUpper || ((m_nFlag & RIGHT_CLOSED) == 0 && dVar == m_dUpper))
				return false;
			return true;
		}
		
		int m_nFlag;
		double m_dLower, m_dUpper;
		
		double m_dValue;
	}
	class Variant
	{	
		Variant(String strName)
		{
			m_strName = strName;
		}
		
		String m_strName;
		ArrayList<Grade> m_alGrade = new ArrayList<Grade>();
	}
	ArrayList<Variant> m_alVariant = new ArrayList<Variant>();
	class Node
	{	
		Node(String strVariant, double dValue)
		{
			m_strVariant = strVariant;
			m_dValue = dValue;
		}
		
		String m_strVariant;
		double m_dValue;
		
		Node m_nodeParent = null;
		ArrayList<Node> m_alChildNode = new ArrayList<Node>();
	}
	Node m_nodeRoot = null;
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
