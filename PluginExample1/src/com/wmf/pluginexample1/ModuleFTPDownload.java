//-------------------------------------------------------------
// \project PluginExample1
// \file ModuleFTPDownload.java
// \brief FTP下载模块
// \author 王庆飞
// \date 2017-6-27
// \attention
// Copyright(c) WeatherMap Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

package com.wmf.pluginexample1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.wmf.model.*;

public class ModuleFTPDownload extends Module 
{
	public ModuleFTPDownload(int nID) 
	{
		super(nID);
		
		m_alOutputPort.add(new Port(this));
    }

	public String GetGroupName()
	{
		return "基本模块";
	}
	public String GetName()
	{
		return "FTP下载";
	}
	public String GetDescription()
	{
		return "";
	}
	
	//用于 参数->XML 等
	public int GetParam(ArrayList<Param> alParam)
	{
		try
		{
			int nOffset = super.GetParam(alParam);
			
			alParam.add(new Param("Hostname", m_strHostname, "主机名", "", "连接参数"));
			alParam.add(new Param("Port", String.format("%d", m_nPort), "端口", "", "连接参数", Param.EditType.UInt));
			alParam.add(new Param("Username", m_strUsername, "用户名", "", "登录参数"));
			alParam.add(new Param("Password", m_strPassword, "密码", "", "登录参数"));
			alParam.add(new Param("Encoding", m_strEncoding, "编码", "FTP服务器系统编码", "", Param.EditType.SelectOrInput, "GBK|UTF-8"));
			
			alParam.add(new Param("Pathname", m_strPathname, "路径名", "例如：/download/test/", ""));
			alParam.add(new Param("FileName", m_strFileName, "文件名", "支持时间变量、通配符", ""));
			alParam.add(new Param("DstDir", m_strDstDir, "目标目录", "", "", Param.EditType.Dir));
			
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
			m_strHostname = (String)alParam.get(i++).m_objValue;
			m_nPort = Integer.parseInt((String)alParam.get(i++).m_objValue);
			m_strUsername = (String)alParam.get(i++).m_objValue;
			m_strPassword = (String)alParam.get(i++).m_objValue;
			m_strEncoding = (String)alParam.get(i++).m_objValue;
			
			m_strPathname = (String)alParam.get(i++).m_objValue;
			m_strFileName = (String)alParam.get(i++).m_objValue;
			m_strDstDir = (String)alParam.get(i++).m_objValue;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return i;	
	}
	
	boolean Download(FTPClient ftpc, String strPathname, String strFileName, String strDstDir)
	{
		try
		{
			if (!ftpc.changeWorkingDirectory(new String(strPathname.getBytes(m_strEncoding), "iso-8859-1")))
				return false;
			ftpc.enterLocalPassiveMode();
			
			FTPFile[] files = ftpc.listFiles();
			for (FTPFile file : files)
			{
				String strName = file.getName();
				if (file.isFile())
				{
					if (strName.matches(strFileName))
					{
						FileOutputStream fos = new FileOutputStream(new File(strDstDir + "/" + strName));
						ftpc.retrieveFile(new String(strName.getBytes(m_strEncoding), "iso-8859-1"), fos);
						fos.close();
						m_ll.add(strDstDir + "/" + strName);
					}
				}
				else if (file.isDirectory())
				{
					String str = strDstDir + "/" + strName;
					File fileDir = new File(str);
					if (!fileDir.exists())
						fileDir.mkdir();
					Download(ftpc, strPathname + strName + "/", strFileName, str);
				}
			}
			
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
	
	public boolean Execute()
	{
		m_ll.clear();
		boolean bResult = false;
		FTPClient ftpc = new FTPClient();
		try
		{
			ftpc.connect(m_strHostname, m_nPort);
			if (!FTPReply.isPositiveCompletion(ftpc.getReplyCode()))
			{
				ftpc.disconnect();
				return false;
			}
			
			if (!ftpc.login(m_strUsername, m_strPassword))
			{
				ftpc.disconnect();
				return false;
			}
			ftpc.setControlEncoding(m_strEncoding);
			ftpc.setFileType(FTPClient.BINARY_FILE_TYPE);  
			
			String strFileName = m_model.FormatDate(m_strFileName, false);
			//------------------------------------------------------------------------
			//构造文件名正则表达式
			int i = 0, j = 0;
			char chMeta[] = {'^', '$', '(', ')', '[', ']', '{', '}', '+', '.'};
			String strRegex = "^";
			for (i = 0; i < strFileName.length(); i++)
			{
				char ch = strFileName.charAt(i);
				if (ch == '*')
				{
					strRegex += "[\\s\\S]*";
				}
				else if (ch == '?')
				{
					strRegex += "[\\s\\S]{1}";
				}
				else
				{
					for (j = 0; j < chMeta.length; j++)
					{
						if (ch == chMeta[j])
						{
							strRegex += "\\";
							break;
						}
					}
					strRegex += ch;
				}
			}
			strRegex += "$";
			//------------------------------------------------------------------------
			File fileDir = new File(m_strDstDir);
			if (!fileDir.exists())
				fileDir.mkdirs();
			bResult = Download(ftpc, m_strPathname, strRegex, m_strDstDir);
			
			ftpc.logout();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				if (ftpc.isConnected())
					ftpc.disconnect();
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
		OnParamChanged();
		return bResult;
	}
	public int GetOutputParam(Port port, ArrayList<Param> alParam)
	{
		int i = FindPort(port, false);
		if (i == -1)
			return 0;
		alParam.add(new Param("FileNames", m_ll));
		return alParam.size();
	}
	
	protected String m_strHostname = "";
	protected int m_nPort = 21;
	protected String m_strUsername = "", m_strPassword = "";
	protected String m_strEncoding = "GBK";
	protected String m_strPathname;
	protected String m_strFileName;
	protected String m_strDstDir;
	
	protected LinkedList<String> m_ll = new LinkedList<String>();
}
