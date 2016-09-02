package org.cloudbus.cloudsim;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.MyCloudlet;
import org.cloudbus.cloudsim.power.MyPowerBroker;
import org.cloudbus.cloudsim.power.MyVm;
import org.cloudbus.cloudsim.power.Mycenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimpleWattPerMipsMetric;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_BAZAR;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.xml.DvfsDatas;

public class NewTest {

	//任务大小每个要不一样。否则贪心是白扯。
	//不能分配到其他center.,鉴于如此，看来必须要重写一个broker类了。明天再说。
	//重新写了个类MyPowerBroker,.但是看到源代码竟然特别有趣，倒是不如直接用原先的MyPowerBroker，因此直接移花接木就可以了。就做了这么一点微小的工作
	//8-21不行。一定要重写broker！否则只能按照默认的方式分配虚拟机给center,如果有可能是不是还要重写一个Vm类把datacenter的id传参进去？。。
	//但是也不一定。因为MyPowerBroker->第二层processResourceCharacteristics(SimEvent ev)的参数是ev。。。看看能不能在ev上做手脚吧。
	//回去把程序的调试从创建一个entity子类比如center和broker开始直到Cloudsim.startsimulation()或者更往后要走一遍，要不根本就不知道这entity和event是啥时候
	//加进去的，这如何下手啊。
	
	//卧槽。。真是不妙  根本 除了找到vm的dest是什么时候传入SimEvent 就根本没有任何vm可以传入目标center的机会。只能从这里钻空子了。。话说cloudsim竟然没有提供定向
	//在center创建vm？？？简直神不符合常理啊！！
	//分析出来了一部分。。。应该是在MyPowerBroker.processEvent(ev)之前！应该是在之前就产生了分配多少个vm的策略？？(大雾   还有待分析  
	//那就得看最初始，就是刚刚MyPowerBroker.startEntity()开始之后发生什么了！
	//MDZZ。。。完全估算错误！！看这源代码的意思，clousim闹了半天根本就没打算做定向在center创建vm的策略吗？！！
	//因为一开始就没打算传入center的name或者id,所以也不应该从初始看了。。应该在。。？
	//决定了！！不是重写broker而是重写vm！加上一个string targetcenter，在第三层vmcreate上直接忽略原先第二层默认的createVmsInDatacenter(getDatacenterIdsList().get(0));
	//然后直接在上边改掉center[0]的魔咒。。2333 这是明天的活啦！..但是这么说来，怎么也得重写broker的第三层才是。
	//不好整！！应该先把(int[])getdata()这个玩应弄明白！那东西为啥会转成int[]？？
	//又被打击了。只能从第二层改，而且它的设置是，只要是一个broker，所有它拥有的vm就全在一个center中批量创建。这就好纠结了。。
	//虽然贪心变得作用微乎其微，毕竟接收的量少了。但是还是要一个线程创建4个broker分别属于4个center吧。它broker发收机制实在是不错，当然也难改，我并不想要动它。
	//还需要动些手脚把vm的center传进去，这个超麻烦。这一个搞不好，可能还要重写simEvent。。。。
	//第二层的ev貌似没用到dest参数。说不好可以在datacenter里边下手。注意是datacenter!!!!因为是由它传过来的event.
	
	//不行。还是缝合的死死。就只能这样了！最终方案决定！
	//不改simevent,不改datacenter,只改vm和broker。在【第二层】get(0)那里偷换成vm的targetdatacenter的信息，
	//其次：1.在之前就用4个broker筛选好了不同center的vm,直接添加。但是很不爽，却肯定是对的。
	//2.只用1个broker，但是把第二层的代码改成：接收完4个center的characteristics的信息之后，就开始每个vm读取targetdatacenter信息，并且把批量调用的代码篡改。
	
	private static List<Mycenter> DatacenterList = new ArrayList<Mycenter>();
	private static List<MyPowerBroker> MyPowerBrokerList = new ArrayList<MyPowerBroker>();//接收传出的信息(模拟完毕)
//	private static MyPowerBroker broker ;
	private static ArrayList<Date> timeList = new ArrayList<Date>();
	private static List<MyCloudlet> allcloudletList = new ArrayList<MyCloudlet>();//根本用不上 用这个玩意submitcloudletlist之后就没有卵用了  竟然才看出来。。
	private static List<MyVm> allvmlist = new ArrayList<MyVm>();//恐怕用不上这个吧。不，有点用。。。帮忙记录vm的数量。。然后分发给vm id...
	
