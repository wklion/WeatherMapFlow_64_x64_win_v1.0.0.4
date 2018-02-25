//-------------------------------------------------------------
// \project PluginTest
// \file ModelCoeff.java
// \brief 系数模型类
// \author 王庆飞
// \date 2017-7-19
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.plugintest;

import com.wmf.model.*;

public class ModelCoeff extends Model 
{
	public ModelCoeff() 
	{
    }
	
	public Object GetParam(String strName)
	{
		return new Double(10.0);
	}
	public boolean SetParam(String strName, Object objValue)
	{
		try
		{
			if (strName.equalsIgnoreCase("StationNum"))
			{
				
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
	
	public boolean Execute()
	{
		return true;
	}
}
