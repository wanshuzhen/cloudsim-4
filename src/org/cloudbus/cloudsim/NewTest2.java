package org.cloudbus.cloudsim;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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

public class NewTest2 {

	
	private static List<Mycenter> DatacenterList = new ArrayList<Mycenter>();
	private static List<MyPowerBroker> MyPowerBrokerList = new ArrayList<MyPowerBroker>();//接收传出的信息(模拟完毕)
//	private static MyPowerBroker broker ;
	private static List<MyCloudlet> allcloudletList = new ArrayList<MyCloudlet>();//根本用不上 用这个玩意submitcloudletlist之后就没有卵用了  竟然才看出来。。
	private static List<MyVm> allvmlist = new ArrayList<MyVm>();//恐怕用不上这个吧。不，有点用。。。帮忙记录vm的数量。。然后分发给vm id...
	private static ObjectInputStream ois = null;
	private static PrintWriter pw = null;
	private static int count = 0;
	
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
			mips = 150;
			bw = 1000;
			pesNumber = 1; 
			vmm = "Xen"; 
			break;
		case 1:
			size = 1500; 
			ram = 128; 
			mips = 200;
			bw = 2000;
			pesNumber = 1; 
			vmm = "BetterXen"; 
			break;
		case 2:
			size = 2000; 
			ram = 256; 
			mips = 300;
			bw = 2000;
			pesNumber = 1; 
			vmm = "MoreXen"; 
			break;
		case 3:
			size = 2500; 
			ram = 256; 
			mips = 350;
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
	
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		Log.printLine("Starting mysimple...");

		ServerSocket ss = new ServerSocket(1000);
		Socket s = ss.accept();
		
		ois = new ObjectInputStream(s.getInputStream());
		Integer num = (Integer)ois.readObject();
		System.out.println("received the number !!");
		
		pw = new PrintWriter(s.getOutputStream(),true);
		pw.println("received the number.");//必须用PrintWriter.println才能让client接到消息。write不行。。。。
		
		try {
		
			int num_user = num; // 这个要更改。
			
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
					
			int vmss = 1;
			for(int i = 0 ; i < 1 ; i ++){
				vmlist.addAll(createVM(broker.getId(), vmss ,0, "SY"));
				//submit vm list to the broker
				
				List <MyCloudlet> list = new ArrayList<MyCloudlet>();
				long length = 24 * 60 * 60 * 150;//空转24h
				long fileSize = 300;
				long outputSize = 300;
				int pesNumber = 1 ;
				UtilizationModel utilizationModel = new UtilizationModelFull();
				//测试用例的话，初始为模拟开始的时间。
				MyCloudlet cloudlet = new MyCloudlet(allcloudletList.size(), length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, new Date(CloudSim.getSimulationCalendar().getTimeInMillis()));
				cloudlet.setUserId(broker.getId());
				list.add(cloudlet);
				allcloudletList.add(cloudlet);
				
				missionList.add(cloudlet);
				//submit cloudletlist to the broker?.,
			}
				
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
				double DatacenterLastClock = (center.getLastProcessTime());
				double lastPower = center.getPower();
				DrawPics.drawElec(temp, DatacenterLastClock, lastPower, center.getName());//打印电量总分布
				
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
			System.out.println(vmss*50/100+" in SY and "+vmss*30/100+" in BJ and "+vmss*20/100+" in SH and "+(vmss-vmss*50/100-vmss*30/100-vmss*20/100)+" in CD");
			
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
			num = 200 ;//改成2压力小很多。别忘改回200！
			mips = 400 ;
			break;
		case "BJ":
			ram = 8192;
			num = 400 ;//改成4压力小很多。别忘改回200！
			mips = 800 ;
			break;
		case "SH":
			ram = 8192;
			num = 100;//改成1压力小很多。别忘改回200！
			mips = 200 ;
			break;
		case "CD":
			ram = 2048;
			num = 200;//改成2压力小很多。别忘改回200！
			mips = 200 ;
			break;
		}
		//按照不明配置 内部默认的配置写法。
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
	
		@SuppressWarnings("unchecked")
		public void run() {
			
			int time = 10;
			
			while(true){
				
				CloudSim.pauseSimulation(time);//别弄错！这个的意思是，在200ms的时候pause一次！按照源代码来看，程序会不断停滞100ms知道resumeSimulation为止。
				//绝不可以一开始pause！要不一开始不是数据中心被弄成实体！而是broker被变成实体了！！所以！只要改成time就好了。
				//此处解决了concurrency...Exception.！！很可能原因是使用迭代器删除了某些东西！
				
				while (true) {
					if (CloudSim.isPaused()) {
						break;
					}
					System.out.println("还在运行？");
					try {
						Thread.sleep(100);//此线程一直休眠到Cloudsim.pause为止！
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				List<Date> dateList = null;
				try {
					dateList = (List<Date>)ois.readObject();
					time += 10;
					count ++; 
					System.out.println("received the "+count+" broker.");
					pw.println("get the "+count+" broker.");//一定要是println！！
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					dateList = null;
				}
				
				if(dateList == null){
					CloudSim.resumeSimulation();
					break;
				}
				
				MyPowerBroker broker = null;
				try {
					broker = new MyPowerBroker("broker_"+MyPowerBrokerList.size());
				} catch (Exception e) {
					e.printStackTrace();
				}
				MyPowerBrokerList.add(broker);//内部类可以调用外部类中的private static
		
				
				//Create VMs and Cloudlets and send them to broker
				
				int num_user = dateList.size();
				List<MyVm> vmlist = new ArrayList<MyVm>();
				List<MyCloudlet> missionList = new ArrayList<MyCloudlet>();
						
				for(int i = 0 ; i < num_user ; i ++){
					int vms = (int)(Math.random() * 7 + 1 ) ;//每个人最多创建8台虚机。。。以后再改
					vmlist.addAll(createVM(broker.getId(), vms*25/100 ,0, "SY"));//vms*50/100的虚拟机是0号机。。。瞎编的数据 谁知道他会请求什么
					vmlist.addAll(createVM(broker.getId(), vms*25/100 ,1, "BJ"));
					vmlist.addAll(createVM(broker.getId(), vms*25/100 ,2, "SH"));
					vmlist.addAll(createVM(broker.getId(), vms-vms*25/100*3 ,3,"CD"));
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
//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}//我只想让这个线程在模拟中睡10秒 按照100：1  那么就是睡0.1s 即100ms 即：每隔10s发送一次这10s内的请求集合
					
					CloudSim.resumeSimulation();
					
			}
			
			
		}
		
	}
	
}