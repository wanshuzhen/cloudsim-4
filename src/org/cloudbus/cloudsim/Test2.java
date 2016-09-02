package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Test2 {

	private static List<Datacenter> DatacenterList = new ArrayList<Datacenter>();
	private static List<DatacenterBroker> brokerList = new ArrayList<DatacenterBroker>();
	private static List<List<Cloudlet>> allcloudletList = new ArrayList<List<Cloudlet>>();
	private static List<Vm> allvmlist = new ArrayList<Vm>();
	
	private static List<Vm> createVM(int userId, int vms) {

		LinkedList<Vm> list = new LinkedList<Vm>();

		long size = 10000; 
		int ram = 512; 
		int mips = 200;
		long bw = 1000;
		int pesNumber = 1; 
		String vmm = "Xen"; 

		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());//这里！space和time！
//			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
//			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerDynamicWorkload(mips, pesNumber));
			list.add(vm[i]);
		}

		return list;
	}
	
	
	public static void main(String[] args) {

		Log.printLine("Starting MyExample...");

		try {
			int num_user = 1;//【人口这玩意之后再说吧/ 】  
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  

			CloudSim.init(num_user, calendar, trace_flag);

			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			createDatacenter();
			
			//Third step: Create Brokers
			createBrokerList(num_user);

			//Fourth step: Create one virtual machine for each broker/user
			for(int i = 0 ; i < num_user ; i++){
				int vms = 4 ;//权且设置每个人只需要一台虚拟机。。。【这里要改?】
				List<Vm> vmlist = createVM(brokerList.get(i).getId(), vms);
				//submit vm list to the broker
				brokerList.get(i).submitVmList(vmlist);
				allvmlist.addAll(vmlist);
			}

			//Fifth step: Create two Cloudlets
			int missionNum = 40 ;
			createCloudletList(num_user , missionNum);//【到底要创建多少个任务？】【嘛，假设每个人4个吧，】【最后要num_user*missionNum为总任务数
			
			//submit cloudlet list to the brokers
			for(int i = 0 ; i < brokerList.size() ; i ++){
				for(int j = 0 ;  j < missionNum ; j ++)
				brokerList.get(i).submitCloudletList(allcloudletList.get(i*missionNum + j));//每个人接收四个任务
//				brokerList.get(i).submitCloudletList(allcloudletList.get(i*4+1));
//				brokerList.get(i).submitCloudletList(allcloudletList.get(i*4+2));
//				brokerList.get(i).submitCloudletList(allcloudletList.get(i*4+3));
				
//				brokerList.get(i).bindCloudletToVm(allcloudletList.get(i*4).get(0).getCloudletId(), allvmlist.get(i).get(0).getId());
//				brokerList.get(i).bindCloudletToVm(allcloudletList.get(i*4+1).get(0).getCloudletId(), allvmlist.get(i).get(0).getId());
//				brokerList.get(i).bindCloudletToVm(allcloudletList.get(i*4+2).get(0).getCloudletId(), allvmlist.get(i).get(0).getId());
//				brokerList.get(i).bindCloudletToVm(allcloudletList.get(i*4+3).get(0).getCloudletId(), allvmlist.get(i).get(0).getId());
			}

			// Sixth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<List<Cloudlet>> allList = new ArrayList<List<Cloudlet>>();
			for(int i = 0 ; i < brokerList.size() ; i ++){
				allList.add(brokerList.get(i).getCloudletReceivedList());
			}

			CloudSim.stopSimulation();
			
			for(int i = 0 ; i < brokerList.size() ; i ++){
				Log.print("=============> User "+brokerList.get(i).getId()+"    ");
				printCloudletList(allList.get(i));
			}
			DatacenterList.get(0).printDebts();
			DatacenterList.get(1).printDebts();
			DatacenterList.get(2).printDebts();
			DatacenterList.get(3).printDebts();
			
			
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
	
	private static Datacenter createCenter(String s){
		List<Host> cities = new ArrayList<Host>();
		
		long storage = 1000000;//1T
		int bw = 10000;
		int hostId = 0 ;
		int ram = 0 ;
		int num = 0 ;
		int mips = 0 ;
		switch(s){
		case "SY":
			ram = 4096;
			num = 200 ;
			mips = 400 ;
			break;
		case "BJ":
			ram = 8192;
			num = 400 ;
			mips = 800 ;
			break;
		case "SH":
			ram = 8192;
			num = 100;
			mips = 200 ;
			break;
		case "CD":
			ram = 2048;
			num = 200;
			mips = 200 ;
			break;
		}
		List<Pe> peList = new ArrayList<Pe>();//每个host的标配
		peList.add(new Pe(0,new PeProvisionerSimple(mips)));
		for(int i = 0 ; i < num ; i ++){
			cities.add(new Host(hostId,new RamProvisionerSimple(ram),new BwProvisionerSimple(bw),storage,peList,new VmSchedulerTimeShared(peList)));
			hostId ++ ;
		}
		
		String arch = "x64" ;
		String os = "Macos";
		String vmm = "Xen" ;
		double time_zone = 8.0 ;
		double cost = 3.0;             
		double costPerMem = 0.05;		
		double costPerStorage = 0.001;	
		double costPerBw = 0.1;			
		LinkedList<Storage> storageList = new LinkedList<Storage>();
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, cities, time_zone, cost, costPerMem, costPerStorage, costPerBw);
		
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(s, characteristics, new VmAllocationPolicySimple(cities), storageList, 0);
//			PowerDatacenter center = new PowerDatacenter(s, characteristics, new VmAllocationPolicySimple(cities), storageList, 0);
//			System.out.println(center.getPower());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return datacenter;
	}
	
	private static void createBrokerList(int num_user) {
		try {
			for(int i = 0 ; i < num_user ; i ++){
				DatacenterBroker broker = new DatacenterBroker("Broker");
				brokerList.add(broker);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void createCloudletList(int num_user ,int missionNum){
		for(int i = 0 ; i < num_user * missionNum ; i ++){
			List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
			long length = 40000;
			long fileSize = 300;
			long outputSize = 300;
			int pesNumber = 1 ;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet.setUserId(brokerList.get(i/missionNum).getId());
			cloudletList.add(cloudlet);
			allcloudletList.add(cloudletList);
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
	
//	private static void firstOrderOfVms(){
//		int size = allcloudletList.size();
//		for(int i = 0 ; i < size ; i ++){
//			allcloudletList.get(i).get(0)
//		}
//	}
	
}
