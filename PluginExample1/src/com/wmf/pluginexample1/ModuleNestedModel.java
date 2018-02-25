//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleNestedModel.java
// \brief 嵌套模型模块类
// \author 王庆飞
// \date 2016-10-24
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.wmf.model.*;

public class ModuleNestedModel extends Module 
{
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
			mp.m_portNested = m_portNested;
			return mp;
		}
		
		Port m_portNested = null;
	}
	
	public ModuleNestedModel(int nID) 
	{
		super(nID);
		
		m_modelNested = null;
		m_strName = "嵌套模型"; 
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return m_strName;
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
			Port portToNested = ((MyPort)portTo).m_portNested;
			return portToNested.GetModule().OnAttach(portFrom, portToNested);
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
			
			alParam.add(new Param("Model", m_modelNested != null ? m_modelNested.ToXML() : "", "模型", "", "", Param.EditType.File, "模型文件(*.xml)|*.xml"));
			alParam.add(new Param("Name", m_strName, "名称", "", "", Param.EditType.MultilineText));
		
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
			m_modelNested = Model.FromXML((String)alParam.get(i++).m_objValue, m_model.m_strFileName);
			m_strName = (String)alParam.get(i++).m_objValue;
			
			Update();
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
			return m_modelNested;
		}
		else 
		{
			if (strName.startsWith("NestedInputPort") || strName.startsWith("NestedOutputPort"))
			{
				boolean bInput = strName.startsWith("NestedInputPort");
				ArrayList<Port> al = bInput ? m_alInputPort : m_alOutputPort;
				int i = Integer.parseInt(strName.substring(bInput ? 15 : 16));
				if (i < 0 || i >= al.size())
					return null;
				MyPort mp = (MyPort)al.get(i);
				return mp.m_portNested;
			}
		}
		return null;
	}
	public boolean SetParam(String strName, Object objValue)
	{
		try
		{
			if (strName.equalsIgnoreCase("ModelFileName"))
			{
				m_modelNested = Model.Load((String)objValue);
				if (m_modelNested == null)
					return false;
				m_strName = new File(m_modelNested.m_strFileName).getName();
				int nEnd = m_strName.lastIndexOf('.');
				if (nEnd != -1)
					m_strName = m_strName.substring(0, nEnd);
				
				Update();
				
				return true;
			}
			else if (strName.equalsIgnoreCase("Model"))
			{
				m_modelNested = Model.FromXML(((Model)objValue).ToXML(), m_model.m_strFileName);
				if (m_modelNested == null)
					return false;				
				Update();
				return true;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
	
	private void Update()
	{
		int j = 0;
		ArrayList<Port> alInputNested = new ArrayList<Port>(), alOutputNested = new ArrayList<Port>(); 
		if (m_modelNested != null)
		{
			LinkedList<Module> llModule = m_modelNested.GetModules();
			Iterator<Module> it = llModule.iterator();
			while (it.hasNext())
			{
				Module module = it.next();
				ArrayList<Port> alInputPort = module.GetInputPorts();
				for (j = 0; j < alInputPort.size(); j++)
				{
					Port port = alInputPort.get(j);
					if (port.GetAttachedPipeList().size() > 0)
						continue;
					if (j > 0 && port.GetType() == Port.Type.Unknown)
						continue;
					alInputNested.add(port);
				}
				ArrayList<Port> alOutputPort = module.GetOutputPorts();
				for (j = 0; j < alOutputPort.size(); j++)
				{
					Port port = alOutputPort.get(j);
					if (port.GetAttachedPipeList().size() > 0)
						continue;
					alOutputNested.add(port);
				}
			}
			
			m_modelNested.m_moduleContainer = this;
		}
		
		Update(alInputNested, true);
		Update(alOutputNested, false);
	}
	private void Update(ArrayList<Port> alNested, boolean bInput)
	{
		ArrayList<Port> al = bInput ? m_alInputPort : m_alOutputPort;
		int i = 0;
		if (al.size() < alNested.size())
		{
			for (i = al.size(); i < alNested.size(); i++)
				InsertPort(-1, new MyPort(this), bInput);
		}
		else if (al.size() > alNested.size())
		{
			for (i = al.size() - 1; i >= alNested.size(); i--)
				RemovePort(i, bInput);
		}
		for (i = 0; i < al.size(); i++)
		{
			MyPort mp = (MyPort)al.get(i);
			Port portNested = alNested.get(i);
			mp.SetModule(this);
			mp.SetType(portNested.GetType());
			mp.SetName(String.format("%s-%d", portNested.GetModule().GetNestedName(), portNested.GetModule().GetID()) + "|" + portNested.GetName());
			mp.m_portNested = portNested;
		}
	}
	
	public boolean Execute()
	{
		if (m_modelNested == null)
		{
			m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"模型\"参数为null。", GetNestedName(), GetID()));
			return false;
		}
		
		m_modelNested.m_dateStart = m_model.m_dateStart;
		m_modelNested.m_dateDataStart = m_model.m_dateDataStart;
		m_modelNested.m_dateDataEnd = m_model.m_dateDataEnd;
		
		Set<ModelListener> setListener = m_model.GetListeners();
		Iterator<ModelListener> it = setListener.iterator();  
		while (it.hasNext())
			m_modelNested.AddListener(it.next());
		
		boolean bResult = false;
		try
		{
			m_modelNested.SetCommand(m_model.GetCommand());
			bResult = m_modelNested.Execute();
			if (bResult)
			{
				OnParamChanged(); //嵌套模型执行后，输出参数可能改变
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		it = setListener.iterator();
		while (it.hasNext())
			m_modelNested.RemoveListener(it.next());
		
		return bResult;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		Port portNested = ((MyPort)port).m_portNested;
		return portNested.GetModule().GetOutputParam(portNested, alParam);
	}
	
	public void Destroy()
	{
		if (m_modelNested != null)
			m_modelNested.Destroy();
	}
	
	protected Model m_modelNested;
	protected String m_strName;
}
