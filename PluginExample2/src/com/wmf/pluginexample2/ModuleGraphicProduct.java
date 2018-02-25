//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleGraphicProduct.java
// \brief GraphicProduct模块类
// \author 王庆飞
// \date 2016-5-16
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.wmf.model.*;
import com.wmf.model.Model.LogLevel;
import com.weathermap.objects.*;

public class ModuleGraphicProduct extends ModuleBase
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
			mp.m_strDS = m_strDS;
			mp.m_strDataset = m_strDataset;
			mp.m_strUnit = m_strUnit;
			mp.m_strGeoJSON = m_strGeoJSON;
			mp.m_strGeoJSONFile = m_strGeoJSONFile;
			return mp;
		}
		
		String m_strDS = "";
		String m_strDataset = "";
		String m_strUnit = "";
		String m_strGeoJSON = "";
		String m_strGeoJSONFile = ""; 
	}
	
	public ModuleGraphicProduct(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new MyPort(this));
		m_alOutputPort.add(new Port(this, Port.Type.ImageFile));
		
		m_strTemplateFile = "";
		m_rcBounds = new Rectangle2D.Double();
		m_strClipRegion = "";
		m_strMainTitle = "";
		m_strSubTitle = "";
		m_strOrganization = "";
		m_strPublishTime = "";
		
		m_strFileName = "c:/[yyMMddHH].png";
		
		m_strResultFileName = "";
    }
	public void Destroy()
	{
		if (m_wsTemplate != null)
		{
			m_wsTemplate.Destroy();
			m_wsTemplate = null;
		}
		super.Destroy();
	}
	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "图形产品\n制作";
	}
	public String GetDescription()
	{
		return "输入数据集，输出图形产品。";
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
			if (!(portTo instanceof MyPort))
				return false;
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			MyPort mp = (MyPort)portTo;
			if (alParam.size() == 1)
			{
				if (portFrom.GetType() != Port.Type.JSONFile)
					return false;
				if (alParam.get(0).m_strName.equalsIgnoreCase("GeoJSON"))
				{
					mp.m_strGeoJSON = (String)alParam.get(0).m_objValue;
					mp.m_strGeoJSONFile = "";
				}
				else
				{
					mp.m_strGeoJSON = "";
					mp.m_strGeoJSONFile = (String)alParam.get(0).m_objValue;
				}
			}
			else if (alParam.size() == 2)
			{
				if (!alParam.get(0).m_strName.equalsIgnoreCase(Param.Datasource) || !alParam.get(1).m_strName.equalsIgnoreCase(Param.Dataset))
					return false;
				mp.m_strDS = (String)alParam.get(0).m_objValue;
				mp.m_strDataset = (String)alParam.get(1).m_objValue;
				mp.SetType(portFrom.GetType());
			}
			else
			{
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
	public boolean OnDetach(Port portFrom, Port portTo)
	{
		if (!super.OnDetach(portFrom, portTo))
			return false;
		try
		{
			int i = FindPort(portTo, true);
			if (i == -1)
				return true;
			if (!(portTo instanceof MyPort))
				return true;
			MyPort mp = (MyPort)portTo;
			ArrayList<Param> alParam = new ArrayList<Param>();
			portFrom.GetModule().GetOutputParam(portFrom, alParam);
			if (alParam.size() == 1)
			{
				if (portFrom.GetType() != Port.Type.JSONFile)
					return false;
				mp.m_strGeoJSON = "";
				mp.m_strGeoJSONFile = "";
			}
			else if (alParam.size() == 2)
			{
				if (!alParam.get(0).m_strName.equalsIgnoreCase(Param.Datasource) || !alParam.get(1).m_strName.equalsIgnoreCase(Param.Dataset))
					return false;
				mp.m_strDS = "";
				mp.m_strDataset = "";
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
			int i = 0;
			
			alParam.add(new Param("TemplateFile", m_strTemplateFile, "模板文件", "", "", Param.EditType.File, "模板文件(*.xml)|*.xml"));
			
			for (i = 0; i < m_alInputPort.size(); i++)
			{
				Port p = m_alInputPort.get(i);
				if (!(p instanceof MyPort))
					break;
				MyPort mp = (MyPort)p;
				String strGroups = "输入端/" + mp.GetName();
				alParam.add(new Param(Param.Datasource, mp.m_strDS, "数据源", "数据源别名", strGroups, Param.EditType.ReadOnly));
				alParam.add(new Param(Param.Dataset, mp.m_strDataset, "数据集", "数据集名称", strGroups, Param.EditType.ReadOnly));
				alParam.add(new Param("Unit", mp.m_strUnit, "单位", "", strGroups));
			}
			
			alParam.add(new Param(Param.Bounds, String.format("%f %f %f %f", m_rcBounds.getMinX(), m_rcBounds.getMinY(), m_rcBounds.getMaxX(), m_rcBounds.getMaxY()), 
					"显示范围", "left bottom right top", "参数", Param.EditType.FixedFloatArray));
			alParam.add(new Param("ClipRegion", m_strClipRegion, "裁剪区域", "例如：[NAME]='山西省'", "参数", Param.EditType.Expression, ""));
			alParam.add(new Param("MainTitle", m_strMainTitle, "主标题", "", "参数"));
			alParam.add(new Param("SubTitle", m_strSubTitle, "副标题", "", "参数"));
			alParam.add(new Param("Organization", m_strOrganization, "制作单位", "", "参数"));
			alParam.add(new Param("PublishTime", m_strPublishTime, "发布时间", "", "参数"));
			
			String strOptionalValues = "";
			for (i = 0; i < m_alInputPort.size(); i++)
			{
				Port p = m_alInputPort.get(i);
				if (!(p instanceof MyPort))
					break;
				MyPort mp = (MyPort)p;
				if (i > 0)
					strOptionalValues += "|";
				strOptionalValues += mp.GetName();
			}
			for (i = 0; i < m_alGeoLegendToLayer.size(); i++)
			{
				alParam.add(new Param("GeoLegendToLayer", m_alGeoLegendToLayer.get(i), String.format("图例%d对应图层", i + 1), "", "参数/图例", Param.EditType.Select, strOptionalValues));
			}
			
			alParam.add(new Param("FileName", m_strFileName, "文件名", "", "输出端", Param.EditType.File, "png文件(*.png)|*.png|所有文件(*.*)|*.*"));
			
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
			int j = 0, k = 0, l = 0;
			String str;
			//-------------------------------------------------------------------------------------------------------------------
			m_strTemplateFile = (String)alParam.get(i++).m_objValue;
			ArrayList<String> alName = new ArrayList<String>();
			int nGeoLegendCount = 0;
			if (m_strTemplateFile.isEmpty())
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"模板文件\"参数为空。", GetNestedName(), GetID()));
			}
			else
			{
				String strTemplateFile = m_strTemplateFile;
				if (!new File(strTemplateFile).isAbsolute())
					strTemplateFile = new File(m_model.m_strFileName).getParent() + "/" + m_strTemplateFile;
				if (!new File(strTemplateFile).exists())
				{
					m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块模板文件\"%s\"不存在。", GetNestedName(), GetID(), strTemplateFile));
				}
				else
				{
					if (m_wsTemplate != null)
						m_wsTemplate.Destroy();
					m_wsTemplate = new Workspace();
					if (!m_wsTemplate.Load(strTemplateFile.replace('\\', '/')))
					{
						m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块加载模板文件\"%s\"失败！", GetNestedName(), GetID(), strTemplateFile));
						m_wsTemplate.Destroy();
						m_wsTemplate = null;
					}
					else
					{
						for (j = 0; j < m_wsTemplate.GetMapCount(); j++)
						{
							Map map = m_wsTemplate.GetMap(j);
							String strMapName = map.GetName();
							if (strMapName.isEmpty())
								continue;
							for (k = map.GetLayerGroupCount() - 1; k >= 0; k--)
							{
								LayerGroup lg = map.GetLayerGroup(k);
								String strLayerGroupName = lg.GetName();
								if (strLayerGroupName.isEmpty())
									continue;
								for (l = map.GetLayerCount(strLayerGroupName) - 1; l >= 0; l--)
								{
									Layer layer = map.GetLayer(l, strLayerGroupName);
									if (layer.GetDataset() != null)
										continue;
									String strLayerName = layer.GetName();
									if (strLayerName.isEmpty())
										continue;
									alName.add(strMapName + "." + strLayerGroupName + "." + strLayerName);
									str = layer.GetType();
									if (str.equalsIgnoreCase("VectorField"))
									{
										alName.add(strMapName + "." + strLayerGroupName + "." + strLayerName + ".Direction");
									}
									else if (str.equalsIgnoreCase("VectorFieldArrow"))
									{
										alName.add(strMapName + "." + strLayerGroupName + "." + strLayerName + ".U");
										alName.add(strMapName + "." + strLayerGroupName + "." + strLayerName + ".V");
									}
								}
							}
						}
						
						Layout layout = m_wsTemplate.GetLayout(0);
						DatasetVector dvLayout = (DatasetVector)layout.GetDatasource().GetDataset(0);
				        Recordset rsLayout = dvLayout.Query(null, null);
				        rsLayout.MoveFirst();
				        while (!rsLayout.IsEOF())
				        {
				        	Geometry g = rsLayout.GetGeometry();
				            str = g.GetType();
				            if (str.equalsIgnoreCase("GeoLegend"))
				            {
				            	if (g.GetPropertyValueCount("LegendItem") == 0)
				            		++nGeoLegendCount;
				            }
				        	rsLayout.MoveNext();
				        }
				        rsLayout.Destroy();
					}
				}
			}
			
			for (k = 0; k < m_alInputPort.size(); k++)
			{
				Port p = m_alInputPort.get(k);
				if (!(p instanceof MyPort))
					break;
			}
			for (j = k; j < alName.size(); j++)
				InsertPort(k == m_alInputPort.size() ? -1 : k, new MyPort(this), true);
			for (j = 0; j < alName.size(); j++)
			{
				MyPort mp = (MyPort)m_alInputPort.get(j);
				mp.SetName(alName.get(j));
			}
			for (j = k - 1; j >= 1 && j >= alName.size(); j--)
				RemovePort(j, true);
			//-------------------------------------------------------------------------------------------------------------------
			for (j = 0; j < alName.size(); j++)
			{
				MyPort mp = (MyPort)m_alInputPort.get(j);
				if (alParam.get(i).m_strName.equalsIgnoreCase(Param.Datasource) && alParam.get(i + 1).m_strName.equalsIgnoreCase(Param.Dataset))
				{
					mp.m_strDS = (String)alParam.get(i++).m_objValue;
					mp.m_strDataset = (String)alParam.get(i++).m_objValue;
					mp.m_strUnit = (String)alParam.get(i++).m_objValue;
				}
				else
				{
					mp.m_strDS = "";
					mp.m_strDataset = "";
					mp.m_strUnit = "";
				}
			}
			
			while (!alParam.get(i).m_strName.equalsIgnoreCase("Bounds"))
				i++;
			String[] strs = ((String)alParam.get(i++).m_objValue).split(" ");
			m_rcBounds = new Rectangle2D.Double(Double.parseDouble(strs[0]), Double.parseDouble(strs[1]), 
					Double.parseDouble(strs[2]) - Double.parseDouble(strs[0]), Double.parseDouble(strs[3]) - Double.parseDouble(strs[1]));
			m_strClipRegion = (String)alParam.get(i++).m_objValue;
			m_strMainTitle = (String)alParam.get(i++).m_objValue;
			m_strSubTitle = (String)alParam.get(i++).m_objValue;
			m_strOrganization = (String)alParam.get(i++).m_objValue;
			m_strPublishTime = (String)alParam.get(i++).m_objValue;
			
			m_alGeoLegendToLayer.clear();
			for (j = 0; j < nGeoLegendCount; j++)
			{
				if (alParam.get(i).m_strName.equalsIgnoreCase("GeoLegendToLayer"))
					m_alGeoLegendToLayer.add((String)alParam.get(i++).m_objValue);
				else
					m_alGeoLegendToLayer.add("");
			}
			
			while (!alParam.get(i).m_strName.equalsIgnoreCase("FileName"))
				i++;
			m_strFileName = (String)alParam.get(i++).m_objValue;
			//-------------------------------------------------------------------------------------------------------------------
			m_alOutputPort.get(0).SetName(m_strFileName);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	public boolean OnBeforeExecute()
	{
		try
		{
			if (m_wsTemplate == null)
			{
				m_model.OutputLog(Model.LogLevel.Error, String.format("\"%s-%d\"模块\"模板\"为null。", GetNestedName(), GetID()));
				return false;
			}
			int i = 0, j = 0, k = 0;
			String str;
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			//--------------------------------------------------------------------------------------------------------------
			//加载模板到当前工作空间
			String strTemplateFile = m_strTemplateFile;
			if (!new File(strTemplateFile).isAbsolute())
				strTemplateFile = new File(m_model.m_strFileName).getParent() + "/" + m_strTemplateFile;
			if (!new File(strTemplateFile).exists())
				throw new AssertionError();
			SAXReader saxr = new SAXReader();
			Document doc = saxr.read(new InputStreamReader(new FileInputStream(strTemplateFile), "gb2312"));
			String strPath = new File(strTemplateFile).getParent().replace('\\', '/');
			
			List<Element> listElement = doc.selectNodes("Workspace/Datasources/Datasource");
			Iterator<Element> it = listElement.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				str = e.attributeValue("Server");
				if (!new File(str).isAbsolute())
					str = strPath + "/" + str;
				if (ws.GetDatasource(e.attributeValue("Alias")) != null)
				{
					model.OutputLog(LogLevel.Warning, String.format("\"%s-%d\"模块模板中数据源\"%s\"和工作空间中数据源重名。", GetNestedName(), GetID(), 
							e.attributeValue("Alias"))); 
				}
				ws.OpenDatasource("{\"Type\":\"" + e.attributeValue("Type") + "\",\"Alias\":\"" + e.attributeValue("Alias") + "\",\"Server\":\"" + str + "\"}");
			}
			
			listElement = doc.selectNodes("Workspace/Maps/Map");
			it = listElement.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				Map map = new Map();
                ws.InsertMap(map, -1);
                map.FromXML(e.asXML());
			}
			
			listElement = doc.selectNodes("Workspace/Layouts/Layout");
			it = listElement.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				Layout layout = new Layout();
                ws.InsertLayout(layout, -1);
                layout.FromXML(e.asXML());
			}
			
			listElement = ((Element)doc.selectSingleNode("Workspace/Symbols")).elements();
			it = listElement.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				str = e.attributeValue("FileName");
				if (!new File(str).isAbsolute())
					str = strPath + "/" + str;
				if (e.getName().equals("Symbol"))
					ws.LoadSymbol(str);
				else if (e.getName().equals("LineSymbol"))
					ws.LoadLineSymbol(str);
				else if (e.getName().equals("FillSymbol"))
					ws.LoadFillSymbol(str);
			}
			//--------------------------------------------------------------------------------------------------------------
			for (i = m_alInputPort.size() - 1, j = 0; i >= 0; i--)
			{
				Port p = m_alInputPort.get(i);
				if (!(p instanceof MyPort))
					continue;
				MyPort mp = (MyPort)p;
				Datasource ds = ws.GetDatasource(mp.m_strDS);
				if (ds == null)
					continue;
				Dataset dataset = null;
				if(mp.m_strDataset.equals("原始数据")){
					dataset = ds.GetDataset(0);
				}
				else{
					dataset = ds.GetDataset(mp.m_strDataset);
				}
				if (dataset == null)
					continue;
				
				str = mp.GetName();
				if (str.isEmpty())
	    			continue;
				String[] strs = str.split("\\.");
				if (strs.length < 3)
					continue;
				Map map = ws.GetMap(strs[0]);
				if (map == null)
					continue;
				LayerGroup lg = map.GetLayerGroup(strs[1]);
				if (lg == null)
					continue;
				Layer layer = map.GetLayer(strs[2], lg.GetName());
				if (layer == null)
					continue;
				
				if (strs.length == 3)
					layer.SetDataset(dataset);
				else if (strs.length == 4)
					layer.SetPropertyValue(strs[3], "{\"Datasource\":\"" + mp.m_strDS + "\",\"Dataset\":\"" + mp.m_strDataset + "\"}");
				if (j == 0)
				{
					if (!m_rcBounds.isEmpty())
					{
						map.SetBounds(m_rcBounds);
					}
					else
					{
						if (map.GetBounds().isEmpty())
						{
							Rectangle2D rc = dataset.GetBounds();
							if (!dataset.GetProjection().equals(map.GetProjection()))
								Projection.Transform(dataset.GetProjection(), map.GetProjection(), rc);
							map.SetBounds(rc);
						}
					}
					
					if (!m_strClipRegion.isEmpty())
					{
						Layer layerR = map.GetLayer("Region");
						if (layerR == null)
							layerR = map.GetLayer("Region", "MGCTop");
						if (layerR != null)
						{
							Dataset datasetR = layerR.GetDataset();
							if (datasetR != null)
								map.SetClipRegion("{\"Datasource\":\"" + datasetR.GetDatasource().GetAlias() + "\",\"Dataset\":\"" + datasetR.GetName() + "\",\"Where\":\"" + m_strClipRegion + "\"}");
							layerR.SetPropertyValue("Filter", m_strClipRegion);
						}	
					}
				}
				++j;
			}
			
			m_strSubTitleValue = m_strSubTitle;
			m_strPublishTimeValue = m_strPublishTime;
			Layout layout = ws.GetLayout(0);
			DatasetVector dvLayout = (DatasetVector)layout.GetDatasource().GetDataset(0);
	        Recordset rsLayout = dvLayout.Query(null, null);
	        rsLayout.MoveFirst();
	        i = 0;
	        while (!rsLayout.IsEOF())
	        {
	            Geometry g = rsLayout.GetGeometry();
	            str = g.GetType();
	            if (str.equalsIgnoreCase("GeoText"))
	            {
	            	rsLayout.Edit();
	                
	                GeoText gt = (GeoText)g;
	                str = gt.GetText();
	                if (str.equalsIgnoreCase("MainTitle"))
	                {
	                	gt.SetText(m_strMainTitle);
	                }
	                else if (str.equalsIgnoreCase("SubTitle"))
	                {
	                	m_strSubTitleValue = m_model.FormatDate(m_strSubTitle, true);	                	
	                	gt.SetText(m_strSubTitleValue);
	                }
	                else if (str.equalsIgnoreCase("Organization"))
	                {
	                	gt.SetText(m_strOrganization);
	                }
	                else if (str.equalsIgnoreCase("PublishTime"))
	                {
	                	m_strPublishTimeValue = m_model.FormatDate(m_strPublishTime, true);
	                	gt.SetText(m_strPublishTimeValue);
	                }
	                
	                rsLayout.SetGeometry(g);
	                rsLayout.Update();
	            }
	            else if (str.equalsIgnoreCase("GeoPicture"))
	            {
	            	str = g.GetPropertyValue("FileName");
	            	if (!new File(str).isAbsolute())
	            	{
	            		rsLayout.Edit();
	            		g.SetPropertyValue("FileName", strPath + "/" + str);
		            	rsLayout.SetGeometry(g);
		                rsLayout.Update();
	            	}
	            }
	            else if (str.equalsIgnoreCase("GeoLegend"))
	            {
	            	if (g.GetPropertyValueCount("LegendItem") == 0)
	            	{
	            		Layer layer = null;
	            		str = m_alGeoLegendToLayer.get(i++);
	            		if (!str.isEmpty())
	            		{
		            		String[] strs = str.split("\\.");
		    				if (strs.length >= 3)
		    				{
		    					Map map = ws.GetMap(strs[0]);
		    					if (map != null)
		    					{
		    						LayerGroup lg = map.GetLayerGroup(strs[1]);
		    						if (lg != null)
		    							layer = map.GetLayer(strs[2], lg.GetName());
		    					}
		    				}
	            		}
	    				if (layer != null)
	    				{
	    					String strType = layer.GetType();
							if (strType.equalsIgnoreCase("VectorRange") || strType.equalsIgnoreCase("VectorUnique"))
							{
								int nCount = layer.GetPropertyValueCount(strType + "Item");
		                        ArrayList<Double> al = new ArrayList<Double>(); 
		                        for (j = 0; j < nCount; j++)
		                        {
		                        	str = layer.GetPropertyValue(strType + "Item", j);
		                            JSONObject jsonObj = new JSONObject(str);
		                        	al.add(jsonObj.getDouble("Value"));
		                        }
		                        
		                        DatasetVector dv = (DatasetVector)layer.GetDataset();
		                    	double[] dRange = new double[2];
		                    	for (j = 0; j < 2; j++)
		                    	{
			                    	Recordset rs = dv.Query("{\"Fields\":\"" + (j == 0 ? "min" : "max") + "(" + layer.GetPropertyValue(strType.substring(6) + "Expression") + ")\"}", null);
			                    	dRange[j] = (Double)rs.GetFieldValue(null);
			                    	rs.Destroy();
		                    	}
		                        
		                        int[] nRange = new int[]{0, nCount - 1};
		                        for (j = 0; j < 2; j++)
		                        {
			                        for (k = nCount - 1; k >= 0; k--)
			                        {
			                        	if (dRange[j] >= al.get(k))
			                        	{
			                        		nRange[j] = k;
			                        		break;
			                        	}
			                        }
		                        }
		                        
		                        for (j = nRange[0]; j <= nRange[1]; j++)
		                        {
		                        	str = layer.GetPropertyValue(strType + "Item", j);
		                            JSONObject jsonObj = new JSONObject(str);
		    	        			str = String.format("{\"Type\":\"%s\",\"Caption\":\"%s\"", dv.GetType(), jsonObj.getString("Caption"));
		    	        			if (jsonObj.has("PointStyle"))
		    	        				str += ",\"PointStyle\":" + jsonObj.getString("PointStyle"); 	
		    	        			if (jsonObj.has("LineStyle"))
		    	        				str += ",\"LineStyle\":" + jsonObj.getString("LineStyle");
		    	        			if (jsonObj.has("FillStyle"))
		    	        				str += ",\"FillStyle\":" + jsonObj.getString("FillStyle");
		    	        			str += "}";
		    	                    g.AddPropertyValue("LegendItem", str);
		                        }
		                        if (nRange[1] < nCount - 1)
		                        {
		                        	str = layer.GetPropertyValue(strType + "Item", nRange[1] + 1);
		                            JSONObject jsonObj = new JSONObject(str);
		                        	g.SetPropertyValue("MaxValueCaption", jsonObj.getString("Caption"));
		                        }
							}
							else if (strType.equalsIgnoreCase("RasterRange") || strType.equalsIgnoreCase("RasterUnique"))
							{
								int nCount = layer.GetPropertyValueCount(strType + "Item");
		                        ArrayList<Double> al = new ArrayList<Double>(); 
		                        for (j = 0; j < nCount; j++)
		                        {
		                        	str = layer.GetPropertyValue(strType + "Item", j);
		                            JSONObject jsonObj = new JSONObject(str);
		                        	al.add(jsonObj.getDouble("Value"));
		                        }
		                        
		                        DatasetRaster dr = (DatasetRaster)layer.GetDataset();
		                    	double[] dRange = new double[]{dr.GetMinValue(), dr.GetMaxValue()};
		                        
		                        int[] nRange = new int[]{0, nCount - 1};
		                        for (j = 0; j < 2; j++)
		                        {
			                        for (k = nCount - 1; k >= 0; k--)
			                        {
			                        	if (dRange[j] >= al.get(k))
			                        	{
			                        		nRange[j] = k;
			                        		break;
			                        	}
			                        }
		                        }
		                        
		                        for (j = nRange[0]; j <= nRange[1]; j++)
		                        {
		                        	str = layer.GetPropertyValue(strType + "Item", j);
		                            JSONObject jsonObj = new JSONObject(str);
		    	        			str = String.format("{\"Type\":\"Region\",\"Caption\":\"%s\",\"FillStyle\":{\"ForeColor\":\"%s\"}}", 
		    	        					jsonObj.getString("Caption"), jsonObj.getString("Color"));
		    	                    g.AddPropertyValue("LegendItem", str);
		                        }
		                        if (nRange[1] < nCount - 1)
		                        {
		                        	str = layer.GetPropertyValue(strType + "Item", nRange[1] + 1);
		                            JSONObject jsonObj = new JSONObject(str);
		                        	g.SetPropertyValue("MaxValueCaption", jsonObj.getString("Caption"));
		                        }
							}
	    				}
	            	}
	            }
	            g.Destroy();
	            rsLayout.MoveNext();
	        }
	        rsLayout.Destroy();
	        
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
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
		boolean bResult = false;
		try
		{
			int i = 0, j = 0;
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			
			Layout layout = ws.GetLayout(0);
			layout.Draw();
			Image image = layout.GetGraphics().GetImage();
			
			m_strResultFileName = m_strFileName.replace('\\', '/');
			m_strResultFileName = m_model.FormatDate(m_strResultFileName, false);			
			if (!new File(m_strResultFileName).isAbsolute())
				m_strResultFileName = new File(m_model.m_strFileName).getParent().replace('\\', '/') + "/" + m_strResultFileName;
			
			File file = new File(new File(m_strResultFileName).getParent());
			if (!file.exists())
				file.mkdirs();
			bResult = image.Save(m_strResultFileName);
			//------------------------------------------------------------------------------------------------------------------
			for (i = 0; i < m_alInputPort.size(); i++)
			{
				Port p = m_alInputPort.get(i);
				if (!(p instanceof MyPort))
					break;
				MyPort mp = (MyPort)p;
				if (mp.m_strGeoJSON.isEmpty() && mp.m_strGeoJSONFile.isEmpty())
					continue;
				if (!mp.m_strGeoJSONFile.isEmpty())
				{
					if (!new File(mp.m_strGeoJSONFile).exists())
						continue;
				}
				
				String str = mp.GetName();
				if (str.isEmpty())
					continue;
				String[] strs = str.split("\\.");
				if (strs.length < 3)
					continue;
				Map map = ws.GetMap(strs[0]);
				if (map == null)
					continue;
				LayerGroup lg = map.GetLayerGroup(strs[1]);
				if (lg == null)
					continue;
				Layer layer = map.GetLayer(strs[2], lg.GetName());
				if (layer == null)
					continue;
				
				StringBuffer sb = null, sbGeoJSON = null;
				BufferedWriter bw = null;
				if (!mp.m_strGeoJSON.isEmpty())
				{
					sb = new StringBuffer();
				}
				else
				{
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mp.m_strGeoJSONFile), "UTF-8"));
					sbGeoJSON = new StringBuffer(); 
			        while ((str = br.readLine()) != null) 
			        {
			        	sbGeoJSON.append(str + "\n");
			        }
			        br.close();
			        
					bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mp.m_strGeoJSONFile), "UTF-8"));
				}
		        
				Output(sb, bw, "[{\"type\":\"Metadata\",\"Metadata\":{");
				Output(sb, bw, "\"MainTitle\":\"" + m_strMainTitle + "\",\"SubTitle\":\"" + m_strSubTitleValue + 
						"\",\"Organization\":\"" + m_strOrganization + "\",\"PublishTime\":\"" + m_strPublishTimeValue + 
						"\",\"Unit\":\"" + mp.m_strUnit + "\"}}"); 
				
				str = layer.GetType();
				if (str.equalsIgnoreCase("VectorRange") || str.equalsIgnoreCase("VectorUnique") || 
						str.equalsIgnoreCase("RasterRange") || str.equalsIgnoreCase("RasterUnique"))
				{
					str += "Item";
					Output(sb, bw, ",\n{\"type\":\"" + str + "\",\"items\":[\n");
					int nCount = layer.GetPropertyValueCount(str);
					for (j = 0; j < nCount; j++)
					{
						if (j > 0)
                        	Output(sb, bw, ",\n");
						Output(sb, bw, layer.GetPropertyValue(str, j));
					}
					Output(sb, bw, "]}");
				}
				
				Output(sb, bw, ",\n");
				Output(sb, bw, sb != null ? mp.m_strGeoJSON : sbGeoJSON.toString());
				Output(sb, bw, "]");
				
				if (sb != null)
				{
					mp.m_strGeoJSON = sb.toString();	
				}
				else
				{
					bw.flush();
					bw.close();
				}
			}
			
			//------------------------------------------------------------------------------------------------------------------
			OnParamChanged();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
        
		return bResult;
	}
	public boolean OnAfterExecute()
	{
		if (m_wsTemplate != null)
		{
			int i = 0;
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			
			for (i = 0; i < m_wsTemplate.GetDatasourceCount(); i++)
            {
				Datasource ds = m_wsTemplate.GetDatasource(i);
                ws.CloseDatasource(ds.GetAlias());
            }
			
			for (i = ws.GetMapCount() - 1; i >= 0; i--)
                ws.RemoveMap(i);
            for (i = ws.GetLayoutCount() - 1; i >= 0; i--)
                ws.RemoveLayout(i);
            
            Datasource ds = ws.GetSymbolDatasource();
            if (ds != null)
            {
                for (i = 0; i < 3; i++)
                {
                    DatasetVector dv = (DatasetVector)ds.GetDataset(i == 0 ? "Symbol" : (i == 1 ? "LineSymbol" : "FillSymbol"));
                    if (dv == null)
                        continue;
                    Recordset rs = dv.Query("", null);
                    rs.DeleteAll();
                    rs.Destroy();
                }
            }
		}
		
		return super.OnAfterExecute();
	}
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		alParam.add(new Param("ResultFileName", m_strResultFileName));
		for (i = 0; i < m_alInputPort.size(); i++)
		{
			Port p = m_alInputPort.get(i);
			if (!(p instanceof MyPort))
				break;
			MyPort mp = (MyPort)p;
			if (mp.m_strGeoJSON.isEmpty() && mp.m_strGeoJSONFile.isEmpty())
				continue;
			if (!mp.m_strGeoJSON.isEmpty())
				alParam.add(new Param("GeoJSON", mp.m_strGeoJSON));
			else
				alParam.add(new Param("GeoJSONFile", mp.m_strGeoJSONFile));
		}
		return alParam.size();
	}
	//------------------------------------------------------------------------------------
	protected String m_strTemplateFile;
	protected Rectangle2D m_rcBounds;
	protected String m_strClipRegion;
	protected String m_strMainTitle;
	protected String m_strSubTitle;
	protected String m_strOrganization;
	protected String m_strPublishTime;
	protected ArrayList<String> m_alGeoLegendToLayer = new ArrayList<String>();
	
	protected String m_strFileName;
	//------------------------------------------------------------------------------------
	protected Workspace m_wsTemplate = null;
	protected String m_strSubTitleValue, m_strPublishTimeValue;
	//------------------------------------------------------------------------------------
	protected String m_strResultFileName;
}
