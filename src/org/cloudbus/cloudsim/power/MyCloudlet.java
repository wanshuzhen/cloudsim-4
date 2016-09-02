package org.cloudbus.cloudsim.power;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.core.CloudSim;

public class MyCloudlet extends Cloudlet{

	private Date requestDate = null;
	
	private double requestTime ;//请求时间和初始时间的时间差  而finishtime又是结束时间和初始时间的时间差
	
	public MyCloudlet(final int cloudletId,final long cloudletLength, final int pesNumber, final long cloudletFileSize,
			final long cloudletOutputSize, final UtilizationModel utilizationModelCpu, final UtilizationModel utilizationModelRam,
			final UtilizationModel utilizationModelBw, final Date requestDate) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		this.requestDate = requestDate;
		Calendar initCalendar = CloudSim.getSimulationCalendar();
		requestTime = (double)((requestDate.getTime() - initCalendar.getTimeInMillis())/1000)/100;//按照虚拟的快100倍流速
		
	}
	
	public Date getRequestDate(){
		return requestDate;
	}
	
	public double getRequestTime(){
		return requestTime;
	}
	
	public double getWaitTime(){
		return getFinishTime() - requestTime ;
	}
	
	

	
	
}
