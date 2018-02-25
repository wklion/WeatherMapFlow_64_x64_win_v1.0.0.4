//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleExportToGeoJSON.java
// \brief 导出到GeoJSON模块类
// \author 王庆飞
// \date 2016-11-1
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.wmf.model.*;
import com.weathermap.objects.*;
import java.awt.geom.Point2D;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ModuleExportToGeoJSON extends ModuleBase
{
	public ModuleExportToGeoJSON(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new Port(this, Port.Type.Unknown));
		m_alOutputPort.add(new Port(this, Port.Type.JSONFile));

		m_strDSInput = "";
		m_strDatasetInput = "";
		
		m_strCoordFormat = "%f";
		m_strFields = "";
		
		m_strFileName = "c:/[yyMMddHH].json";
		
		m_strOutput = "";
    }

	public String GetGroupName()
	{
		return "数据导出";
	}
	public String GetName()
	{
		return "导出到\nGeoJSON";
	}
	public String GetDescription()
	{
		return "点、线、面数据集导出到GeoJSON文件。";
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
			if (portFrom.GetType() != Port.Type.DatasetPoint && portFrom.GetType() != Port.Type.DatasetLine && portFrom.GetType() != Port.Type.DatasetRegion)
				return false;
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() < 2)
				return false;
			m_strDSInput = (String)alParam.get(0).m_objValue;
			m_strDatasetInput = (String)alParam.get(1).m_objValue;
			
			portTo.SetType(portFrom.GetType());
			portTo.SetName(alParam.get(1).m_objValue + "@" + alParam.get(0).m_objValue);	
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
			Model2 model = ((Model2)m_model);
			
			alParam.add(new Param(Param.Datasource, m_strDSInput, "数据源", "数据源别名", "输入端", Param.EditType.ReadOnly));
			alParam.add(new Param(Param.Dataset, m_strDatasetInput, "数据集", "数据集名称", "输入端", Param.EditType.ReadOnly));
			
			alParam.add(new Param("CoordFormat", m_strCoordFormat, "坐标格式", "", "参数"));
			alParam.add(new Param("Fields", m_strFields, "导出字段", "", "参数", Param.EditType.MultiSelect, model.GetFields(m_strDSInput, m_strDatasetInput, true)));
			
			alParam.add(new Param("FileName", m_strFileName, "文件名", "", "输出端", Param.EditType.File, "json文件(*.json)|*.json|所有文件(*.*)|*.*"));
			
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
			m_strDSInput = (String)alParam.get(i++).m_objValue;
			m_strDatasetInput = (String)alParam.get(i++).m_objValue;
			
			m_strCoordFormat = (String)alParam.get(i++).m_objValue;
			m_strFields = (String)alParam.get(i++).m_objValue;
			
			m_strFileName = (String)alParam.get(i++).m_objValue;
			
			m_alInputPort.get(0).SetName(m_strDatasetInput + "@" + m_strDSInput);
			m_alOutputPort.get(0).SetName(m_strFileName);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	void Output(StringBuffer sb, BufferedWriter bw, String str)
	{
		try
		{
			if (sb != null)
				sb.append(str);
			else
				bw.write(str);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	public boolean Execute()
	{
		try
		{
			Model2 model = ((Model2)m_model);
			if (!model.CheckInput(this, 0, m_strDSInput, m_strDatasetInput, false))
				return false;
			Workspace ws = model.m_ws;
			
			StringBuffer sb = null;
			BufferedWriter bw = null;
			if (m_strFileName.equalsIgnoreCase("GeoJSON"))
			{
				sb = new StringBuffer();
			}
			else
			{
				m_strOutput = m_strFileName.replace('\\', '/'); 
				m_strOutput = m_model.FormatDate(m_strOutput, false);
				if (!new File(m_strOutput).isAbsolute())
					m_strOutput = new File(m_model.m_strFileName).getParent().replace('\\', '/') + "/" + m_strOutput;
				
				File file = new File(m_strOutput).getParentFile();
				if (!file.exists())
					file.mkdirs();
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m_strOutput), "UTF-8"));
			}
			
			int h = 0, i = 0, j = 0;
			Datasource ds = ws.GetDatasource(m_strDSInput);
			DatasetVector dv = (DatasetVector)ds.GetDataset(m_strDatasetInput);
			
			JSONArray ja = new JSONArray(dv.GetFields());
			String[] strs = null;
			int[] arrIndex = null;
			if (!m_strFields.isEmpty())
			{
				strs = m_strFields.split("\\|");
				arrIndex = new int[strs.length];
				for (i = 0; i < ja.length(); i++) 
				{
					JSONObject jo = ja.getJSONObject(i);
					for (j = 0; j < strs.length; j++)
					{
						if (jo.getString("Name").equalsIgnoreCase(strs[j]))
						{	
							arrIndex[j] = i;
							break;
						}
					}
				}
			}
			
			Output(sb, bw, "{\"type\":\"FeatureCollection\",\"features\":[\n");
			
			Recordset rs = dv.Query("", null);
			rs.MoveFirst();
			while (!rs.IsEOF())
			{
				Geometry g = rs.GetGeometry();
				if (h > 0)
					Output(sb, bw, ",\n");
				Output(sb, bw, "{\"type\":\"Feature\",\"geometry\":{");
				if (g instanceof GeoPoint)
				{
					GeoPoint gp = (GeoPoint)g;
					Point2D pt2d = gp.GetPoint();
					Output(sb, bw, String.format("\"type\":\"Point\",\"coordinates\":[" + m_strCoordFormat + "," + m_strCoordFormat + "]", pt2d.getX(), pt2d.getY()));
				}
				else if (g instanceof GeoLine)
				{
					Output(sb, bw, "\"type\":\"MultiLineString\",\"coordinates\":[\n"); 
					GeoLine gl = (GeoLine)g;
					int nSubCount = gl.GetSubCount();
					for (i = 0; i < nSubCount; i++)
					{
						if (i > 0)
							Output(sb, bw, ",\n");
						Output(sb, bw, "[");
						int nPointCount = gl.GetPointCount(i);
						for (j = 0; j < nPointCount; j++)
						{
							Point2D pt2d = gl.GetPoint(j, i);
							if (j > 0)
								Output(sb, bw, ",");
							Output(sb, bw, String.format("[" + m_strCoordFormat + "," + m_strCoordFormat + "]", pt2d.getX(), pt2d.getY()));
						}
						Output(sb, bw, "]");
					}
					Output(sb, bw, "]");
				}
				else if (g instanceof GeoRegion)
				{
					Output(sb, bw, "\"type\":\"MultiPolygon\",\"coordinates\":[\n"); 
					GeoRegion gr = (GeoRegion)g;
					int nSubCount = gr.GetSubCount();
					for (i = 0; i < nSubCount; i++)
					{
						if (gr.GetShell(i) == -1)
						{
							if (i > 0)
								Output(sb, bw, "],\n");
							Output(sb, bw, "[");
						}
						else
						{
							if (i > 0)
								Output(sb, bw, ",\n");
						}
						Output(sb, bw, "[");
						int nPointCount = gr.GetPointCount(i);
						for (j = 0; j < nPointCount; j++)
						{
							Point2D pt2d = gr.GetPoint(j, i);
							if (j > 0)
								Output(sb, bw, ",");
							Output(sb, bw, String.format("[" + m_strCoordFormat + "," + m_strCoordFormat + "]", pt2d.getX(), pt2d.getY()));
						}
						Output(sb, bw, "]");
					}
					Output(sb, bw, "]]");
				}
				Output(sb, bw, "}"); //geometry
				
				if (strs != null)
				{
					Output(sb, bw, ",\"properties\":{");
					for (i = 0; i < strs.length; i++)
					{
						if (i > 0)
							Output(sb, bw, ",");
						JSONObject jo = ja.getJSONObject(arrIndex[i]);
						if (jo.getString("Type").equalsIgnoreCase("Text"))
							Output(sb, bw, String.format("\"%s\":\"%s\"", strs[i], rs.GetFieldValue(strs[i]).toString()));
						else
							Output(sb, bw, String.format("\"%s\":%s", strs[i], rs.GetFieldValue(strs[i]).toString()));
					}
					Output(sb, bw, "}"); //properties
				}
				Output(sb, bw, "}"); //feature
				
				g.Destroy();
				++h;
				rs.MoveNext();
			}
			rs.Destroy();
			
			Output(sb, bw, "]}");
			if (sb != null)
			{
				m_strOutput = sb.toString();
			}
			else
			{
				bw.flush();
				bw.close();
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
		alParam.add(new Param(m_strFileName.equalsIgnoreCase("GeoJSON") ? "GeoJSON" : "ResultFileName", m_strOutput));
		return alParam.size();
	}
	
	protected String m_strDSInput;
	protected String m_strDatasetInput;
	
	protected String m_strCoordFormat;
	protected String m_strFields;

	protected String m_strFileName;
	
	protected String m_strOutput;
}
