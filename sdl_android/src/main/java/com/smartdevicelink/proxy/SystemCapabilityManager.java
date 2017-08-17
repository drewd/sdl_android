package com.smartdevicelink.proxy;

import com.smartdevicelink.proxy.interfaces.OnSystemCapabilityListener;
import com.smartdevicelink.proxy.rpc.GetSystemCapability;
import com.smartdevicelink.proxy.rpc.GetSystemCapabilityResponse;
import com.smartdevicelink.proxy.rpc.RegisterAppInterfaceResponse;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.util.CorrelationIdGenerator;

import java.util.HashMap;

public class SystemCapabilityManager {
	HashMap<SystemCapabilityType, Object> cachedSystemCapabilities = new HashMap<>();
	ISystemCapabilityManager callback;

	public interface ISystemCapabilityManager{
		void onSendPacketRequest(RPCRequest message);
	}

	public SystemCapabilityManager(ISystemCapabilityManager callback){
		this.callback = callback;
	}

	public void parseRAIResponse(RegisterAppInterfaceResponse response){
		cachedSystemCapabilities.put(SystemCapabilityType.HMI, response.getHmiCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.DISPLAY, response.getDisplayCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.AUDIO_PASSTHROUGH, response.getAudioPassThruCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.BUTTON, response.getButtonCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.HMI_ZONE, response.getHmiZoneCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.PRESET_BANK, response.getPresetBankCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.SOFTBUTTON, response.getSoftButtonCapabilities());
		cachedSystemCapabilities.put(SystemCapabilityType.SPEECH, response.getSpeechCapabilities());
	}

	/**
	 * @param systemCapabilityType Type of capability desired
	 * @param scListener callback to execute upon retrieving capability
	 */
	public void getCapability(final SystemCapabilityType systemCapabilityType, final OnSystemCapabilityListener scListener){
		Object capability = cachedSystemCapabilities.get(systemCapabilityType);
		if(capability != null){
			scListener.onCapabilityRetrieved(capability);
		}else if(scListener == null){
			return;
		}

		retrieveCapability(systemCapabilityType, scListener);
	}

	/**
	 * @param systemCapabilityType Type of capability desired
	 * @return Desired capability if it is cached in the manager, otherwise returns a null object
	 * and works in the background to retrieve the capability for the next call
	 */
	public Object getCapability(final SystemCapabilityType systemCapabilityType){
		Object capability = cachedSystemCapabilities.get(systemCapabilityType);
		if(capability != null){
			return capability;
		}

		retrieveCapability(systemCapabilityType, null);
		return null;
	}

	/**
	 * @param systemCapabilityType Type of capability desired
	 * passes GetSystemCapabilityType request to  `callback` to be sent by proxy
	 */
	private void retrieveCapability(final SystemCapabilityType systemCapabilityType, final OnSystemCapabilityListener scListener){
		final GetSystemCapability request = new GetSystemCapability();
		request.setSystemCapabilityType(systemCapabilityType);
		request.setOnRPCResponseListener(new OnRPCResponseListener() {
			@Override
			public void onResponse(int correlationId, RPCResponse response) {
				if(response.getSuccess()){
					Object retrievedCapability = ((GetSystemCapabilityResponse) response).getSystemCapability().getCapabilityForType(systemCapabilityType);
					cachedSystemCapabilities.put(systemCapabilityType, retrievedCapability);
					scListener.onCapabilityRetrieved(retrievedCapability);
				}else{
					scListener.onError(response.getInfo());
				}
			}
		});
		request.setCorrelationID(CorrelationIdGenerator.generateId());

		callback.onSendPacketRequest(request);
	}
}