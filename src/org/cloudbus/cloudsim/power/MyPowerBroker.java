package org.cloudbus.cloudsim.power;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.EventPostBroker;
import org.cloudbus.cloudsim.core.SimEvent;

public class MyPowerBroker extends DatacenterBroker {

	public MyPowerBroker(String name) throws Exception {
		super(name);
	}
	
	public MyPowerBroker(String name, EventPostBroker evt_) throws Exception {
		super(name,evt_);
	}
	
	@Override
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter();
		}
	}
	
	protected void createVmsInDatacenter() {
		// send as much vms as possible for this datacenter before trying the next one
		int requestedVms = 0;
		List<Integer> datacenterIdList = new ArrayList<Integer>();
		for (Vm vm : getVmList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
				int datacenterId = CloudSim.getEntityId(((MyVm)vm).getTarget());
				if(!datacenterIdList.contains((Integer)datacenterId)){
					datacenterIdList.add(datacenterId);//竟然忘了添加
//					getDatacenterRequestedIdsList().add(datacenterId);
				}
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + ((MyVm)vm).getTarget());
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}

		

		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}
	
	//后边就完全继承自DatacenterBroker,..因为源代码 是完全调用DatacenterBroker的  看PowerDatacenterBroker源代码就知道PowerDatacenterBroker根本没有用处。
	
//	@Override//用默认的轮循就行了。
//	protected void submitCloudlets() {
//		int vmIndex = 0;
//                
//                Log.printLine("allo oui submitCloudlet !");
//                
//		for (Cloudlet cloudlet : getCloudletList()) {
//			Vm vm;
//			// if user didn't bind this cloudlet and it has not been executed yet
//			if (cloudlet.getVmId() == -1) {
//				vm = getVmsCreatedList().get(vmIndex);
//			} else { // submit to the specific vm
//				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
//				if (vm == null) { // vm was not created
//					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
//							+ cloudlet.getCloudletId() + ": bount VM not available");
//					continue;
//				}
//			}
//
//			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
//					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
//			cloudlet.setVmId(vm.getId());
//			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
//			cloudletsSubmitted++;
//			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
//			getCloudletSubmittedList().add(cloudlet);
//		}
//
//		// remove submitted cloudlets from waiting list
//		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
//			getCloudletList().remove(cloudlet);
//		}
//	}
	
}
