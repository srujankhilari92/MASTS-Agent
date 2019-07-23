/** 
 * @Class Name :  AppZip.java
 */

package com.varutra.masts.proxy;

/**
 * @author     :  Varutra Consulting Pvt. Ltd.
 * @Create On  :  Apr 15, 2014 11:55:13 AM
 * @License    :  Copyright  2014 Varutra Consulting Pvt. Ltd.- All rights reserved.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AppZip {
	public static List<String> fileList;

	AppZip() {
		fileList = new ArrayList<String>();
	}

	public static void zipIt(String zipFile) {

		byte[] buffer = new byte[1024];

		try {

			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);

			for (String file : AppZip.fileList) {

				ZipEntry ze = new ZipEntry(file);
				zos.putNextEntry(ze);

				FileInputStream in = new FileInputStream(
						MyService.SOURCE_FOLDER + File.separator + file);

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				in.close();
			}
			zos.closeEntry();
			zos.close();
			System.out.println("Done");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void generateFileList(File node) {

		if (node.isFile()) {
			fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
		}

		if (node.isDirectory()) {
			String[] subNote = node.list();
			for (String filename : subNote) {
				generateFileList(new File(node, filename));
			}
		}

	}

	private static String generateZipEntry(String file) {
		return file.substring(MyService.SOURCE_FOLDER.length() + 1,
				file.length());
	}
}