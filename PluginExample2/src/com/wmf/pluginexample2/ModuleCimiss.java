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

package com.wmf.pluginexample2;
import java.util.ArrayList;

import com.wmf.model.*;

public class ModuleCimiss extends ModuleBase
{
	public ModuleCimiss(int nID)
	{
		super(nID);
		Port port = new Port(this);
		port.SetType(Port.Type.valueOf("JSONFile"));
		port.SetName("json数据");
		m_alOutputPort.add(new Port(this));
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "Cimiss\n查询";
	}
	public String GetDescription()
	{
		return "Cimiss数据查询。";
	}
	
	public boolean OnAttachModel(Model model)
	{
		if (!super.OnAttachModel(model))
			return false;
		m_model = model;
		Execute();
		return true;
	}
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		int nOffset = super.GetParam(alParam);
		alParam.add(new Param("Alias", m_strApiUser, "Cimiss账号", "", ""));
		alParam.add(new Param("Alias", m_strApiPwd, "Cimiss密码", "", ""));
		alParam.add(new Param("Alias", m_strHost, "主机", "", ""));
		alParam.add(new Param("Alias", m_strAreaCode, "区域编码", "", ""));
		
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
		m_strApiUser = (String)alParam.get(i++).m_objValue;
		m_strApiPwd = (String)alParam.get(i++).m_objValue;
		m_strHost = (String)alParam.get(i++).m_objValue;
		m_strAreaCode = (String)alParam.get(i++).m_objValue;
		return i;
	}
	
	public boolean Execute(){
		Model2 model = ((Model2)m_model);
		String urlF = "http://%s/cimiss-web/api?userId=%s&pwd=%s&interfaceId=statSurfPreInRegion&elements=Station_Name,Cnty,Station_Id_C,Lat,Lon&timeRange=[20170621000000,20170622010000]&adminCodes=%s&dataFormat=json";
		String url = String.format(urlF, m_strHost,m_strApiUser,m_strApiPwd,m_strAreaCode);
		model.OutputLog(Model.LogLevel.Info, url);
		return true;
	}
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam){
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		/*Model2 model = ((Model2)m_model);
		Datasource ds = m_strAlias.isEmpty() ? null : model.m_ws.GetDatasource(m_strAlias);
		if (ds == null || i >= ds.GetDatasetCount())
			return 0;
		alParam.add(new Param(Param.Datasource, ds.GetAlias()));
		
		Dataset dataset = ds.GetDataset(i);
		dataset.Open();
		if (dataset instanceof DatasetRaster)
			((DatasetRaster)dataset).CalcExtreme();
		alParam.add(new Param(Param.Dataset, dataset.GetName()));*/
		return alParam.size();
	}
	protected String m_strApiUser;
	protected String m_strApiPwd;
	protected String m_strHost;//主机
	protected String m_strAreaCode;//区域编码
}
