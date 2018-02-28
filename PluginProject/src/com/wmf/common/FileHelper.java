package com.wmf.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * @author 杠上花
 */
public class FileHelper {
	/**
	 * @作者:wangkun
	 * @日期:2017年8月8日
	 * @修改日期:2017年8月8日
	 * @参数:
	 * @返回:
	 * @说明:读文件
	 */
	public String readFile(String strFile){
		File file = new File(strFile);
		if(!file.exists()){
			System.out.println("文件不存在!");
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try{
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader read = new InputStreamReader(fis,"utf-8");
			BufferedReader bufferedReader = new BufferedReader(read);
			
			String lineTxt = null;
			while((lineTxt = bufferedReader.readLine()) != null){
				sb.append(lineTxt);
	                }
			read.close();
			fis.close();
		}
		catch(Exception ex){
			
		}
		return sb.toString();
	}
}
