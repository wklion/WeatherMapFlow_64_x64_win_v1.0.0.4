//-------------------------------------------------------------
// \project WeatherMapFlow DefaultUserInfoFinder
// \file DefaultUserInfoFinder.java
// \brief 
// \author 王庆飞
// \date 2017-2-23
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.defaultuserinfofinder;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.wmf.userinfofinder.*;

class DefaultUserInfo extends UserInfo 
{
	DefaultUserInfo(String strUsername, String strUserGroup, String strPassword, String strRole)
	{
		super(strUsername, strUserGroup, strPassword);
		m_strRole = strRole;
	}
	
	public String m_strRole = "";
}

public class DefaultUserInfoFinder implements UserInfoFinder
{
	public DefaultUserInfoFinder()
	{
		try
		{
			SAXReader saxr = new SAXReader();
			Document doc = saxr.read(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/DefaultUserInfoFinder.xml"), "UTF-8"));
			List<Element> liste = doc.selectNodes("DefaultUserInfoFinder/Roles/Role");
			Iterator<Element> it = liste.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				if (e.attributeValue("Permissions").isEmpty())
					continue;
				
				String[] strs = e.attributeValue("Permissions").split(",");
				LinkedList<String> ll = new LinkedList<String>();
				for (int i = 0; i < strs.length; i++)
					ll.add(strs[i]);
				
				m_hmPermission.put(e.attributeValue("Name"), ll);
			}
			
			liste = doc.selectNodes("DefaultUserInfoFinder/Users/User");
			it = liste.iterator();
			while (it.hasNext())
			{
				Element e = it.next();
				
				if (m_llUserGroup.indexOf(e.attributeValue("Group")) == -1)
					m_llUserGroup.add(e.attributeValue("Group"));
				
				m_hmUserInfo.put(e.attributeValue("Name") + "@" + e.attributeValue("Group"), 
						new DefaultUserInfo(e.attributeValue("Name"), e.attributeValue("Group"), e.attributeValue("Password"), e.attributeValue("Role")));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public List<String> GetUserGroups() throws Exception
	{
		return m_llUserGroup; 
	}
	
	public UserInfo GetUserInfo(String strUsername, String strUserGroup) throws Exception
	{
		String str = strUsername + "@" + strUserGroup;
		if (!m_hmUserInfo.containsKey(str))
			return null;
		return m_hmUserInfo.get(str);
	}
	
	public List<String> GetPermissions(String strUsername, String strUserGroup) throws Exception
	{
		String str = strUsername + "@" + strUserGroup;
		if (!m_hmUserInfo.containsKey(str))
			return null;
		DefaultUserInfo dui = m_hmUserInfo.get(str);
		if (!m_hmPermission.containsKey(dui.m_strRole))
			return null;
		return m_hmPermission.get(dui.m_strRole);
	}
	
	public static void main(String[] args) throws Exception 
	{
		//test
		DefaultUserInfoFinder duif = new DefaultUserInfoFinder();
		UserInfo ui = duif.GetUserInfo("王五", "用户组2");
		List<String> list = duif.GetPermissions("李四", "用户组2");
	}
	
	private HashMap<String, List<String>> m_hmPermission = new HashMap<String, List<String>>();
	LinkedList<String> m_llUserGroup = new LinkedList<String>();
	private HashMap<String, DefaultUserInfo> m_hmUserInfo = new HashMap<String, DefaultUserInfo>();
}
