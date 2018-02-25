//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleCalcTime.java
// \brief 计算时间模块类
// \author 王庆飞
// \date 2016-10-27
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.wmf.model.*;

public class ModuleCalcTime extends Module 
{
	public ModuleCalcTime(int nID) 
	{
		super(nID);
		
		m_ePeriodType = PeriodType.Day;
		m_nPeriodCount = 1;
		m_nHourOffset = -8;
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "计算时间";
	}
	public String GetDescription()
	{
		return "计算模型的\"数据开始时间\"和\"数据结束时间\"。";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			
			alParam.add(new Param("PeriodType", m_ePeriodType.toString(), "时段类型", "", "", Param.EditType.Select, "Hour|Day|Week|TenDays|Month|Year"));
			alParam.add(new Param("PeriodCount", String.format("%d", m_nPeriodCount), "时段个数", "", "", Param.EditType.UInt));
			alParam.add(new Param("HourOffset", String.format("%d", m_nHourOffset), "小时偏移", "", "", Param.EditType.Int));
			
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
			m_ePeriodType = PeriodType.valueOf((String)alParam.get(i++).m_objValue);
			m_nPeriodCount = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_nHourOffset = Integer.parseInt((String)alParam.get(i++).m_objValue);
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
			Calendar c = new GregorianCalendar();
			c.setTime(m_model.m_dateStart);
			c.add(Calendar.HOUR, m_nHourOffset);
			if (m_nHourOffset > 0)
				m_model.m_dateDataStart = c.getTime();
			else
				m_model.m_dateDataEnd = c.getTime();
			
			switch (m_ePeriodType)
			{
			case Hour:
				c.add(Calendar.HOUR, (m_nHourOffset > 0 ? 1 : -1) * m_nPeriodCount);
				break;
			case Day:
				c.add(Calendar.DATE, (m_nHourOffset > 0 ? 1 : -1) * m_nPeriodCount);
				break;
			case Week:
				c.add(Calendar.DATE, (m_nHourOffset > 0 ? 7 : -7) * m_nPeriodCount);
				break;	
			case TenDays:
				{
					for (int i = 0; i < m_nPeriodCount; i++)
					{
						final int nActualMaximum = c.getActualMaximum(Calendar.DAY_OF_MONTH);
						final int nDayOfMonth = c.get(Calendar.DAY_OF_MONTH);
						int nAmount = 0;
						if (m_nHourOffset > 0)
						{
							nAmount = (nDayOfMonth == 20 || nDayOfMonth == 21) ? (nActualMaximum - 20) : 10;
						}
						else 
						{
							nAmount = (nDayOfMonth == nActualMaximum || nDayOfMonth == 1) ? -(nActualMaximum - 20) : -10;
						}
						c.add(Calendar.DATE, nAmount);
					}
				}
				break;
			case Month:
				{
					for (int i = 0; i < m_nPeriodCount; i++)
					{
						GregorianCalendar cTemp = new GregorianCalendar();
						cTemp.setTime(c.getTime());
						cTemp.add(Calendar.DATE, m_nHourOffset > 0 ? 15 : -15);
						final int nActualMaximum = cTemp.getActualMaximum(Calendar.DAY_OF_MONTH);
						c.add(Calendar.DATE, m_nHourOffset > 0 ? nActualMaximum : -nActualMaximum);
					}
				}	
				break;	
			case Year:
				{
					for (int i = 0; i < m_nPeriodCount; i++)
					{
						GregorianCalendar cTemp = new GregorianCalendar();
						cTemp.setTime(c.getTime());
						cTemp.add(Calendar.DATE, m_nHourOffset > 0 ? 180 : -180);
						final int nActualMaximum = cTemp.getActualMaximum(Calendar.DAY_OF_YEAR);
						c.add(Calendar.DATE, m_nHourOffset > 0 ? nActualMaximum : -nActualMaximum);
					}
				}	
				break;		
			default:
				throw new AssertionError();
			}
			if (m_nHourOffset > 0)
				m_model.m_dateDataEnd = c.getTime();
			else
				m_model.m_dateDataStart = c.getTime();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	enum PeriodType
	{
		Hour,
		Day,
		Week,
		TenDays,
		Month,
		Year
	}
	PeriodType m_ePeriodType;
	int m_nPeriodCount;
	int m_nHourOffset;
}
