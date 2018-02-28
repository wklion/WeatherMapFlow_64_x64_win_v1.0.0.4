/**     
 * @公司:	spd
 * @作者: wangkun       
 * @创建: 2017-06-11
 * @最后修改: 2017-06-11
 * @功能: cimiss数据
 **/
package com.wmf.mode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CimissData {
		private String Province;//省
		private String City;//市
		private String Cnty;//区县
		private String Station_Name;//站名
		private String Admin_Code_CHN;//行政区代码 add by lugt 20170930
		private String Station_Id_C;//站号
		private String Station_levl;//站点级别
		private double MAX_TEM_MAX;//最高温
		private double MIN_TEM_MIN;//最低温
		private double TEM;//温度
		private double SUM_PRE;//累积降水
		private double SUM_PRE_1H;//1小时累积降水
		private double SUM_PRE_3H;//3小时累积降水
		private double SUM_PRE_6H;//6小时累积降水
		private double SUM_PRE_12H;//12小时降水
		private double SUM_PRE_24H;//24小时降水
		private double WIN_D_INST_Max;//极大风风向
		private double WIN_S_Inst_Max;//极大风风速
		private int WIN_S_INST_Max_OTime;//极大风出现时间 add by lugt 20170824
		private double VIS;//能见度
		private double Lon;//经度
		private double Lat;//纬度
	
		
		private String Datetime;
		public String getProvince() {
			return Province;
		}
		public void setProvince(String province) {
			Province = province;
		}
		
		public String getAdmin_Code_CHN() {
			return Admin_Code_CHN;
		}
		public void setAdmin_Code_CHN(String admin_Code_CHN) {
			Admin_Code_CHN = admin_Code_CHN;
		}
		public String getDatetime() {
			return Datetime;
		}
		public void setDatetime(String dateTime) {
			Datetime = dateTime;
		}
		public double getSUM_PRE_1H() {
			return SUM_PRE_1H;
		}
		public void setSUM_PRE_1H(double sUM_PRE_1H) {
			SUM_PRE_1H = sUM_PRE_1H;
		}
		public double getSUM_PRE_3H() {
			return SUM_PRE_3H;
		}
		public void setSUM_PRE_3H(double sUM_PRE_3H) {
			SUM_PRE_3H = sUM_PRE_3H;
		}
		public double getSUM_PRE_6H() {
			return SUM_PRE_6H;
		}
		public void setSUM_PRE_6H(double sUM_PRE_6H) {
			SUM_PRE_6H = sUM_PRE_6H;
		}
		public double getSUM_PRE_12H() {
			return SUM_PRE_12H;
		}
		public void setPRE_12H(double pRE_12H) {
			SUM_PRE_12H = pRE_12H;
		}
		public double getSUM_PRE_24H() {
			return SUM_PRE_24H;
		}
		public void setPRE_24H(double pRE_24H) {
			SUM_PRE_24H = pRE_24H;
		}
		public double getSUM_PRE() {
			return SUM_PRE;
		}
		public void setSUM_PRE(double sUM_PRE) {
			SUM_PRE = sUM_PRE;
		}
		public void setSUM_PRE_12H(double sUM_PRE_12H) {
			SUM_PRE_12H = sUM_PRE_12H;
		}
		public void setSUM_PRE_24H(double sUM_PRE_24H) {
			SUM_PRE_24H = sUM_PRE_24H;
		}
		public double getWIN_S_Inst_Max() {
			return WIN_S_Inst_Max;
		}
		public void setWIN_S_Inst_Max(double wIN_S_Inst_Max) {
			WIN_S_Inst_Max = wIN_S_Inst_Max;
		}
		public double getWIN_D_INST_Max() {
			return WIN_D_INST_Max;
		}
		public void setWIN_D_INST_Max(double wIN_D_INST_Max) {
			WIN_D_INST_Max = wIN_D_INST_Max;
		}
		
		public int getWIN_S_INST_Max_OTime() {
			return WIN_S_INST_Max_OTime;
		}
		public void setWIN_S_INST_Max_OTime(int wIN_S_INST_Max_OTime) {
			WIN_S_INST_Max_OTime = wIN_S_INST_Max_OTime;
		}
		public double getMAX_TEM_MAX() {
			return MAX_TEM_MAX;
		}
		public void setMAX_TEM_MAX(double mAX_TEM_MAX) {
			MAX_TEM_MAX = mAX_TEM_MAX;
		}
		public double getMIN_TEM_MIN() {
			return MIN_TEM_MIN;
		}
		public void setMIN_TEM_MIN(double mIN_TEM_MIN) {
			MIN_TEM_MIN = mIN_TEM_MIN;
		}
		public double getTEM() {
			return TEM;
		}
		public void setTEM(double tEM) {
			TEM = tEM;
		}
		public String getCity() {
			return City;
		}
		public void setCity(String city) {
			City = city;
		}
		public String getCnty() {
			return Cnty;
		}
		public void setCnty(String cnty) {
			Cnty = cnty;
		}
		public String getStation_Name() {
			return Station_Name;
		}
		public void setStation_Name(String station_Name) {
			Station_Name = station_Name;
		}
		public String getStation_Id_C() {
			return Station_Id_C;
		}
		public void setStation_Id_C(String station_Id_C) {
			Station_Id_C = station_Id_C;
		}
		public String getStation_levl() {
			return Station_levl;
		}
		public void setStation_levl(String station_levl) {
			Station_levl = station_levl;
		}
		public double getLon() {
			return Lon;
		}
		public void setLon(double lon) {
			Lon = lon;
		}
		public double getLat() {
			return Lat;
		}
		public void setLat(double lat) {
			Lat = lat;
		}
		public double getVIS() {
			return VIS;
		}
		public void setVIS(double vIS) {
			VIS = vIS;
		}
}
