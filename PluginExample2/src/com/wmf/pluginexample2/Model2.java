//-------------------------------------------------------------
// \project PluginExample2
// \file Model2.java
// \brief 模型类2
// \author 王庆飞
// \date 2016-4-26
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import com.wmf.model.*;
import com.weathermap.objects.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * 如果输入端或输出端可动态增删端口，需要派生端口类，否则Undo可能出错。
 */
class DatasetPort extends Port
{
	public DatasetPort(Module module)
	{
		super(module);
    }
	public DatasetPort(Module module, Type eType)
	{
		super(module, eType);
    }
	public Port Clone()
	{
		DatasetPort dp = new DatasetPort(null, m_eType);
		dp.m_strName = m_strName;
		dp.m_strDS = m_strDS;
		dp.m_strDataset = m_strDataset;
		return dp;
	}
	
	String m_strDS = "";
	String m_strDataset = "";
}

public class Model2 extends Model
{
	public Model2() 
	{
		m_ws = new Workspace();
    }
	public void Destroy()
	{
		m_ws.Destroy();
		super.Destroy();
	}
	
	public Object GetParam(String strName)
	{
		if (strName.equalsIgnoreCase("Workspace"))
		{
			return m_ws;
		}
		return null;
	}
	
	public String GetWritableDatasources(boolean bRaster)
	{
		String str = "";
		Iterator<Module> it = m_llModule.iterator();
		while (it.hasNext())
		{
			Module module = it.next();
			if (module instanceof ModuleCreateDS)
			{
				ModuleCreateDS mcds = (ModuleCreateDS)module;
				if (bRaster)
				{
					if (mcds.m_strType.equalsIgnoreCase("ESRI Shapefile"))
						continue;
				}
				else
				{
					if (mcds.m_strType.equalsIgnoreCase("GTiff") || mcds.m_strType.equalsIgnoreCase("grib_api"))
						continue;
				}
				
				if (!str.isEmpty())
					str += "|";
				str += mcds.m_strAlias;
			}
		}
		return str;
	}
	public String GetFields(String strDS, String strDataset, boolean bIncludeText)
	{
		try
		{
			Datasource ds = strDS.isEmpty() ? null : m_ws.GetDatasource(strDS);
			if (ds == null)
				return "";
			DatasetVector dv = (DatasetVector)ds.GetDataset(strDataset);
			if (dv == null)
				return "";
			String strFields = dv.GetFields();
			if (strFields == null)
				return "";
			String strResult = "";
			JSONArray ja = new JSONArray(strFields); 
			for (int j = 0; j < ja.length(); j++) 
			{
				JSONObject jo = ja.getJSONObject(j);
				if (!bIncludeText && jo.getString("Type").equalsIgnoreCase("Text"))
					continue;
				if (!strResult.isEmpty())
					strResult += "|";
				strResult += jo.getString("Name");
			}
			return strResult;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return "";
	}
	public boolean CheckInput(Module module, int nPort, String strDS, String strDataset, boolean bOptional)
	{
		try
		{
			String strWho = String.format("\"%s-%d\"模块的第%d个输入端口", module.GetNestedName(), module.GetID(), nPort); 
			if (strDS.isEmpty())
			{
				if (bOptional)
				{
					return true;
				}
				else
				{
					OutputLog(LogLevel.Error, strWho + "数据源别名为空。"); 
					return false;
				}
			}
			if (strDataset.isEmpty())
			{
				if (bOptional)
				{
					return true;
				}
				else
				{
					OutputLog(LogLevel.Error, strWho + "数据集名称为空。");
					return false;
				}
			}
			Datasource ds = m_ws.GetDatasource(strDS);
			if (ds == null)
			{
				if (bOptional)
				{
					return true;
				}
				else
				{
					OutputLog(LogLevel.Error, strWho + String.format("未找到数据源\"%s\"。", strDS)); 
					return false;
				}
			}
			Dataset dataset = ds.GetDataset(strDataset);
			if (dataset == null)
			{
				if (bOptional)
				{
					return true;
				}
				else
				{
					OutputLog(LogLevel.Error, strWho + String.format("未找到数据集\"%s\"。", strDataset));
					return false;
				}
			}
			ArrayList<Port> alPort = module.GetInputPorts();
			if (nPort < 0 || nPort >= alPort.size())
				throw new AssertionError();
			if (alPort.get(nPort).GetType() != Port.Type.valueOf("Dataset" + dataset.GetType()))
			{
				OutputLog(LogLevel.Error, strWho + String.format("端口类型和数据集\"%s\"类型不一致。", strDataset));
				return false;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean OnAfterExecute()
	{
		Iterator<Module> it = m_llModule.iterator();
		while (it.hasNext())
		{
			Module module = it.next();
			if (module instanceof ModuleCreateDS)
			{
				ModuleCreateDS mcds = (ModuleCreateDS)module; 
				m_ws.CloseDatasource(mcds.m_strAlias);
			}
		}
		return super.OnAfterExecute();
	}
	
	public boolean Check(Model2 model2, HashMap<String, Module> hmModule, ArrayList<String> alSingle,
			HashMap<String, Port> hmPort)
	{
		HashMap<String, Module> hmModuleThis = new HashMap<String, Module>();
		ArrayList<String> alSingleThis = new ArrayList<String>(); //单数据集
		HashMap<String, Port> hmPortThis = new HashMap<String, Port>();
		
		String str = "";
		int j = 0, k = 0;
		Iterator<Module> it = model2.m_llModule.iterator();
		while (it.hasNext())
		{
			Module m = it.next();
			str = "";
			if ((m instanceof ModuleCreateDS) || (m instanceof ModuleOpenDS))
			{
				if (m instanceof ModuleCreateDS)
					str = ((ModuleCreateDS)m).m_strAlias;
				else if (m instanceof ModuleOpenDS)
					str = ((ModuleOpenDS)m).m_strAlias;
				if (str.isEmpty())
				{
					OutputLog(LogLevel.Error, String.format("\"%s-%d\"模块数据源别名为空。", 
							m.GetNestedName(), m.GetID()));
					return false;
				}
				if (hmModule.containsKey(str))
				{
					Module mLast = hmModule.get(str);
					OutputLog(LogLevel.Error, String.format("\"%s-%d\"模块和\"%s-%d\"模块数据源重名。",
							mLast.GetNestedName(), mLast.GetID(), m.GetNestedName(), m.GetID()));
					return false;
				}
				else
				{
					hmModule.put(str, m);
				}
				
				if (m instanceof ModuleCreateDS)
				{
					hmModuleThis.put(str, m);
					ModuleCreateDS mcds = (ModuleCreateDS)m;
					if (mcds.m_strType.equalsIgnoreCase("ESRI Shapefile") || mcds.m_strType.equalsIgnoreCase("GTiff"))
					{
						alSingle.add(str);
						alSingleThis.add(str);
					}
				}
			}
			else
			{
				Model modelNested = (Model)m.GetParam("Model");
				if (modelNested != null)
				{
					if (modelNested instanceof Model2)
					{
						if (!Check((Model2)modelNested, hmModule, alSingle, hmPort))
							return false;
					}
				}
				else
				{
					ArrayList<Port> alPort = m.GetOutputPorts();
					for (j = 0; j < alPort.size(); j++)
					{
						if (alPort.get(j).GetType() == Port.Type.CSV)
							continue;
						str = "";
						ArrayList<Param> alParam = new ArrayList<Param>();
						m.GetOutputParam(alPort.get(j), alParam);
						k = alSingle.size();
						if (alParam.size() == 2 && alParam.get(0).m_strName.equalsIgnoreCase(Param.Datasource) && 
								alParam.get(1).m_strName.equalsIgnoreCase(Param.Dataset))
						{
							if (((String)alParam.get(0).m_objValue).isEmpty())
							{
								OutputLog(LogLevel.Error, String.format("\"%s-%d\"模块的第%d个输出端口数据源别名为空。", 
										m.GetNestedName(), m.GetID(), j));
								return false;
							}
							if (((String)alParam.get(1).m_objValue).isEmpty())
							{
								OutputLog(LogLevel.Error, String.format("\"%s-%d\"模块的第%d个输出端口数据集名称为空。", 
										m.GetNestedName(), m.GetID(), j));
								return false;
							}
							for (k = 0; k < alSingle.size(); k++)
							{
								if (alSingle.get(k).equalsIgnoreCase((String)alParam.get(0).m_objValue))
								{
									str = (String)alParam.get(0).m_objValue;
									break;
								}
							}
						}				
						if (str.isEmpty())
							str = alPort.get(j).GetName();
						if (str.isEmpty())
						{
							if (alPort.get(j).GetType() == Port.Type.Unknown)
								continue;
							else
							{
								OutputLog(LogLevel.Error, String.format("\"%s-%d\"模块的第%d个输出端口名称为空。", 
										m.GetNestedName(), m.GetID(), j));
								return false;
							}
						}
						if (hmPort.containsKey(str))
						{
							Port pLast = hmPort.get(str);
							Module mLast = pLast.GetModule();
							if (k < alSingle.size())
							{
								OutputLog(LogLevel.Error, String.format("\"%s\"数据源只能创建一个数据集，\"%s-%d\"模块的第%d个输出端口和\"%s-%d\"模块的第%d个输出端口数据源重名。", alSingle.get(k),
										mLast.GetNestedName(), mLast.GetID(), mLast.FindPort(pLast, false), 
										m.GetNestedName(), m.GetID(), j));
							}
							else
							{
								OutputLog(LogLevel.Error, String.format("\"%s-%d\"模块的第%d个输出端口和\"%s-%d\"模块的第%d个输出端口重名。", 
										mLast.GetNestedName(), mLast.GetID(), mLast.FindPort(pLast, false), 
										m.GetNestedName(), m.GetID(), j));
							}
							return false;
						}
						else
						{
							hmPort.put(str, alPort.get(j));
							hmPortThis.put(str, alPort.get(j));
						}
					}
				}
			}
		}
		
		Iterator<Map.Entry<String, Module>> itModule = hmModuleThis.entrySet().iterator();
		while (itModule.hasNext())
		{
			Map.Entry<String, Module> e = itModule.next();
			hmModule.remove(e.getKey());
		}
		for (j = 0; j < alSingleThis.size(); j++)
			alSingle.remove(alSingleThis.get(j));
		Iterator<Map.Entry<String, Port>> itPort = hmPortThis.entrySet().iterator();
		while (itPort.hasNext())
		{
			Map.Entry<String, Port> e = itPort.next();
			hmPort.remove(e.getKey());
		}
		
		return true;
	}
	
	public boolean Execute()
	{
		if (m_moduleContainer == null)
		{
			HashMap<String, Module> hmModule = new HashMap<String, Module>();
			ArrayList<String> alSingle = new ArrayList<String>(); //单数据集
			HashMap<String, Port> hmPort = new HashMap<String, Port>();
			if (!Check(this, hmModule, alSingle, hmPort))
				return false;
		}

		Workspace wsOld = m_ws;
		if (m_moduleContainer != null)
		{
			Model model = m_moduleContainer.GetModel();
			if (model == null)
				throw new AssertionError();
			Workspace ws = (Workspace)model.GetParam("Workspace");
			if (ws != null)
				m_ws = ws;
		}
		
		boolean bResult = super.Execute();
		
		m_ws = wsOld;
		return bResult;
	}
	
	public Workspace m_ws;
}
