package com.alibaba.aliware.grpc.swift.event.swift.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DebugPrintInfoUtils {

	public static void printWithDate(String path, String info, boolean isAppend) {
		BufferedWriter writer = null ;
		try {
			writer = new BufferedWriter(new FileWriter(new File(path), isAppend));
			writer.write(info + "\t");
			writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(writer != null){
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void print(String path, String info, boolean isAppend) {
		BufferedWriter writer = null ;
		try {
			writer = new BufferedWriter(new FileWriter(new File(path), isAppend));
			writer.write(info + "\t");
				writer.newLine();
				writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				writer.close();
			} catch (Exception e) {
			}
		}
	}
}
