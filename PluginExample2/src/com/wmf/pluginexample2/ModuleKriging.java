//-------------------------------------------------------------
// \project PluginExample2
// \file ModuleKriging.java
// \brief Kriging模块类
// \author 王庆飞
// \date 2016-7-11
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample2;

import java.util.ArrayList;
import java.awt.geom.Rectangle2D;
import java.io.File;

import org.codehaus.jettison.json.JSONObject;

import com.wmf.model.*;
import com.weathermap.objects.*;

public class ModuleKriging extends ModuleBase
{
	public ModuleKriging(int nID)
	{
		super(nID);
		
		m_alInputPort.add(new DatasetPort(this, Port.Type.DatasetPoint));
		m_alOutputPort.add(new Port(this, Port.Type.DatasetRaster));
		
		m_strField = "";
		m_rcBounds = new Rectangle2D.Double();
		m_strCellSize = "0.05 0.05";
		m_strCellValueType = "Single";
		m_strSearchMode = "RadiusVariable";
		
		m_nPointCount = 12;
		m_nMaxRadius = 0;
		
		m_nRadius = 5;
		m_nMinPointCount = 0;
		
		m_strSemiVariogramModel = "Spherical";
		m_dRange = 0.5;
		m_dNugget = 0.01; 
		m_dSill = 0.5;
		m_a = 1.0;
		m_b = 2.0;
		
		m_bCrossValidation = false;
		m_strResidualFileName = "";
		m_strDSRaster = "";
		m_strDatasetRaster = String.format("Kriging%d", m_nID);
    }

