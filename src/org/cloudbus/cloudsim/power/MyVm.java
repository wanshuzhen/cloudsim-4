package org.cloudbus.cloudsim.power;

import java.util.ArrayList;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.SimEvent;

public class MyVm extends Vm {
	
	private String targetDatacenter ;//这里权且使用名字。虽然十分危险。。因为用户可能逗比传进来乱七八糟的东西。。但是由于是数据也不打算进行错误检查。随机生成即可。
	
	public MyVm(
			int id,
			int userId,
			double mips,
			int numberOfPes,
			int ram,
			long bw,
			long size,
			String vmm,
			CloudletScheduler cloudletScheduler,
			String targetDatacenter
			) {
		super(id,userId,mips,numberOfPes,ram,bw,size,vmm,cloudletScheduler);
		this.targetDatacenter = targetDatacenter;
	}
	
	public String getTarget(){
		return this.targetDatacenter;
	}
	
	
	
}
