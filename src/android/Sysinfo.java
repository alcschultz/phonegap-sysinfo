package com.ankamagames.plugins.sysinfo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.os.Build;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.Process;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import android.util.*;

public class Sysinfo extends CordovaPlugin {
	private MemoryInfo memoryInfo;
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callback) {

		Activity activity = this.cordova.getActivity();
		ActivityManager m = (ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE);
		this.memoryInfo = new MemoryInfo();
		m.getMemoryInfo(this.memoryInfo);
		
		if (action.equals("getInfo")) {
			try {
				JSONObject r = new JSONObject();
	            r.put("cpu", this.getCpuInfo());
	            r.put("memory", this.getMemoryInfo());
	            Log.d("OUTPUT", r.toString());
	            callback.success(r);
			} catch (final Exception e) {
				callback.error(e.getMessage());
			}
		}
		
		return false;
	}
	
	public JSONObject getCpuInfo() {
		JSONObject cpu = new JSONObject();
		try {
			// Get CPU Core count
			String output = readSystemFile("/sys/devices/system/cpu/present");
			String[] parts = output.split("-");
			Integer cpuCount = Integer.parseInt(parts[1]) + 1;
			
			cpu.put("count", cpuCount);

			// Get CPU Core frequency
			JSONArray cpuCores = new JSONArray();
			for(int i = 0; i < cpuCount; i++) {
				Integer cpuMaxFreq = getCPUFrequencyMax(i);
				cpuCores.put(cpuMaxFreq == 0 ? null : cpuMaxFreq);
			}
			
			//Get architecture
			String arch = getInfo();
			
			cpu.put("cores", cpuCores);
			cpu.put("architecture", arch);
			
		} catch (final Exception e) { 
			Log.e("ERROR", e.toString());
		}
		return cpu;
	}
	
	public JSONObject getMemoryInfo() {
		JSONObject memory = new JSONObject();
		try {
			memory.put("available", this.memoryInfo.availMem);
			memory.put("total", this.getTotalMemory());
			memory.put("threshold", this.memoryInfo.threshold);
			memory.put("low", this.memoryInfo.lowMemory);
		} catch (final Exception e) {
			
		}
		return memory;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public Object getTotalMemory() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			return this.memoryInfo.totalMem;
		}
		else {
			return null;
		}
	}
	
	/**
	 * @return in kiloHertz.
	 * @throws SystemUtilsException
	 */
	public int getCPUFrequencyMax(int index) throws Exception {
		return readSystemFileAsInt("/sys/devices/system/cpu/cpu" + index + "/cpufreq/cpuinfo_max_freq");
	}
	
	private String readSystemFile(final String pSystemFile) {
		String content = "";
		InputStream in = null;
		try {
	      final Process process = new ProcessBuilder(new String[] { "/system/bin/cat", pSystemFile }).start();
	      in = process.getInputStream();
	      content = readFully(in);
	    } catch (final Exception e) { } 
		return content;
	}
	
	private int readSystemFileAsInt(final String pSystemFile) throws Exception {
		String content = readSystemFile(pSystemFile);
		if (content == "") {
			return 0;
		}
		return Integer.parseInt( content );
	}
	
	private String readFully(final InputStream pInputStream) throws IOException {
		final StringBuilder sb = new StringBuilder();
		final Scanner sc = new Scanner(pInputStream);
	    while(sc.hasNextLine()) {
	      sb.append(sc.nextLine());
	    }
	    return sb.toString();
	}
	
	private String getInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("abi: ").append(Build.CPU_ABI).append("\n");
		if (new File("/proc/cpuinfo").exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
				String aLine;
				while ((aLine = br.readLine()) != null) {
					sb.append(aLine + "\n");
				}
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		return sb.toString();
	}
}