	public String GetGroupName()
	{
		return "空间分析";
	}
	public String GetName()
	{
		return "普通克里金\n插值";
	}
	public String GetDescription()
	{
		return "普通克里金插值站点到格点。";
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
			DatasetPort dp = (DatasetPort)portTo;
			dp.m_strDS = (String)alParam.get(0).m_objValue;
			dp.m_strDataset = (String)alParam.get(1).m_objValue;
			
			portTo.SetName(alParam.get(1).m_objValue + "@" + alParam.get(0).m_objValue);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	String GetDescription(String strSemiVariogramModel)
	{
		if (strSemiVariogramModel.equals("Spherical"))
			return "y = Nugget + Sill * (1.5 * x / Range - 0.5 * (x * x * x) / (Range * Range * Range))";
		else if (strSemiVariogramModel.equals("Exponential"))
			return "y = Nugget + Sill * (1.0 - exp(-(3.0 * x) / Range))";
		else if (strSemiVariogramModel.equals("Gaussian"))
			return "y = Nugget + Sill * (1.0 - exp(-(9.0 * x * x) / (Range * Range)))";
		else if (strSemiVariogramModel.equals("LinearRegression"))
			return "y = Nugget + b * x";
		else if (strSemiVariogramModel.equals("ExponentialRegression"))
			return "y = Nugget * exp(b * x)";
		else if (strSemiVariogramModel.equals("PowerRegression"))
			return "y = Nugget + a * pow(x, b)";
		return "";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			//-------------------------------------------------------------------------------------------------------------------
			Model2 model = ((Model2)m_model);
			Workspace ws = model.m_ws;
			
			DatasetPort dp = (DatasetPort)m_alInputPort.get(0);
			String strOptionalValues = model.GetFields(dp.m_strDS, dp.m_strDataset, false);
			if (m_strField.isEmpty() && !strOptionalValues.isEmpty())
				m_strField = strOptionalValues.split("\\|")[0];
			
			Datasource ds = dp.m_strDS.isEmpty() ? null : ws.GetDatasource(dp.m_strDS);
        	if (ds != null)
        	{
        		DatasetVector dv = (DatasetVector)ds.GetDataset(dp.m_strDataset);    
        		if (dv != null)
        		{
					if (m_rcBounds.isEmpty())
						m_rcBounds = dv.GetBounds();
        		}
        	}
        	
        	String strDatasources = model.GetWritableDatasources(true);
        	if (m_strDSRaster.isEmpty() && !strDatasources.isEmpty())
        		m_strDSRaster = strDatasources.split("\\|")[0];
        	//-------------------------------------------------------------------------------------------------------------------
			for (int i = 0; i < m_alInputPort.size(); i++)
			{
        		dp = (DatasetPort)m_alInputPort.get(i);
				alParam.add(new Param(Param.Datasource, dp.m_strDS, "数据源", "数据源别名", "输入端/点数据", Param.EditType.ReadOnly));
				alParam.add(new Param(Param.Dataset, dp.m_strDataset, "数据集", "数据集名称", "输入端/点数据", Param.EditType.ReadOnly));
			}
			
			alParam.add(new Param(Param.Field, m_strField, "插值字段", "", "参数", Param.EditType.SelectOrInput, strOptionalValues));
			alParam.add(new Param(Param.Bounds, String.format("%f %f %f %f", m_rcBounds.getMinX(), m_rcBounds.getMinY(), m_rcBounds.getMaxX(), m_rcBounds.getMaxY()), 
					"插值范围", "left bottom right top", "参数", Param.EditType.FixedFloatArray));
			alParam.add(new Param(Param.CellSize, m_strCellSize, "插值分辨率", "x y", "参数", Param.EditType.FixedFloatArray));		
			alParam.add(new Param(Param.ValueType, m_strCellValueType, "栅格值类型", "", "参数", Param.EditType.Select, "UInt8|Int8|UInt16|Int16|UInt32|Int32|Single|Double"));
			alParam.add(new Param("SearchMode", m_strSearchMode, "搜索模式", "", "参数", Param.EditType.Select, "RadiusVariable|RadiusFixed|All"));
			
			alParam.add(new Param("PointCount", String.format("%d", m_nPointCount), "搜索点数", "", "参数/RadiusVariable", m_strSearchMode.equals("RadiusVariable") ? Param.EditType.UInt : Param.EditType.ReadOnly));
			alParam.add(new Param("MaxRadius", String.format("%d", m_nMaxRadius), "最大半径", "", "参数/RadiusVariable", m_strSearchMode.equals("RadiusVariable") ? Param.EditType.UInt : Param.EditType.ReadOnly));
			
			alParam.add(new Param("Radius", String.format("%d", m_nRadius), "搜索半径", "", "参数/RadiusFixed", m_strSearchMode.equals("RadiusFixed") ? Param.EditType.UInt : Param.EditType.ReadOnly));
			alParam.add(new Param("MinPointCount", String.format("%d", m_nMinPointCount), "最小点数", "", "参数/RadiusFixed", m_strSearchMode.equals("RadiusFixed") ? Param.EditType.UInt : Param.EditType.ReadOnly));
			
			alParam.add(new Param("SemiVariogramModel", m_strSemiVariogramModel, "半变异函数模型", GetDescription(m_strSemiVariogramModel), "参数/半变异函数", Param.EditType.Select, 
					"Spherical|Exponential|Gaussian|LinearRegression|ExponentialRegression|PowerRegression"));
			alParam.add(new Param("Range", String.format("%f", m_dRange), "变程", "区域化变量在空间上具有相关性的范围。", "参数/半变异函数", Param.EditType.Float));
			alParam.add(new Param("Nugget", String.format("%f", m_dNugget), "块金值", "相当于变量纯随机性的部分。 ", "参数/半变异函数", Param.EditType.Float));
			alParam.add(new Param("Sill", String.format("%f", m_dSill), "基台值", "变量在空间上的总变异性大小；块金值和拱高之和。", "参数/半变异函数", Param.EditType.Float));
			alParam.add(new Param("a", String.format("%f", m_a), "系数a", "", "参数/半变异函数", Param.EditType.Float));
			alParam.add(new Param("b", String.format("%f", m_b), "系数b", "", "参数/半变异函数", Param.EditType.Float));
		
			alParam.add(new Param("CrossValidation", m_bCrossValidation ? "true" : "false", "是否交叉验证", "", "参数", Param.EditType.Select, "false|true"));
			alParam.add(new Param("ResidualFileName", m_strResidualFileName, "残差文件", "", "参数", Param.EditType.File, "CSV文件(*.csv)|*.csv|所有文件(*.*)|*.*"));
	
			alParam.add(new Param(Param.Datasource, m_strDSRaster, "数据源", "栅格数据源别名", "输出端", Param.EditType.SelectOrInput, strDatasources));
			alParam.add(new Param(Param.Dataset, m_strDatasetRaster, "数据集", "栅格数据集名称", "输出端"));
			
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
		if (alParam.get(nIndex).m_strName.equals("SearchMode"))
		{
			alParam.get(nIndex + 1).m_eEditType = objValue.equals("RadiusVariable") ? Param.EditType.UInt : Param.EditType.ReadOnly;
			alParam.get(nIndex + 2).m_eEditType = alParam.get(nIndex + 1).m_eEditType;
			alParam.get(nIndex + 3).m_eEditType = objValue.equals("RadiusFixed") ? Param.EditType.UInt : Param.EditType.ReadOnly;
			alParam.get(nIndex + 4).m_eEditType = alParam.get(nIndex + 3).m_eEditType;
			return true;
		}
		else if (alParam.get(nIndex).m_strName.equals("SemiVariogramModel"))
		{
			alParam.get(nIndex).m_strDescription = GetDescription((String)objValue);
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
			int j = 0;
			while (alParam.get(i).m_strName.equalsIgnoreCase(Param.Datasource))
			{
				if (j == m_alInputPort.size())
					m_alInputPort.add(new DatasetPort(this));
				
				DatasetPort dp = (DatasetPort)m_alInputPort.get(j);
				dp.m_strDS = (String)alParam.get(i++).m_objValue;
				dp.m_strDataset = (String)alParam.get(i++).m_objValue;
				dp.SetName(dp.m_strDataset + "@" + dp.m_strDS);

				++j;
			}
			
			m_strField = (String)alParam.get(i++).m_objValue;
			String[] strs = ((String)alParam.get(i++).m_objValue).split(" ");
			m_rcBounds = new Rectangle2D.Double(Double.parseDouble(strs[0]), Double.parseDouble(strs[1]), 
					Double.parseDouble(strs[2]) - Double.parseDouble(strs[0]), Double.parseDouble(strs[3]) - Double.parseDouble(strs[1]));
			m_strCellSize = (String)alParam.get(i++).m_objValue;
			m_strCellValueType = (String)alParam.get(i++).m_objValue;
			m_strSearchMode = (String)alParam.get(i++).m_objValue;
			m_nPointCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_nMaxRadius = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_nRadius = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_nMinPointCount = Integer.parseInt((String)alParam.get(i++).m_objValue);

			m_strSemiVariogramModel = (String)alParam.get(i++).m_objValue;	
			m_dRange = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dNugget = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_dSill = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_a = Double.parseDouble((String)alParam.get(i++).m_objValue);
			m_b = Double.parseDouble((String)alParam.get(i++).m_objValue);
			
			m_bCrossValidation = Boolean.parseBoolean((String)alParam.get(i++).m_objValue);
			m_strResidualFileName = (String)alParam.get(i++).m_objValue;
			
			m_strDSRaster = (String)alParam.get(i++).m_objValue;
			m_strDatasetRaster = (String)alParam.get(i++).m_objValue;
			
			m_alOutputPort.get(0).SetName(m_strDatasetRaster + "@" + m_strDSRaster);
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
			Model2 model = ((Model2)m_model);
			DatasetPort dp = (DatasetPort)m_alInputPort.get(0);
			if (!model.CheckInput(this, 0, dp.m_strDS, dp.m_strDataset, false))
				return false;
			Workspace ws = model.m_ws;
			
			Analyst analyst = Analyst.CreateInstance("OrdinaryKriging", ws);
	        String str = "{\"Datasource\":\"" + dp.m_strDS + "\",\"Dataset\":\"" + dp.m_strDataset + "\"}";
	        analyst.SetPropertyValue("Point", str); //站点数据
	
	        analyst.SetPropertyValue("Field", m_strField); //插值字段
	
	        str = String.format("{\"left\":%f,\"bottom\":%f,\"right\":%f,\"top\":%f}", m_rcBounds.getMinX(), m_rcBounds.getMinY(), m_rcBounds.getMaxX(), m_rcBounds.getMaxY());
	        analyst.SetPropertyValue("Bounds", str); //插值范围
	
	        analyst.SetPropertyValue("CellSize", m_strCellSize); //栅格分辨率
	        analyst.SetPropertyValue("CellValueType", m_strCellValueType); //栅格值类型
	
	        analyst.SetPropertyValue("SearchMode", m_strSearchMode); 
	        if (m_strSearchMode.equalsIgnoreCase("RadiusVariable"))
	        {
		        str = String.format("{\"PointCount\":%d,\"MaxRadius\":%d}", m_nPointCount, m_nMaxRadius);
		        analyst.SetPropertyValue("RadiusVariable", str);
	        }
	        else if (m_strSearchMode.equalsIgnoreCase("RadiusFixed"))
	        {
		        str = String.format("{\"Radius\":%d,\"MinPointCount\":%d}", m_nRadius, m_nMinPointCount);
		        analyst.SetPropertyValue("RadiusFixed", str);
	        }
	
	        analyst.SetPropertyValue("SemiVariogramModel", m_strSemiVariogramModel);
	        analyst.SetPropertyValue("Range", String.format("%f", m_dRange));
	        analyst.SetPropertyValue("Nugget", String.format("%f", m_dNugget));
	        analyst.SetPropertyValue("Sill", String.format("%f", m_dSill));
	        analyst.SetPropertyValue("a", String.format("%f", m_a));
	        analyst.SetPropertyValue("b", String.format("%f", m_b));
	
	        analyst.SetPropertyValue("CrossValidation", m_bCrossValidation ? "true" : "false"); //是否交叉验证，默认值为false
	        
	        String strResidualFileName = m_strResidualFileName;
			if (!new File(strResidualFileName).isAbsolute())
				strResidualFileName = new File(m_model.m_strFileName).getParent() + "/" + strResidualFileName;
			analyst.SetPropertyValue("ResidualFileName", strResidualFileName); //残差文件名
	
	        str = "{\"Datasource\":\"" + m_strDSRaster + "\",\"Dataset\":\"" + m_strDatasetRaster + "\"}";
	        analyst.SetPropertyValue("Raster", str);
	
	        boolean bResult = analyst.Execute();
	        if (bResult)
	        {
	        	if (m_bCrossValidation)
	        	{
	        		m_model.OutputLog(Model.LogLevel.Info, String.format("\"%s-%d\"模块\"交叉验证结果：", GetNestedName(), GetID()));
	        		str = analyst.GetPropertyValue("CrossValidationResult");
	        		JSONObject jsonObj = new JSONObject(str);
	        		m_model.OutputLog(Model.LogLevel.Info, "平均误差：" + jsonObj.getString("ME"));
	        		m_model.OutputLog(Model.LogLevel.Info, "平均绝对误差：" + jsonObj.getString("MAE"));
	        		m_model.OutputLog(Model.LogLevel.Info, "均方根误差：" + jsonObj.getString("RMSE"));
	        		m_model.OutputLog(Model.LogLevel.Info, "决定系数：" + jsonObj.getString("R2"));
	        	}
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
	
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		alParam.add(new Param(Param.Datasource, m_strDSRaster));
		alParam.add(new Param(Param.Dataset, m_strDatasetRaster));
		return alParam.size();
	}
	
	String m_strField;
	Rectangle2D m_rcBounds;
	String m_strCellSize;
	String m_strCellValueType;
	String m_strSearchMode;
	
	int m_nPointCount;
	int m_nMaxRadius;
	
	int m_nRadius;
	int m_nMinPointCount;
	
	String m_strSemiVariogramModel;
	double m_dRange;
	double m_dNugget; 
	double m_dSill;
	double m_a, m_b;
	
	boolean m_bCrossValidation;
	String m_strResidualFileName;
	
	String m_strDSRaster;
	String m_strDatasetRaster;
}
