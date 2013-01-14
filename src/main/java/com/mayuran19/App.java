package com.mayuran19;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.mayuran19.jasperServer.client.rest.JasperServerRestClient;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) {
		System.out.println("Hello World!");
		JasperServerRestClient client = new JasperServerRestClient();
		InputStream is = client.downloadReport("ExpenseReport", "XLS");

		try {
			FileOutputStream fos = new FileOutputStream(UUID.randomUUID()
					.toString() + ".xls");
			byte[] bytes = new byte[1024];
			while (is.read(bytes) != -1) {
				fos.write(bytes);
			}
			fos.flush();
			fos.close();
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
