package Socket;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudbus.cloudsim.core.CloudSim;

public class Client {
	
	private static ArrayList<Date> timeList = new ArrayList<Date>();
	
	private static ObjectOutputStream oos = null;

	public static void main(String[] args) throws UnknownHostException, IOException, ParseException {
		
//		Socket socket = new Socket("2001:da8:9000:a811:c930:9882:dcf4:8e41",1000);
		Socket socket = new Socket("172.28.231.241",1000);
		//预备序列化传输到服务器
		oos = new ObjectOutputStream(socket.getOutputStream());
		
		//读入所有文件。
		read();
		
		//发送总请求人数：
		oos.writeObject((Integer)timeList.size());
		
		//定时发送所有序列化Date
		send();
		
		
	}

	private static void send() throws IOException {
		
		final int time = 10 ;
		ListIterator<Date> it1 = timeList.listIterator();
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(2007, 8, 1, 0, 0, 0);
		boolean isFinal = false;
		Date first = null;
		if(it1.hasNext()) first =  it1.next();
		else return;
		
		while(true){
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//我只想让这个线程在模拟中睡10秒 按照100：1  那么就是睡0.1s 即100ms 即：每隔10s发送一次这10s内的请求集合
			
			calendar.add(Calendar.SECOND, time);
			Date date = new Date(calendar.getTimeInMillis());
	//		boolean judgeNext = it1.hasNext();
			List<Date> dateList = new ArrayList<Date>();
			while(date.compareTo(first)>0){//一次全加入就行，多次就不行？*100去掉就不行？？
				dateList.add(first);//只要cnt++就加到里边
				if(it1.hasNext()) {
					System.out.println(date + " "+ first);
					first = it1.next();
				}
				else {
					isFinal = true; 
					break;
				}
			}
			if(dateList.size() == 0){
				continue ;
			}
			else {
				oos.writeObject(dateList);
			}
			if(isFinal){
				oos.close();
				break;
			}
			
		}
		
		
		
		
	}

	public static void read() throws FileNotFoundException, IOException, ParseException {
		
		//批量读取，这是不对的！应该要这个buffer批量读取之后，按照请求时间按照时间模拟。。。用一个TreeSet顺序存储时间吧.
		//然后这个虚拟机还不能停止模拟？一直运行center？
		//循环读取currenttmp  虽然是加密文件  但是名字以后再做修改。
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("currenttmp")));
		String regex = new String("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}");//请求的时间不知道是2008-01-01还是2008/01/01还是01/01/2008...编号或许全是数字吧。
		Pattern p = Pattern.compile(regex);
		String s = new String();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		while((s = reader.readLine())!=null){
			Matcher m = p.matcher(s);
			if(m.find()){
				Date d = format.parse(m.group());
				timeList.add(d);//user。。。这么看来只要知道user有几个就行？user是三无属性啊，知道有多少个就可以？
			}
		}
		reader.close();
		timeList.sort(new Comparator<Date>(){

			@Override
			public int compare(Date o1, Date o2) {
				return o1.compareTo(o2);
			}
			
		});
	}

}
