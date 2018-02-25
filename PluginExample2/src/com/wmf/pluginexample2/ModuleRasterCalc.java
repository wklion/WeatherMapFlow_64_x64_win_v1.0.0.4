//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleRasterCalc.java
// \brief RasterCalc模块类
// \author 王庆飞
// \date 2016-7-18
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;

import com.wmf.model.*;
import com.weathermap.objects.*;

/**
 * 如果输入端或输出端可动态增删端口，需要派生端口类，否则Undo可能出错。
 */
class VariablePort extends Port
{
	public VariablePort(Module module)
	{
		super(module);
    }
	public VariablePort(Module module, Type eType)
	{
		super(module, eType);
    }
	public Port Clone()
	{
		VariablePort vp = new VariablePort(null, m_eType);
		vp.m_strName = m_strName;
		vp.m_strVarName = m_strVarName;
		vp.m_dVarValue = m_dVarValue;
		vp.m_strDS = m_strDS;
		vp.m_strDataset = m_strDataset;
		return vp;
	}
	
	String m_strVarName = "";
	double m_dVarValue = -9999; //支持指定数值
	String m_strDS = "";
	String m_strDataset = "";
}

public class ModuleRasterCalc extends ModuleBase
{
	public ModuleRasterCalc(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new VariablePort(this, Port.Type.DatasetRaster));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRaster));
		
		m_alExpressionItem = new ArrayList<ExpressionItem>();
		m_alExpressionItem.add(new ExpressionItem());
		
		m_strDSOutput = "";
		m_strDatasetOutput = String.format("RasterCalc%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "栅格\n代数运算";
	}
	public String GetDescription()
	{
		return "";
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
			VariablePort vp = (VariablePort)portTo;
			vp.m_strDS = (String)alParam.get(0).m_objValue;
			vp.m_strDataset = (String)alParam.get(1).m_objValue;			
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
			VariablePort vp = (VariablePort)portTo;
			vp.m_strDS = "";
			vp.m_strDataset = "";
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
			Model2 model = ((Model2)m_model);
        	String strDatasources = model.GetWritableDatasources(true);
        	if (m_strDSOutput.isEmpty() && !strDatasources.isEmpty())
        		m_strDSOutput = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
        	String strOptionalValues = "";
        	for (int i = 'a'; i <= 'z'; i++)
        	{
        		if (!strOptionalValues.isEmpty())
        			strOptionalValues += "|";
        		strOptionalValues += String.format("%c", i);
        	}
        	alParam.add(new Param("ExpressionItemCount", String.format("%d", m_alExpressionItem.size()), "算术表达式个数", "", "参数", Param.EditType.ItemCount, "2")); //"2":ExpressionItem参数个数
			for (int i = 0; i < m_alExpressionItem.size(); i++)
			{
				ExpressionItem ei = m_alExpressionItem.get(i);
				String strGroups = String.format("参数/第%d个算术表达式", i + 1);
				alParam.add(new Param("Expression", ei.m_strExpression, "算术表达式", "", strGroups, Param.EditType.Expression, strOptionalValues));  
				alParam.add(new Param("Condition", ei.m_strCondition, "条件", "", strGroups, Param.EditType.Expression, strOptionalValues));
			}
        	
			for (int i = 0; i < m_alInputPort.size(); i++)
        	{
        		VariablePort vp = (VariablePort)m_alInputPort.get(i);
				alParam.add(new Param(vp.m_strVarName, String.format("%f", vp.m_dVarValue), vp.m_strVarName, "", "参数/变量", Param.EditType.Float));
        	}
			
			for (int i = 0; i < m_alInputPort.size(); i++)
        	{
        		VariablePort vp = (VariablePort)m_alInputPort.get(i);
        		alParam.add(new Param(Param.Datasource, vp.m_strDS, "数据源", "数据源别名", "输入端/" + vp.m_strVarName, Param.EditType.ReadOnly));
				alParam.add(new Param(Param.Dataset, vp.m_strDataset, "数据集", "数据集名称", "输入端/" + vp.m_strVarName, Param.EditType.ReadOnly));
        	}
			
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
		if (alParam.get(nIndex).m_strName.equals("ExpressionItemCount"))
		{
			int i = 0, j = 0, nOldItemCount = Integer.parseInt((String)alParam.get(nIndex).m_objValue), nNewItemCount = Integer.parseInt((String)objValue);
			if (nOldItemCount < nNewItemCount)
			{
				String strOptionalValues = "";
	        	for (i = 'a'; i <= 'z'; i++)
	        	{
	        		if (!strOptionalValues.isEmpty())
	        			strOptionalValues += "|";
	        		strOptionalValues += String.format("%c", i);
	        	}
	        	
	        	j = nIndex + 1 + nOldItemCount * 2;
				for (i = nOldItemCount; i < nNewItemCount; i++)
				{
					String strGroups = String.format("参数/第%d个算术表达式", i + 1);
					alParam.add(j++, new Param("Expression", "", "算术表达式", "", strGroups, Param.EditType.Expression, strOptionalValues));  
					alParam.add(j++, new Param("Condition", "", "条件", "", strGroups, Param.EditType.Expression, strOptionalValues));
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
			final int nExpressionItemCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_alExpressionItem.clear();
			
			int j = 0, k = 0, l = 0;
			ArrayList<String> alVar = new ArrayList<String>(); 
			Model2 model = (Model2)m_model;
			Analyst analyst = Analyst.CreateInstance("RasterCalc", model.m_ws);
			while (m_alExpressionItem.size() < nExpressionItemCount)
			{
				ExpressionItem ei = new ExpressionItem();
				ei.m_strExpression = (String)alParam.get(i++).m_objValue;
				ei.m_strCondition = (String)alParam.get(i++).m_objValue;
				m_alExpressionItem.add(ei);
				
				for (j = 0; j < 2; j++)
				{
					String str = (j == 0 ? ei.m_strExpression : ei.m_strCondition);
					if (str.isEmpty())
						continue;
					str = str.replace("[DataStartTime]", String.format("%d", m_model.m_dateDataStart.getTime()));
					str = str.replace("[DataEndTime]", String.format("%d", m_model.m_dateDataEnd.getTime()));
					if (!analyst.SetPropertyValue("Expression", str))
						continue;
					String[] strs = str.split("\\[|\\]");
					for (k = 1; k < strs.length; k += 2)
					{
						for (l = 0; l < alVar.size(); l++)
						{
							if (strs[k].equals(alVar.get(l)))
								break;
						}
						if (l == alVar.size())
							alVar.add(strs[k]);
					}
				}
			}
			analyst.Destroy();
			
			for (j = m_alInputPort.size(); j < alVar.size(); j++)
				InsertPort(-1, new VariablePort(this, Port.Type.DatasetRaster), true);
			for (j = 0; j < alVar.size(); j++)
			{
				VariablePort vp = (VariablePort)m_alInputPort.get(j);
				vp.m_strVarName = alVar.get(j);
				if (i < alParam.size())
					vp.m_dVarValue = Float.parseFloat((String)alParam.get(i++).m_objValue);
				vp.SetName(vp.m_strVarName);
			}
			for (j = m_alInputPort.size() - 1; j >= 1 && j >= alVar.size(); j--)
				RemovePort(j, true);
			
			for (j = 0; j < m_alInputPort.size(); j++)
			{
				VariablePort vp = (VariablePort)m_alInputPort.get(j);
				if (i < alParam.size() - 2)
				{
					vp.m_strDS = (String)alParam.get(i++).m_objValue;
					vp.m_strDataset = (String)alParam.get(i++).m_objValue;
				}
				else
				{
					vp.m_strDS = "";
					vp.m_strDataset = "";
				}
			}
			
			i = alParam.size() - 2;
			m_strDSOutput = (String)alParam.get(i++).m_objValue;
			m_strDatasetOutput = (String)alParam.get(i++).m_objValue;
			
			m_alOutputPort.get(0).SetName(m_strDatasetOutput + "@" + m_strDSOutput);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public boolean Execute()
	{
		Model2 model = ((Model2)m_model);
		int i = 0, j = 0;
		for (i = 0; i < m_alExpressionItem.size(); i++)
		{
			ExpressionItem ei = m_alExpressionItem.get(i);
			if (ei.m_strExpression.isEmpty())
			{
				model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"第%d个算术表达式\"参数为空。", GetNestedName(), GetID(), i));
				return false;
			}		
		}
		
		Workspace ws = model.m_ws;
		
		Analyst analyst = Analyst.CreateInstance("RasterCalc", ws);
		
		for (i = 0; i < m_alExpressionItem.size(); i++)
		{
			ExpressionItem ei = m_alExpressionItem.get(i);
			//替换变量为数值
			String strExpression = ei.m_strExpression;
			strExpression = strExpression.replace("[DataStartTime]", String.format("%d", m_model.m_dateDataStart.getTime()));
			strExpression = strExpression.replace("[DataEndTime]", String.format("%d", m_model.m_dateDataEnd.getTime()));
			
			String strCondition = ei.m_strCondition;
			if (!strCondition.isEmpty())
			{
				strCondition = strCondition.replace("[DataStartTime]", String.format("%d", m_model.m_dateDataStart.getTime()));
				strCondition = strCondition.replace("[DataEndTime]", String.format("%d", m_model.m_dateDataEnd.getTime()));
			}
			for (j = 0; j < m_alInputPort.size(); j++)
			{
				VariablePort vp = (VariablePort)m_alInputPort.get(j);
				if (vp.m_strVarName.isEmpty())
	    			throw new AssertionError();
				if (vp.m_strDS.isEmpty())
				{
					strExpression = strExpression.replace("[" + vp.m_strVarName + "]", String.format("%f", vp.m_dVarValue));
					if (!strCondition.isEmpty())
						strCondition = strCondition.replace("[" + vp.m_strVarName + "]", String.format("%f", vp.m_dVarValue));
				}
			}
			
			if (!analyst.AddPropertyValue("ExpressionItem", "{\"Expression\":\"" + strExpression + "\",\"Condition\":\"" + strCondition + "\"}"))
	        {
	        	model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"第%d个算术表达式\"参数不正确。", GetNestedName(), GetID(), i));
	        	analyst.Destroy();
	        	return false;
	        }
		}
        
		String str;
		for (i = 0; i < m_alInputPort.size(); i++)
		{
			VariablePort vp = (VariablePort)m_alInputPort.get(i);
			if (vp.m_strVarName.isEmpty())
    			throw new AssertionError();
			if (!vp.m_strDS.isEmpty())
			{
				str = "{\"Datasource\":\"" + vp.m_strDS + "\",\"Dataset\":\"" + vp.m_strDataset + "\"}";
		        analyst.SetPropertyValue(vp.m_strVarName, str);
			}
		}

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
		
	class ExpressionItem
	{
		ExpressionItem()
		{
			m_strExpression = "";
			m_strCondition = "";
		}
		
		String m_strExpression;
		String m_strCondition;
	}
	ArrayList<ExpressionItem> m_alExpressionItem;
	
	String m_strDSOutput;
	String m_strDatasetOutput;
}
