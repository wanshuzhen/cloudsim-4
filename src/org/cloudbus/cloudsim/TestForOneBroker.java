package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimpleWattPerMipsMetric;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_BAZAR;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.xml.DvfsDatas;

public class TestForOneBroker {

	private static List<PowerDatacenter> DatacenterList = new ArrayList<PowerDatacenter>();
	private static PowerDatacenterBroker broker = null;
	private static List<Cloudlet> allcloudletList = new ArrayList<Cloudlet>();
	private static List<Vm> allvmlist = new ArrayList<Vm>();
	
	private static void createVM(int userId, int vms ,int type) {

		long size = 0; 
		int ram = 0; 
		int mips = 0;
		long bw = 0;
		int pesNumber = 0; 
		String vmm = null; 
		
		switch(type){
		case 0:
			size = 10000; 
			ram = 512; 
			mips = 20;
			bw = 1000;
			pesNumber = 1; 
			vmm = "Xen"; 
			break;
		case 1:
			size = 15000; 
			ram = 512; 
			mips = 40;
			bw = 2000;
			pesNumber = 1; 
			vmm = "BetterXen"; 
			break;
		case 2:
			size = 20000; 
			ram = 1024; 
			mips = 80;
			bw = 2000;
			pesNumber = 2; 
			vmm = "MoreXen"; 
			break;
		case 3:
			size = 25000; 
			ram = 2048; 
			mips = 100;
			bw = 5000;
			pesNumber = 4; 
			vmm = "BestXen"; 
			break;
		}
		

		for(int i=0;i<vms;i++){
			Vm vm = new Vm(allvmlist.size(), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());//���space��time��
//			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
//			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerDynamicWorkload(mips, pesNumber));
			allvmlist.add(vm);
		}
	}
	
	
	public static void main(String[] args) {

		Log.printLine("Starting MyExample...");

		try {
			int num_user = 1;//���˿�������֮����˵��/ ��  
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  

			CloudSim.init(num_user, calendar, trace_flag);

			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			createDatacenter();
			for(PowerDatacenter datacenter : DatacenterList){
				datacenter.setDisableMigrations(true);//别忘了。
			}
			
			//Third step: Create Brokers
			broker =  new PowerDatacenterBroker("Broker");
			
			//Fourth step: Create one virtual machine for each broker/user
			for(int i = 0 ; i < num_user ; i++){
				int vms = 1 ;//Ȩ������ÿ����ֻ��Ҫһ̨�����������������Ҫ��?��
				createVM(broker.getId(), vms ,0);
//				createVM(broker.getId(), vms*50/100 ,0);
//				createVM(broker.getId(), vms*30/100 ,1);
//				createVM(broker.getId(), vms*20/100 ,2);
//				createVM(broker.getId(), vms-vms*50/100-vms*30/100-vms*20/100,3);
				//submit vm list to the broker
				broker.submitVmList(allvmlist);
			}

			//Fifth step: Create two Cloudlets
			int missionNum = 40 ;
			createCloudletList(missionNum);//������Ҫ�������ٸ����񣿡��������ÿ����4���ɣ��������Ҫnum_user*missionNumΪ��������
			
			//submit cloudlet list to the brokers
//			for(int j = 0 ;  j < missionNum ; j ++)
			broker.submitCloudletList(allcloudletList);//ÿ���˽����ĸ�����

//			firstOrderOfVms();	
//			greedy();
			
			// Sixth step: Starts the simulation
			double lastClock = CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<List<Cloudlet>> allList = new ArrayList<List<Cloudlet>>();
			allList.add(broker.getCloudletReceivedList());

			CloudSim.stopSimulation();
			
			Log.print("=============> User "+broker.getId()+"    ");
			printCloudletList(allList.get(0));
			DatacenterList.get(0).printDebts();
			DatacenterList.get(1).printDebts();
			DatacenterList.get(2).printDebts();
			DatacenterList.get(3).printDebts();
			
			//直接照搬而来
			double powerSum = 0 ;
			for(int i = 0 ; i < DatacenterList.size() ; i++){
				powerSum += DatacenterList.get(i).getPower();
			}
			Log.printLine(String.format("Total simulation time: %.2f sec", lastClock));
			Log.printLine(String.format("Power Sum :  %.8f W", powerSum ));
			Log.printLine(String.format("Power Average : %.8f W", powerSum / (lastClock*100)));
			Log.printLine(String.format("Energy consumption: %.8f Wh", (powerSum / (lastClock*100)) * (lastClock*100 / 3600)));
			
			Log.printLine("MyExample finished!");

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
	
	private static PowerDatacenter createCenter(String s){
		List<PowerHost> cities = new ArrayList<PowerHost>();
		
		long storage = 1000000;//1T
		int bw = 300000;
		int ram = 0 ;
		int num = 0 ;
		int mips = 0 ;
		switch(s){
		case "SY":
			ram = 4096;
			num = 2 ;
			mips = 400 ;
			break;
		case "BJ":
			ram = 8192;
			num = 4 ;
			mips = 800 ;
			break;
		case "SH":
			ram = 8192;
			num = 1;
			mips = 200 ;
			break;
		case "CD":
			ram = 2048;
			num = 2;
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
			peList.add(new Pe(0,new PeProvisionerSimple(mips),freqs,govs.get(0), ConfigDvfs));
			cities.add(new PowerHost(i,new RamProvisionerSimple(ram),new BwProvisionerSimple(bw),storage,peList,new VmSchedulerTimeShared(peList),new PowerModelSpecPower_BAZAR(peList),false,
					enableDVFS));
		}
		
		String arch = "x86" ;
		String os = "Linus";
		String vmm = "Xen" ;
		double time_zone = 8.0 ;
		double cost = 3.0;             
		double costPerMem = 0.05;		
		double costPerStorage = 0.001;	
		double costPerBw = 0.1;			
		LinkedList<Storage> storageList = new LinkedList<Storage>();
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, cities, time_zone, cost, costPerMem, costPerStorage, costPerBw);
		
		PowerDatacenter datacenter = null;
		try {
			datacenter = new PowerDatacenter(s, characteristics, new PowerVmAllocationPolicySimpleWattPerMipsMetric(cities), storageList, 0.01);
//			PowerDatacenter center = new PowerDatacenter(s, characteristics, new VmAllocationPolicySimple(cities), storageList, 0);
//			System.out.println(center.getPower());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return datacenter;
	}
	
	private static void createCloudletList(int missionNum){
		for(int i = 0 ; i < missionNum ; i ++){
			long length = 4000;
			long fileSize = 300;
			long outputSize = 300;
			int pesNumber = 1 ;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet.setUserId(broker.getId());
			allcloudletList.add(cloudlet);
		}
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
	
	private static void firstOrderOfVms(){
		int size = allcloudletList.size();
		for(int i = 0 ; i < size ; i ++){
			//��ȫ˳���,��������Ӧ����Ĭ�Ϸ�����
			allcloudletList.get(i).setVmId(i%allvmlist.size());
		}
	}
	
	private static void greedy(){//̰���㷨��
		
		allcloudletList.sort(new Comparator<Cloudlet>(){

			@Override
			public int compare(Cloudlet o1, Cloudlet o2) {
				return (int)(o1.getCloudletLength() - o2.getCloudletLength());
			}
			
		});
		
		allvmlist.sort(new Comparator<Vm>(){

			@Override
			public int compare(Vm o1, Vm o2) {
				return (int)((o1.getCurrentRequestedMaxMips()) - (o2.getCurrentRequestedTotalMips())) ;
			}
			
		});
		
		for(int i = 0 ; i < allcloudletList.size() ; i ++){
			allcloudletList.get(i).setVmId(i%allvmlist.size());
		}
	}
	
	
}