	private static List<MyVm> createVM(int userId, int nb_vm ,int type, String targetDatacenter) {

		List<MyVm> currentList = new ArrayList<MyVm>();

		long size = 0; 
		int ram = 0; 
		int mips = 0;
		long bw = 0;
		int pesNumber = 0; 
		String vmm = null; 
		
		switch(type){
		case 0:
			size = 2500; 
			ram = 128; 
			mips = 20;
			bw = 1000;
			pesNumber = 1; 
			vmm = "Xen"; 
			break;
		case 1:
			size = 1500; 
			ram = 128; 
			mips = 40;
			bw = 2000;
			pesNumber = 1; 
			vmm = "BetterXen"; 
			break;
		case 2:
			size = 2000; 
			ram = 256; 
			mips = 60;
			bw = 2000;
			pesNumber = 1; 
			vmm = "MoreXen"; 
			break;
		case 3:
			size = 2500; 
			ram = 256; 
			mips = 80;
			bw = 5000;
			pesNumber = 1; 
			vmm = "BestXen"; 
			break;
		}
		

		for(int i=0;i<nb_vm;i++){
			MyVm vm = new MyVm(allvmlist.size(), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared(),targetDatacenter);//���space��time��
			allvmlist.add(vm);
			currentList.add(vm);
		}
		return currentList;
	}
	
	
	public static void main(String[] args) {

		Log.printLine("Starting mysimple...");
		
		try {
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
		
			//无限读取的话，一定要修改Cloudsim.terminateAt,让其大于0并且别忘了是100:1.而且程序内部维护的时间流动的原理我也不知道，只能简单的使用calendar.add()。
			
			int num_user = timeList.size(); // 这个要更改。
			
			Calendar calendar = Calendar.getInstance();
			calendar.clear();
			calendar.set(2007, 8, 1, 0, 0, 0);//设置时间为wiki的2007-9-1！！//说实话要是能让cloudsim空转正好100：1的时间流速运行1个月就好了 数据全都读入 但是要炸23333
			
			boolean trace_flag = false; // mean trace GridSim events

			CloudSim.init(num_user, calendar, trace_flag);

			createDatacenter();
			for(Mycenter datacenter : DatacenterList){
				datacenter.setDisableMigrations(true);//别忘了。
			}
			
			MyPowerBroker broker = null;
			try {
				broker = new MyPowerBroker("broker_"+MyPowerBrokerList.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
			MyPowerBrokerList.add(broker);//内部类可以调用外部类中的private static
	
			
			//Create VMs and Cloudlets and send them to broker
			
			List<MyVm> vmlist = new ArrayList<MyVm>();
			List<MyCloudlet> missionList = new ArrayList<MyCloudlet>();
					
			for(int i = 0 ; i < 1 ; i ++){
				int vms = (int)(Math.random() * 7 + 1 ) ;//每个人最多创建8台虚机。。。以后再改
				vmlist.addAll(createVM(broker.getId(), vms*50/100 ,0, "SY"));//vms*50/100的虚拟机是0号机。。。瞎编的数据 谁知道他会请求什么
				vmlist.addAll(createVM(broker.getId(), vms*30/100 ,1, "BJ"));
				vmlist.addAll(createVM(broker.getId(), vms*20/100 ,2, "SH"));
				vmlist.addAll(createVM(broker.getId(), vms-vms*50/100-vms*30/100-vms*20/100,3, "CD"));
				//submit vm list to the broker
				
				int missionNum = (int)(Math.random() * 10 );//最多不超过10个？好像太多了。。。以后再改吧。
				missionList.addAll(createCloudletList(broker.getId(), missionNum, new Date(CloudSim.getSimulationCalendar().getTimeInMillis())));//测试用例没有请求时间。。。遂初始化为模拟开始时间
				//submit cloudletlist to the broker?.,
			}
				
				//直接使用贪心！因为在外边传递时直接就排好了序，进入内部默认又是轮循，所以直接相当于贪心了。
				vmlist.sort(new Comparator<MyVm>(){
					
					@Override
					public int compare(MyVm o1, MyVm o2) {
						return (int)((o1.getCurrentRequestedMaxMips()) - (o2.getCurrentRequestedTotalMips())) ;
					}
					
				});
				missionList.sort(new Comparator<MyCloudlet>(){
					
					@Override
					public int compare(MyCloudlet o1, MyCloudlet o2) {
						return (int)(o1.getCloudletLength() - o2.getCloudletLength());
					}
					
				});
				broker.submitVmList(vmlist);
				broker.submitCloudletList(missionList);
				
			
//			firstOrderOfVms();
//			greedy();
			
			new Thread(new NewTest2().new Transmitter()).start();
			Thread.sleep(1000);
			
			double lastClock = CloudSim.startSimulation();

			// Final step: Print results when simulation is over

			List<Cloudlet> newList = MyPowerBrokerList.get(0).getCloudletReceivedList();
			for(int i=1 ; i < MyPowerBrokerList.size(); i++)//查看源代码发现。其实就是cloudlet全都被吞进去，又被吐出来，只不过中间加了一堆时间信息。好比喻。。(逃
				newList.addAll(MyPowerBrokerList.get(i).getCloudletReceivedList());
			//按照请求时间排序
			newList.sort(new Comparator<Cloudlet>(){

				@Override
				public int compare(Cloudlet o1, Cloudlet o2) {
					return ((MyCloudlet)o1).getRequestDate().compareTo(((MyCloudlet)o2).getRequestDate());
				}
				
			});//继续使用匿名内部类排序！。。。简直是越来越熟练了。。
			
			
			Log.printLine("Received " + newList.size() + " cloudlets");

			CloudSim.stopSimulation();

			printCloudletList(newList);
			Log.printLine();

			double power = 0 ;
			//最后一刻的时间是lastClock*100/3600.最后一刻的FinalPower是power.
			for(Mycenter center : DatacenterList){
				power += center.getPower();
				//最终时间的添加需要继续修改源码。。
				List<Double> temp = center.getPowerList();//这里的时间流速是100:1.单位为s。
				temp.add(0, 0.0);
				for(Double pow : temp){
					System.out.println(pow);
				}
				System.out.println();
				DrawPics.drawElec(temp, lastClock, power, center.getName());//打印电量总分布
				
			}
			
			Log.printLine(String.format("Total simulation time: %.2f sec", lastClock));
			Log.printLine(String.format("Power Sum :  %.8f W", power ));
			Log.printLine(String.format("Power Average : %.8f W", power / (lastClock*100)));
			Log.printLine(String.format("Energy consumption: %.8f Wh", (power / (lastClock*100)) * (lastClock*100 / 3600)));

			Log.printLine();
			
			DatacenterList.get(0).printDebts();
			DatacenterList.get(1).printDebts();
			DatacenterList.get(2).printDebts();
			DatacenterList.get(3).printDebts();
			
			for(Cloudlet cloudlet : newList){
				System.out.println(((MyCloudlet)cloudlet).getWaitTime()+"..."+((MyCloudlet)cloudlet).getRequestDate());
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}		
	}
	
	private static void createDatacenter(){
		DatacenterList.add(createCenter("SY"));
		DatacenterList.add(createCenter("BJ"));
		DatacenterList.add(createCenter("SH"));
		DatacenterList.add(createCenter("CD"));
	}
	
	private static Mycenter createCenter(String name){
		List<PowerHost> hosts = new ArrayList<PowerHost>();
		
		long storage = 1000000;//1T
		int bw = 300000;
		int ram = 0 ;
		int num = 0 ;
		int mips = 0 ;
		switch(name){
		case "SY":
			ram = 4096;
			num = 2 ;//改成2压力小很多。别忘改回200！
			mips = 400 ;
			break;
		case "BJ":
			ram = 8192;
			num = 4 ;//改成4压力小很多。别忘改回200！
			mips = 800 ;
			break;
		case "SH":
			ram = 8192;
			num = 1;//改成1压力小很多。别忘改回200！
			mips = 200 ;
			break;
		case "CD":
			ram = 2048;
			num = 2;//改成2压力小很多。别忘改回200！
			mips = 200 ;
			break;
		}
		//按照不明配置
		boolean enableDVFS = true; 
		ArrayList<Double> freqs = new ArrayList<>(); 
		freqs.add(59.925);
		freqs.add(69.93); 
		freqs.add(79.89);
		freqs.add(89.89);
		freqs.add(100.0);
		
		HashMap<Integer,String> govs = new HashMap<Integer,String>();  // Define wich governor is used by each CPU
		govs.put(0,"ondemand");  // CPU 1 use OnDemand Dvfs mode
		
		DvfsDatas ConfigDvfs ;

		for (int i = 0; i < num; i++) {
			ConfigDvfs = new DvfsDatas();
			HashMap<String,Integer> tmp_HM_OnDemand = new HashMap<>();
			tmp_HM_OnDemand.put("up_threshold",95);
			tmp_HM_OnDemand.put("sampling_down_factor",100);
			HashMap<String,Integer> tmp_HM_Conservative = new HashMap<>();
			tmp_HM_Conservative.put("up_threshold",80);
			tmp_HM_Conservative.put("down_threshold",20);
			tmp_HM_Conservative.put("enablefreqstep",0);
			tmp_HM_Conservative.put("freqstep",5);
			HashMap<String,Integer> tmp_HM_UserSpace = new HashMap<>();
			tmp_HM_UserSpace.put("frequency",3);
			ConfigDvfs.setHashMapOnDemand(tmp_HM_OnDemand);
			ConfigDvfs.setHashMapConservative(tmp_HM_Conservative);
			ConfigDvfs.setHashMapUserSpace(tmp_HM_UserSpace);

			//HostDatas tmp_host = vect_hosts.get(i);
			List<Pe> peList = new ArrayList<Pe>();//ÿ��host�ı���
			peList.add(new Pe(i,new PeProvisionerSimple(mips),freqs,govs.get(0), ConfigDvfs));
			hosts.add(new PowerHost(i,new RamProvisionerSimple(ram),new BwProvisionerSimple(bw),storage,peList,new VmSchedulerTimeShared(peList),new PowerModelSpecPower_BAZAR(peList),false,
					enableDVFS));
		}
		
		String arch = "x86" ;
		String os = "Linus";
		String vmm = "Xen" ;
		double time_zone = 8.0 ;//8时区
		double cost = 3.0;             
		double costPerMem = 0.05;		
		double costPerStorage = 0.001;	
		double costPerBw = 0.1;			
		LinkedList<Storage> storageList = new LinkedList<Storage>();
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hosts, time_zone, cost, costPerMem, costPerStorage, costPerBw);
		
		Mycenter datacenter = null;
		try {
			datacenter = new Mycenter(name, characteristics, new PowerVmAllocationPolicySimpleWattPerMipsMetric(hosts), storageList, 0.01);
//			Mycenter center = new Mycenter(s, characteristics, new VmAllocationPolicySimple(hosts), storageList, 0);
//			System.out.println(center.getPower());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return datacenter;

	}
	
	private static List<MyCloudlet> createCloudletList(int user_id, int nb_cloudlet, Date date){

		List <MyCloudlet> list = new ArrayList<MyCloudlet>();
		for(int i = 0 ; i < nb_cloudlet ; i ++){
			long length = (long)(500 + 1000 * Math.random());//随机。。。500-1500之间任何一个数字吧。。。以后再改。。
			long fileSize = 300;
			long outputSize = 300;
			int pesNumber = 1 ;
			UtilizationModel utilizationModel = new UtilizationModelFull();
			//测试用例的话，初始为模拟开始的时间。
			MyCloudlet cloudlet = new MyCloudlet(allcloudletList.size(), length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, date);
			cloudlet.setUserId(user_id);
			list.add(cloudlet);
			allcloudletList.add(cloudlet);
		}
		return list;
	}

	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");
				Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
						indent + indent + dft.format(cloudlet.getFinishTime()));
				
			}
		}

	}
	
	class Transmitter implements Runnable{
	
		@Override
		public void run() {
	
	
			long time = 10 ;//10s 在 simulator中
			ListIterator<Date> it1 = timeList.listIterator();
			Date first = null;
			if(it1.hasNext()) first =  it1.next();
			else return;
			
			while(true){
				
				CloudSim.pauseSimulation(time);//别弄错！这个的意思是，在200ms的时候pause一次！按照源代码来看，程序会不断停滞100ms知道resumeSimulation为止。
				while (true) {
					if (CloudSim.isPaused()) {
						break;
					}
					try {
						Thread.sleep(100);//此线程一直休眠到Cloudsim.pause为止！
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				Calendar calendar = Calendar.getInstance();
				calendar.clear();
				calendar.set(2007, 8, 1, 0, 0, 0);
				double clock = CloudSim.clock() * 100;//这里有bug！怎么可能全都加到一个id=7的broker呢！？回来再看
				calendar.add(Calendar.SECOND, (int)clock);
				Date date = new Date(calendar.getTimeInMillis());
				int cnt = 0 ;
				boolean isFinal = false;
//				boolean judgeNext = it1.hasNext();
				List<Date> dateList = new ArrayList<Date>();
				while(date.compareTo(first)>0){
					cnt ++;
					dateList.add(first);//只要cnt++就加到里边
					if(it1.hasNext()) {
						first = it1.next();
					}
					else {
						isFinal = true; 
						break;
					}
					System.out.println(date + " "+ first);
				}
				if(cnt == 0){
					time += 10;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}//我只想让这个线程在模拟中睡10秒 按照100：1  那么就是睡0.1s 即100ms 即：每隔10s发送一次这10s内的请求集合
					
					CloudSim.resumeSimulation();
					
					continue ;
				}
				
				MyPowerBroker broker = null;
				try {
					broker = new MyPowerBroker("broker_"+MyPowerBrokerList.size());
				} catch (Exception e) {
					e.printStackTrace();
				}
				MyPowerBrokerList.add(broker);//内部类可以调用外部类中的private static
		
				
				//Create VMs and Cloudlets and send them to broker
				
				int num_user = cnt;
				List<MyVm> vmlist = new ArrayList<MyVm>();
				List<MyCloudlet> missionList = new ArrayList<MyCloudlet>();
						
				for(int i = 0 ; i < num_user ; i ++){
					int vms = (int)(Math.random() * 7 + 1 ) ;//每个人最多创建8台虚机。。。以后再改
					vmlist.addAll(createVM(broker.getId(), vms*50/100 ,0, "SY"));//vms*50/100的虚拟机是0号机。。。瞎编的数据 谁知道他会请求什么
					vmlist.addAll(createVM(broker.getId(), vms*30/100 ,1, "BJ"));
					vmlist.addAll(createVM(broker.getId(), vms*20/100 ,2, "SH"));
					vmlist.addAll(createVM(broker.getId(), vms-vms*50/100-vms*30/100-vms*20/100,3,"CD"));
					//submit vm list to the broker
					
					int missionNum = (int)(Math.random() * 10 );//最多不超过10个？好像太多了。。。以后再改吧。
					missionList.addAll(createCloudletList(broker.getId(), missionNum, dateList.get(i)));
					//submit cloudletlist to the broker?.,
				}
					
					//直接使用贪心！因为在外边传递时直接就排好了序，进入内部默认又是轮循，所以直接相当于贪心了。
					vmlist.sort(new Comparator<MyVm>(){
						
						@Override
						public int compare(MyVm o1, MyVm o2) {
							return (int)((o1.getCurrentRequestedMaxMips()) - (o2.getCurrentRequestedTotalMips())) ;
						}
						
					});
					missionList.sort(new Comparator<MyCloudlet>(){
						
						@Override
						public int compare(MyCloudlet o1, MyCloudlet o2) {
							return (int)(o1.getCloudletLength() - o2.getCloudletLength());
						}
						
					});
					broker.submitVmList(vmlist);
					broker.submitCloudletList(missionList);
					
					
//					CloudSim.addEntity(broker);//放进CloudSim中。//这句话造成了线程的不安全。

					if(isFinal){
						CloudSim.resumeSimulation();
						break;
					}
					
					time += 10;
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}//我只想让这个线程在模拟中睡10秒 按照100：1  那么就是睡0.1s 即100ms 即：每隔10s发送一次这10s内的请求集合
					
					CloudSim.resumeSimulation();
			}
			
			
		}
		
	}
	
	
