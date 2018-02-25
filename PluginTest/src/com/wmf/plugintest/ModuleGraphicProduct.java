//-------------------------------------------------------------
// \project PluginTest
// \file ModuleGraphicProduct.java
// \brief GraphicProduct模块类
// \author 王庆飞
// \date 2016-5-16
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.plugintest;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

import com.wmf.model.*;
import com.wmf.model.Port.Type;
import com.weathermap.objects.*;
import com.wmf.pluginexample2.Model2;

public class ModuleGraphicProduct extends com.wmf.pluginexample2.ModuleGraphicProduct
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
			mp.m_strName = m_strName;
			return mp;
		}		
	}
	
	public ModuleGraphicProduct(int nID)
	{
		super(nID);
		
    }
	
	public String GetGroupName()
	{
		return "测试";
	}
	public String GetName()
	{
		return "图形产品\n制作2";
	}
	
	public boolean OnAttach(Port portFrom, Port portTo)
	{
		int i = FindPort(portFrom, false);
		if (i != -1)
			return true;
		i = FindPort(portTo, true);
		if (i == -1)
			return false;
		if (portTo instanceof MyPort)
		{
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() < 1)
				return false;
			if (alParam.get(0).m_objValue != null && !(alParam.get(0).m_objValue instanceof Map))
				return false;
			m_map = (Map<String, String>)alParam.get(0).m_objValue; 
			return true;
		}
		
		return super.OnAttach(portFrom, portTo);
	}
	
	public int SetParam(final ArrayList<Param> alParam)
	{
		int i = super.SetParam(alParam);
		
//		if (!(m_alInputPort.get(m_alInputPort.size() - 1) instanceof MyPort))
//			m_alInputPort.add(new MyPort(this));
		
		return i;
	}
	
	public boolean OnBeforeExecute()
	{
		try
		{
			Model2 model = ((Model2)m_model);
			//-------------------------------------------------------
			Date dateStart = model.m_dateStart;
			Date dateDataStart = model.m_dateDataStart;
			Date dateDataEnd = model.m_dateDataEnd;
			//-------------------------------------------------------
			Calendar c = new GregorianCalendar();
			c.setTime(model.m_dateStart);
			c.add(Calendar.HOUR, 8);
			model.m_dateStart = c.getTime();
			
			c.setTime(model.m_dateDataStart);
			c.add(Calendar.HOUR, 8);
			model.m_dateDataStart = c.getTime();
			
			c.setTime(model.m_dateDataEnd);
			c.add(Calendar.HOUR, 8);
			model.m_dateDataEnd = c.getTime();
			
			super.OnBeforeExecute();
			//-------------------------------------------------------
			model.m_dateStart = dateStart;
			model.m_dateDataStart = dateDataStart;
			model.m_dateDataEnd = dateDataEnd;
			
//			boolean bResult = super.OnBeforeExecute();
//			
//			Model2 model = ((Model2)m_model);
//			Workspace ws = model.m_ws;
//			
//			Layout layout = ws.GetLayout(0);
//			DatasetVector dvLayout = (DatasetVector)layout.GetDatasource().GetDataset(0);
//	        Recordset rsLayout = dvLayout.Query(null, null);
//	        
//	        int i = 0;
//	        Iterator<Map.Entry<String, String>> it = m_map.entrySet().iterator();
//			while (it.hasNext())
//			{
//				Map.Entry<String, String> e = it.next();
//				System.out.println(e.getValue());
//				GeoText gt = new GeoText(e.getValue());
//				gt.SetOrigin(5.0, 5.0 + i * 5.0);
//		        gt.SetPropertyValue("TextStyle", "{\"FontName\":\"宋体\",\"Alignment\":\"TopLeft\",\"ForeColor\":\"RGB(255,0,0)\",\"FontSize\":6.0}");
//		        rsLayout.AddNew(gt);
//		        rsLayout.Update();
//				gt.Destroy();
//				i++;
//			}
//	        
//	        rsLayout.Destroy();
		
//			return bResult;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return true;
	}
	
	public boolean Execute()
	{
		try
		{
			boolean bResult = super.Execute();
			
			return bResult;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	Map<String, String> m_map;
}
