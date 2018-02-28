//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleOpenDS.java
// \brief Cimiss数据查询类
// \author 汪坤
// \date 2018-02-@6
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.projectplugin;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.weathermap.objects.GeoPoint;
import com.weathermap.objects.Recordset;
import com.weathermap.wmo.accessor.Dataset;
import com.weathermap.wmo.accessor.DatasetVector;
import com.weathermap.wmo.accessor.Datasource;
import com.weathermap.wmo.accessor.Field;
import com.weathermap.wmo.space.Workspace;
import com.wmf.common.FileHelper;
import com.wmf.mode.CimissData;
import com.wmf.model.*;
import com.wmf.pluginexample2.*;

public class ModuleJsonToDatasetVector extends ModuleBase
{
	public ModuleJsonToDatasetVector(int nID)
	{
		super(nID);
		Port portIn = new Port(this);
		portIn.SetType(Port.Type.valueOf("JSONFile"));
		portIn.SetName("json数据");
		m_alInputPort.add(portIn);
		Port portOut = new Port(this);
		portOut.SetType(Port.Type.DatasetPoint);
		portOut.SetName("临时数据集");
		m_alOutputPort.add(portOut);
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "Json转换\nDatasetVector";
	}
	public String GetDescription()
	{
		return "Cimiss查询到的json转换成datasetvector。";
	}
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		int nOffset = super.GetParam(alParam);
		alParam.add(new Param("Alias", m_strDatasetAlias, "数据集别名", "", ""));
		alParam.add(new Param("Alias", m_strValueFieldName, "值字段", "", ""));
		Model.GetAlias(alParam, m_alAlias, nOffset);
		
		return alParam.size();
	}
	public boolean OnParamChanged(ArrayList<Param> alParam, int nIndex, Object objValue){
		return super.OnParamChanged(alParam, nIndex, objValue);
	}
	//用于 XML->参数 等
	public int SetParam(final ArrayList<Param> alParam){
		int i = super.SetParam(alParam);
		Model2 model = (Model2)m_model;
		m_strDatasetAlias = (String)alParam.get(i++).m_objValue;
		m_strValueFieldName = (String)alParam.get(i++).m_objValue;
		return i;
	}
	
	public boolean Execute(){
		Model2 model = ((Model2)m_model);
		Workspace ws = new Workspace(model.m_ws.GetHandle());
		Datasource ds = ws.GetDatasource(0);
		DatasetVector dv = ds.CreateDatasetVector("tempDV", Dataset.Type.Point);
		dv.SetProjection("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
		//创建字段
		dv.AddField(new Field("Lon", Field.Type.Double, ""));//经度
		dv.AddField(new Field("Lat", Field.Type.Double, ""));//纬度
		dv.AddField(new Field(m_strValueFieldName, Field.Type.Double, ""));//值字段
		//打开
		model.OutputLog(Model.LogLevel.Info, strJson);
		try {
			JSONObject jo = new JSONObject(strJson);
			int returnCode = jo.getInt("returnCode");
			if(returnCode==-1){
				model.OutputLog(Model.LogLevel.Error, "Cimiss返回代码为-1");
				return false;
			}
			JSONArray ja =  jo.getJSONArray("DS");
			if(ja==null){
				model.OutputLog(Model.LogLevel.Error, "Cimiss数据为空");
				return false;
			}
			Gson gson = new Gson();
			Recordset rs = dv.Query(null,null);
			Rectangle2D rcBounds = new Rectangle2D.Double(), rc;
			int dataCount = ja.length();
			for(int i=0;i<dataCount;i++){
				String objData = ja.get(i).toString();
				CimissData  cd = gson.fromJson(objData, CimissData.class);
				GeoPoint geoPoint = new GeoPoint(cd.getLon(),cd.getLat());
				rc = geoPoint.GetBounds();
                if (rs.GetRecordCount() == 0)
                    rcBounds = rc;
                else
                	Rectangle2D.union(rc, rcBounds, rcBounds);
                
				rs.AddNew(geoPoint);
				rs.SetFieldValue(m_strValueFieldName, cd.getSUM_PRE_1H());
				rs.Update();
				geoPoint.Destroy();
			}
			rs.Destroy();
			dv.SetBounds(rcBounds);	
		} catch (JSONException e) {
			e.printStackTrace();
		}
		model.OutputLog(Model.LogLevel.Info, "生成DV完成!");
		return true;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam){
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		alParam.add(new Param(Param.Datasource, "tempDS"));
		alParam.add(new Param(Param.Dataset, m_strDatasetAlias));
		return alParam.size();
	}
	public boolean OnAttach(Port portFrom, Port portTo){
		int i = FindPort(portFrom, false);
		if (i != -1)
			return true;
		i = FindPort(portTo, true);
		if (i == -1)
			return false;
		ArrayList<Param> tempParam = new ArrayList<Param>();
		portFrom.GetModule().GetOutputParam(portFrom, tempParam);
		if (tempParam.size() < 1)
			return false;
		strJson = (String)tempParam.get(0).m_objValue;
		return true;
	}
	protected String m_strDatasetAlias;//数据集别名
	protected String m_strValueFieldName;//值字段名
	protected String strJson;//json数据
}