//	private static void firstOrderOfVms(){
//		int size = allcloudletList.size();
//		for(int i = 0 ; i < size ; i ++){
//			//��ȫ˳���,��������Ӧ����Ĭ�Ϸ�����
//			allcloudletList.get(i).setVmId(i%allvmlist.size());
//		}
//	}
	
//	private static void greedy(){//̰���㷨��   //不在broker里边弄得话，就是废方法。因为这里只更改了外部allcloudletlist中的匹配，但是真正broker里边要执行的list没有
//											//修改。因此好比扯淡一般。
//		
//		allcloudletList.sort(new Comparator<Cloudlet>(){
//
//			@Override
//			public int compare(Cloudlet o1, Cloudlet o2) {
//				return (int)(o1.getCloudletLength() - o2.getCloudletLength());
//			}
//			
//		});
//		
//		allvmlist.sort(new Comparator<Vm>(){
//
//			@Override
//			public int compare(Vm o1, Vm o2) {
//				return (int)((o1.getCurrentRequestedMaxMips()) - (o2.getCurrentRequestedTotalMips())) ;
//			}
//			
//		});
//		
//		for(int i = 0 ; i < allcloudletList.size() ; i ++){
//			allcloudletList.get(i).setVmId(i%allvmlist.size());
//		}
//	}
	
}
	
