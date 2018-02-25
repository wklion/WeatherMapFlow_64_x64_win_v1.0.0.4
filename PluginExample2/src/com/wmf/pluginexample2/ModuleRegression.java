//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleRegression.java
// \brief 回归分析模块类
// \author 王庆飞
// \date 2017-5-27
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

import com.wmf.model.*;
import com.weathermap.objects.*;

import org.codehaus.jettison.json.JSONObject;

public class ModuleRegression extends ModuleBase
{
	public ModuleRegression(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.Unknown));
    }

	public String GetGroupName()
	{
		return "统计分析";
	}
	public String GetName()
	{
		return "回归分析";
	}
	public String GetDescription()
	{
		return "输入格式支持JDBC.ResultSet或CSV格式。";
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
			if (alParam.get(0).m_objValue instanceof ResultSet)
				m_rs = (ResultSet)alParam.get(0).m_objValue;
			else if (alParam.get(0).m_objValue instanceof List<?>)
				m_list = (List<String>)alParam.get(0).m_objValue;
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
        	
			alParam.add(new Param("Method", m_strMethod, "方法", "", "参数", Param.EditType.Select, "Linear|Exp|Log|Pow"));
			alParam.add(new Param("VarNames", m_strVarNames, "变量名", "空格分隔，最后一个为因变量，其他为自变量。", "参数"));
			
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
			m_strMethod = (String)alParam.get(i++).m_objValue;
			m_strVarNames = (String)alParam.get(i++).m_objValue;
			
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
			if (m_rs == null && m_list == null)
				return false;
			String[] strVarNames = m_strVarNames.split(" ");
			if (strVarNames.length < 2)
				return false;
			
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			
			Analyst analyst = Analyst.CreateInstance("Regression", ws);
			analyst.SetPropertyValue("Method", m_strMethod);
			
			int i = 0, j = 0;
			String str = "";
			
			int[] nVarIndex = new int[strVarNames.length];
			for (i = 0; i < nVarIndex.length; i++)
				nVarIndex[i] = -1;
			ResultSetMetaData rsmd = m_rs != null ? m_rs.getMetaData() : null;
			String[] strs = m_list != null ? m_list.get(0).split(",") : null;
			for (i = 0; i < (rsmd != null ? rsmd.getColumnCount() : strs.length); i++)
			{
				str = (rsmd != null ? rsmd.getColumnLabel(i + 1) : strs[i]);
				for (j = 0; j < strVarNames.length; j++)
				{
					if (strVarNames[j].equalsIgnoreCase(str))
					{
						nVarIndex[j] = i;
						break;
					}
				}
			}
			
			if (m_rs != null)
			{
				m_rs.beforeFirst();
				while (m_rs.next())
				{
					str = "";
					for (j = 0; j < nVarIndex.length; j++)
					{
						if (j > 0)
							str += " ";
						str += m_rs.getString(nVarIndex[j] + 1);
					}
					analyst.AddPropertyValue("SampleData", str);
				}	
			}
			else
			{
				for (i = 2; i < m_list.size(); i++)
	        	{
	        		strs = m_list.get(i).split(",");
	        		
	        		str = "";
	        		for (j = 0; j < nVarIndex.length; j++)
	        		{
	        			if (j > 0)
							str += " ";
						str += strs[nVarIndex[j]];
	        		}
	        		analyst.AddPropertyValue("SampleData", str);
	        	}	
			}
			
			boolean bResult = analyst.Execute();
			if (bResult)
			{
				str = analyst.GetPropertyValue("Output");
				
				JSONObject jsonObj = new JSONObject(str);
				m_model.OutputLog(Model.LogLevel.Info, String.format("\"%s-%d\"模块\"输出：", GetNestedName(), GetID()));
				
				strs = jsonObj.getString("Index").split(" ");
				str = "";
				for (i = 0; i < strs.length; i++)
				{
        			if (i > 0)
						str += " ";
					str += strVarNames[Integer.parseInt(strs[i])];
        		}
				m_model.OutputLog(Model.LogLevel.Info, "变量：" + str);
				
				m_model.OutputLog(Model.LogLevel.Info, "回归系数：" + jsonObj.getString("a"));
				m_model.OutputLog(Model.LogLevel.Info, "回归平方和：" + jsonObj.getString("SSR"));
				m_model.OutputLog(Model.LogLevel.Info, "残差平方和：" + jsonObj.getString("SSE"));
				m_model.OutputLog(Model.LogLevel.Info, "平均误差：" + jsonObj.getString("ME"));
				m_model.OutputLog(Model.LogLevel.Info, "平均绝对误差：" + jsonObj.getString("MAE"));
				m_model.OutputLog(Model.LogLevel.Info, "均方根误差：" + jsonObj.getString("RMSE"));
				m_model.OutputLog(Model.LogLevel.Info, "复相关系数：" + jsonObj.getString("R"));
				m_model.OutputLog(Model.LogLevel.Info, "决定系数：" + jsonObj.getString("R2"));
				m_model.OutputLog(Model.LogLevel.Info, "偏回归平方和：" + jsonObj.getString("SSPR"));
				m_model.OutputLog(Model.LogLevel.Info, "回归系数和方程F检验值：" + jsonObj.getString("F"));
				m_model.OutputLog(Model.LogLevel.Info, "偏相关系数：" + jsonObj.getString("PCC"));
				m_model.OutputLog(Model.LogLevel.Info, "回归系数t检验值：" + jsonObj.getString("t"));
			}
	        analyst.Destroy();
	        
			return bResult;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}

	ResultSet m_rs = null;
	List<String> m_list = null;
	
	String m_strMethod = "Linear";
	String m_strVarNames = "";
}
