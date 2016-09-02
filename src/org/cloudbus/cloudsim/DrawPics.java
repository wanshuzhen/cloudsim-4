package org.cloudbus.cloudsim;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

public class DrawPics {
	
	public static void main(String []args) throws ParseException{
		System.out.println(String.format("%.4f", new Double(45.34636)));
		
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(2007, 8, 1, 0, 0, 0);
		System.out.println(new Date(calendar.getTimeInMillis()));//设置时间为wiki的2007-9-1
		
		DateFormat format = DateFormat.getDateInstance();
		System.out.println(format.isLenient());
		format.setLenient(false);
		System.out.println(format.parse("2015-04-04 01:01:01"));
		System.out.println(format.parse("2015-04-04-01-01-01").compareTo(new Date(calendar.getTimeInMillis())));
		calendar.add(Calendar.SECOND, 100);//增加5秒
		System.out.println(new Date(calendar.getTimeInMillis()));
		
		DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(format2.parse("2015-04-04 01:01:01"));
		System.out.println();
		
		
		ArrayList<Date> list = new ArrayList<Date>();//一下几行代码是对时间排序的测试，很简单。
		list.add(new Date(calendar.getTimeInMillis()));
		list.add(new Date(Calendar.getInstance().getTimeInMillis()));
		list.add(format.parse("2015-04-04"));
		list.sort(new Comparator<Date>(){

			@Override
			public int compare(Date o1, Date o2) {
				return o1.compareTo(o2);
			}
			
		});
		for(Date date: list){
			System.out.println(date);
		}
	}
	
	public static void drawElec(List<Double> Power, double lastClock, double lastPower ,String city) throws FileNotFoundException, IOException {
		
		 int width = 800 ;
		 int height = 600 ;
		 BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		 
		 Graphics g = image.getGraphics();
		 g.setColor(new Color(0,0,0));
		 g.fillRect(0, 0, width, height);
		 g.setColor(new Color(255,255,255));
		 g.setFont(new Font("微软雅黑",Font.PLAIN,22));
		 String s = null;
		 switch(city){
		 case "SY":
			 s = "沈阳";break;
		 case "BJ":
			 s = "北京";break;
		 case "SH":
			 s = "上海";break;
		 case "CD":
			 s = "成都";break;
		 }
		 g.drawString(s + "Datacenter电量变化曲线", 10, 20);
		 g.drawString("电量(w)", 20, 45);
		 g.drawString("小时(h)", 550, 510);
		 
		 int leftX = 30;
		 int leftY = 55;
		 int rightX = 550;
		 int rightY = 500;
		 g.drawLine(leftX, rightY, rightX, rightY);//x轴
		 g.drawLine(leftX, leftY, leftX, rightY);//y轴
		 
		 while(lastClock > 0){
			 lastClock -= 3600;
		 }

		 if(Power.size() > 1){
			 int perwidth = rightX - leftX;
			 perwidth /= ( Power.size() - 1 );//...强制变成int...
			 int perheight = rightY - leftY ;
			 perheight /= (Power.size() - 1);
			 int i = 0 ;
			 for(i = 0 ; i < Power.size() - 1  ;i++){//画n-1个线(不包括最后) 共n个点
				 g.drawLine(leftX + i*perwidth, rightY - i*perheight, leftX + (i+1)*perwidth, rightY - (i+1)*perheight);
				 g.drawString("("+i+", "+String.format("%.2f", Power.get(i)).toString()+")", leftX + i*perwidth, rightY - i*perheight);
			 }
			 g.drawString("("+i+", "+String.format("%.2f", Power.get(i)).toString()+")", leftX + i*perwidth, rightY - i*perheight);
			 g.dispose();
		 }
		 ImageIO.write(image, "JPEG", new FileOutputStream(s + "Datacenter电量曲线.jpeg"));
		 
	}

}
