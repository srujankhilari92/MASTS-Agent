package com.varutra.masts.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

public class ListFiles {

	private StringBuffer listString;
	private ArrayList al;
	private Process p;
	private DataOutputStream stdin;
	private BufferedReader bufferRD;
	private String s;
	private BufferedWriter out;
	private DateFormat sdf;
	private SharedPreferences prefs;
	private Object currentDateandTime;
	private String currentScreen;
	private BufferedInputStream in;
	private static List<String> fileList;
	private ArrayList databasesList = new ArrayList();
	private ArrayList apkdbfiles = new ArrayList();
	String path = "";

	/**
	 * @param context
	 * 
	 */
	public ListFiles(Context context) {
		// TODO Auto-generated constructor stub

		fileList = new ArrayList<String>();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public ListFiles() {
		// TODO Auto-generated constructor stub

		fileList = new ArrayList<String>();

	}

	@SuppressLint("SimpleDateFormat")
	public void readDir(Socket socket, String cmd) {
		String[] getType = cmd.split("\\$");
		
		try {
			if ((getType[0] != null || !getType[0].isEmpty())
					&& (getType[1] != null || !getType[1].isEmpty())) {

				listString = new StringBuffer();

				al = new ArrayList();
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
				Date date;

				try {
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("ls '" + getType[0] + "' -l\n");

					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getInputStream()));
					String line = null;

					while ((line = bufferRD.readLine()) != null) {
						String strRep = line.trim().replaceAll("\\s{2,}", " ");
						String[] splitStr = strRep.split("\\s");
						String fileDir = "-";
						String lastModified = "-";
						String time = "-";
						String length = "-";
						String name = "-";
				
						

						if (strRep.contains(",")) {
							
							fileDir = "File";
							length = splitStr[4];
							lastModified = splitStr[5];
							time = splitStr[6];
							name = splitStr[6] + " " + splitStr[7];

						} else if (splitStr.length == 7) {
							if (splitStr[0].contains("d")
									|| splitStr[0].contains("l")) {
								fileDir = "Dir";
							} else {
								fileDir = "File";
							}
		                    if(splitStr[3].contains("-")||splitStr[3].contains("/")){
		                    	
		                    	length="0";
		                    	lastModified = splitStr[3];
		                    	time = splitStr[4];
		                    	name = splitStr[5]+" "+splitStr[6];
		                    	
		                    }else{
		                    		
		                    	length = splitStr[3];
								lastModified = splitStr[4];
								time = splitStr[5];
								name = splitStr[6];

		                    }
							
						} else if ((splitStr.length == 6 || splitStr.length == 8)) {
							
							if (splitStr[0].contains("d")
									|| splitStr[0].contains("l")) {
								fileDir = "Dir";
							} else {
								fileDir = "File";
							}
							
							if(splitStr[3].equals("0")){
								
								length = splitStr[3];
								lastModified = splitStr[4];
								time = splitStr[5];
								
							    name = splitStr[6]+" "+splitStr[7];
								
								
							}else{

								length = "0";
								lastModified = splitStr[3];
								time = splitStr[4];
								name = splitStr[5];
								
							}
							
							
						} else if ((splitStr.length == 9)) {
							if (splitStr[0].contains("d")
									|| splitStr[0].contains("l")) {
								fileDir = "Dir";
							} else {
								fileDir = "File";
							}
							length = "0";
							lastModified = splitStr[3];
							time = splitStr[4];
							name = splitStr[5];
						}
						
						
						
						
						
						if (getType[1].equals("SORTTYPE")) {

							listString.append(fileDir + "\t" + name + "\t"
									+ lastModified + "\t" + time + "\t"
									+ length + "<br>");
							
							
							
						} else if (getType[1].equals("SORTNAME")) {

							Log.e("SORTNAME if : ", "SORTNAME");
							int size = Integer.parseInt(length);
							//if(name.contains(".apk")){
								
								al.add(new Sortbean(name, fileDir, lastModified,
									time, size));
							//}
						} else if (getType[1].equals("SORTDATE")) {
							Log.e("SORTDATE if : ", "SORTDATE");
							int size = Integer.parseInt(length);
							
							
							
								al.add(new Sortbean(name, fileDir, lastModified,
									time, size));
							

						} else if (getType[1].equals("SORTTIME")) {
							int size = Integer.parseInt(length);
						
								
								al.add(new Sortbean(name, fileDir, lastModified,
										time, size));
							
						} else if (getType[1].equals("SORTSIZE")) {
							Log.e("SORTSIZE if : ", "SORTSIZE");
							int 
							size = Integer.parseInt(length);
							
								
								al.add(new Sortbean(name, fileDir, lastModified,
									time, size));
						
						} else if(getType[1].equals("SORTDB")){
							
							   Log.e("SORTDB call ", "SORTDB call");
							   getapkdb(getType[0]);
							   getFeatureList(apkdbfiles);
							   getDatabaseList(databasesList);
							
						} else{
							listString.append(fileDir + "\t" + name + "\t"
									+ lastModified + "\t" + time + "\t"
									+ length + "<br>");
						}

					}
					if (getType[1].equals("SORTDATE")) {

						Log.e("SORTDATE sort", "SORTDATE");
						Collections.sort(al,
								Collections.reverseOrder(new Datecomparator()));
						Iterator itr3 = al.iterator();
						while (itr3.hasNext()) {
							Sortbean st = (Sortbean) itr3.next();

							listString.append(st.date + "\t" + st.name + "\t"
									+ st.type + "\t" + st.time + "\t" + st.size
									+ "<br>");

						}
					} else if (getType[1].equals("SORTNAME")) {
						Log.e("SORTNAME sort", "SORTNAME");
						Collections.sort(al, new NameComparator());
						Iterator itr = al.iterator();
						while (itr.hasNext()) {
							Sortbean st = (Sortbean) itr.next();

							listString.append(st.name + "\t" + st.type + "\t"
									+ st.date + "\t" + st.time + "\t" + st.size
									+ "<br>");

						}

					} else if (getType[1].equals("SORTSIZE")) {

						Log.e("SORTSIZE sort", "SORTSIZE");
						Collections.sort(al,
								Collections.reverseOrder(new SizeComparator()));
						Iterator itr2 = al.iterator();
						while (itr2.hasNext()) {
							Sortbean st = (Sortbean) itr2.next();

							listString.append(st.size + "\t" + st.name + "\t"
									+ st.date + "\t" + st.time + "\t" + st.type
									+ "<br>");

						}
					} else if (getType[1].equals("SORTTIME")) {
						Log.e("SORTTIME sort", "SORTTIME");
						Collections.sort(al,
								Collections.reverseOrder(new Timecomparator()));
						Iterator itr4 = al.iterator();
						while (itr4.hasNext()) {
							Sortbean st = (Sortbean) itr4.next();

							listString.append(st.time + "\t" + st.name + "\t"
									+ st.date + "\t" + st.type + "\t" + st.size
									+ "<br>");

						}

					} else{
						Log.e("SORTTYPE sort", "SORTTYPE");

						Collections.sort(al,
								Collections.reverseOrder(new Datecomparator()));
						Iterator itr3 = al.iterator();
						while (itr3.hasNext()) {
							Sortbean st = (Sortbean) itr3.next();

							listString.append(st.date + "\t" + st.name + "\t"
									+ st.type + "\t" + st.time + "\t" + st.size
									+ "<br>");

						}
					}
					Send_Result_Message_TO_Client(socket, listString.toString()
							+ "#END");
					al.clear();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					Send_Error_Message_TO_Client(socket, "Enter Valid Commands");
				}
			} else {
				Send_Error_Message_TO_Client(socket, "Enter Valid Commands");
			}
		} catch (NullPointerException e) {
			// TODO: handle exception
			Send_Error_Message_TO_Client(socket, "Enter Valid Commands");

		} catch (Exception ex) {

			Send_Error_Message_TO_Client(socket, "Enter Valid Commands");

		}

	}

	void moveFile(Socket socket, String source, String destn) {
		BufferedReader bufferRD;
		try {
			Process p = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			DataOutputStream stdin = new DataOutputStream(p.getOutputStream());

			stdin.writeBytes("mv " + source + " " + destn + "\n");
			stdin.flush();
			stdin.writeBytes("exit\n");
			stdin.flush();

			bufferRD = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			s = bufferRD.readLine();
			if (s == null) {
				try {
					Send_Result_Message_TO_Client(socket,
							"Move File : Source : " + source + " to " + destn
									+ " Done");
					bufferRD.close();
				} catch (IOException ioexception) {
					ioexception.printStackTrace();
					Send_Error_Message_TO_Client(socket,
							"Move File : Source : " + source + " to " + destn
									+ " Not");

				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	void rm_File(Socket socket, String cmd) {


		File f = new File(cmd.toString().trim());
		if (!f.exists()) {
			Log.e(cmd, " File/Directory not Exist");
			Send_Error_Message_TO_Client(socket, cmd
					+ " File/Directory not Exist");

		} else {
			try {
				if (f.isDirectory()) {
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("rm -r " + cmd + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getErrorStream()));

					s = bufferRD.readLine();
					if (s == null) {
						try {

							Send_Result_Message_TO_Client(socket, cmd
									+ " Removed Successfuliy");
							bufferRD.close();
						} catch (IOException ioexception) {
							
							ioexception.printStackTrace();
							Send_Error_Message_TO_Client(socket,
									"Enter Valid Commands <br><br> "
											+ " Command " + cmd
											+ " Invalid 159");

						}

					} else {
						Send_Result_Message_TO_Client(socket, cmd
								+ " File/Directory not Exist");

					}
				} else {
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("rm " + cmd + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getErrorStream()));

					s = bufferRD.readLine();
					if (s == null) {
						try {
							Send_Result_Message_TO_Client(socket, cmd
									+ " Removed Successful");
							bufferRD.close();
						} catch (IOException ioexception) {
							ioexception.printStackTrace();
							Send_Error_Message_TO_Client(socket, cmd
									+ " File/Directory not Exist");

						}

					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Send_Error_Message_TO_Client(socket, cmd
						+ " File/Directory not Exist");
			}
		}

	}

	void rm_File_Directory(String cmd) {
		File f = new File(cmd.toString().trim());
		if (!f.exists()) {
			Log.e(cmd, " File/Directory not Exist");

		} else {
			try {
				if (f.isDirectory()) {
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("rm -r " + cmd + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getErrorStream()));

					s = bufferRD.readLine();
					if (s == null) {
						try {
							Log.d("rm file/ Directory", cmd
									+ " Removed Successful");

							bufferRD.close();
						} catch (IOException ioexception) {
							Log.e("MyService",
									"Error in Removing file/ directory");
							ioexception.printStackTrace();

						}

					}
				} else {
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("rm " + cmd + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getErrorStream()));

					s = bufferRD.readLine();
					if (s == null) {
						try {

							bufferRD.close();
						} catch (IOException ioexception) {
							Log.e("Exception in removing", "file " + cmd);
							ioexception.printStackTrace();

						}

					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();

			}
		}
	}

	void Copy_Source_to_Dest(Socket socket, String cmd, String destn) {
		
		File FileCopy = new File(cmd.toString().trim());
		File FileCopyTo = new File(destn.toString().trim());
		if (!FileCopy.exists()) {
			Send_Error_Message_TO_Client(socket, cmd + "Source Not Exist");
		} else if (!FileCopyTo.exists()) {
			Send_Error_Message_TO_Client(socket, destn
					+ "Destination Not Exist");
		} else {
			try {
				p = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				stdin = new DataOutputStream(p.getOutputStream());

				stdin.writeBytes("cp -r " + cmd + " " + destn + "\n");
				stdin.flush();
				stdin.writeBytes("exit\n");
				stdin.flush();

				bufferRD = new BufferedReader(new InputStreamReader(
						p.getErrorStream()));

				s = bufferRD.readLine();
				if (s == null) {
					try {

						Send_Result_Message_TO_Client(socket, "File " + cmd
								+ " Copied Successfully");
						bufferRD.close();
					} catch (IOException ioexception) {
						ioexception.printStackTrace();
						Send_Error_Message_TO_Client(socket,
								"No Permission to Read and Write");

					}

				} else {
					Send_Error_Message_TO_Client(socket,
							"No Permission to Read Write");

				}

			} catch (IOException e) {
				// TODO Auto-generated catch block

				Send_Error_Message_TO_Client(socket,
						"No Permission to Read Write");
			}
		}

	}

	void Copy_Source_to_Dest_Directory(Socket socket, String sOURCE_FOLDER,
			String sOURCE_FOLDER_TWO) {
		
		File FileCopy = new File(sOURCE_FOLDER.toString().trim());
		File FileCopyTo = new File(sOURCE_FOLDER_TWO.toString().trim());
		if (!FileCopy.exists()) {
			Send_Error_Message_TO_Client(socket, sOURCE_FOLDER + " Not Exist");
		} else {
			try {
				if (!FileCopyTo.exists()) {
					FileCopyTo.mkdir();
		
				} else {
					Log.e("Temp Folder Already Exist  :: ", "Already Exit");

				}

				p = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				stdin = new DataOutputStream(p.getOutputStream());

				stdin.writeBytes("cp -r " + sOURCE_FOLDER + " "
						+ sOURCE_FOLDER_TWO + "\n");
				stdin.flush();
				stdin.writeBytes("exit\n");
				stdin.flush();

				bufferRD = new BufferedReader(new InputStreamReader(
						p.getErrorStream()));

				s = bufferRD.readLine();
				if (s == null) {
					try {
						
						bufferRD.close();
					} catch (IOException ioexception) {
						ioexception.printStackTrace();

					}

				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();

			}
		}

	}

	void List_ALL_DB_Files(Socket socket) {

		listString = new StringBuffer();

		try {
			p = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			stdin = new DataOutputStream(p.getOutputStream());

			stdin.writeBytes("find / -name *.db\n");
			stdin.flush();
			stdin.writeBytes("exit\n");
			stdin.flush();

			bufferRD = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = null;
			while ((line = bufferRD.readLine()) != null) {
				if (!(line.toString().contains("/mnt"))
						&& !(line.toString()
								.contains("/data/media/0/data/data/")))
					listString.append(line.toString() + "<br>");
			}

			Send_Result_Message_TO_Client(socket, listString + "<br><br>");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();

			Send_Error_Message_TO_Client(socket, "Please Enter Valid Command");
		}

	}

	public List<String> getListFiles(File parentDir) {
		ArrayList<String> inFiles = new ArrayList<String>();
		try {
			File[] files = parentDir.listFiles();

			for (File file : files) {
				if (file.isDirectory()) {
					inFiles.addAll(getListFiles(file));
				} else {
					if (file.getName().endsWith(".db")) {
						
						inFiles.add(file.getName());
					}
				}
			}
		} catch (Exception ex) {
			return inFiles;
		}
		return inFiles;
	}

	void sendApkFile() {

	}

	public String getFile(Socket socket, String receivedMessage) {
		// TODO Auto-generated method stub

		File myFile = new File(receivedMessage.trim());
		try {
			

			if (myFile.exists()) {
				int count;
				byte[] buffer = new byte[1024];

				socket.setTcpNoDelay(true);
				
				OutputStream out = socket.getOutputStream();
				in = new BufferedInputStream(new FileInputStream(myFile));
				int max = 0;
				while ((count = in.read(buffer)) > 0) {
					max++;
					out.write(buffer, 0, count);
					out.flush();
				}

				socket.close();
				Log.e("Send File", "File send successfully");

			} else {
				Log.e("File Does not Exist", "-----X");
				Send_Error_Message_TO_Client(socket, receivedMessage
						+ "Invalid Command");

			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}
		return "SUCCESS";
	}

	public void downloadFile(Socket socket, String receivedMessage) {
		// TODO Auto-generated method stub

		File myFile = new File(receivedMessage.trim());
		try {
			System.out.println("Connecting...");

			if (myFile.exists()) {
				
				int count;
				byte[] buffer = new byte[2048];

				socket.setSendBufferSize(10000);
				socket.setTcpNoDelay(true);
				OutputStream out = socket.getOutputStream();
				BufferedInputStream in = new BufferedInputStream(
						new FileInputStream(myFile));
				while ((count = in.read(buffer)) >= 0) {
					out.write(buffer, 0, count);
					out.flush();
				}
				socket.close();
				Log.e("------------->", "File Send Complet");
			} else {
				Log.e("File Does not Exist", "-----X");
				Send_Error_Message_TO_Client(socket, receivedMessage
						+ "Invalid Command");

			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();

		}
	}

	public void createFILE(Socket socket, String receivedMessage) {
		// TODO Auto-generated method stub
        
		File file = new File(receivedMessage);

		try {
			if (!file.exists()) {
				file.createNewFile();

				Send_Result_Message_TO_Client(socket,
						"File Created Successfully");

				if (file.isFile()) {

					if (new File(receivedMessage.trim()).canRead())

					if (new File(receivedMessage.trim()).canWrite())
						Log.e("File Write", " " + receivedMessage);

				}

			} else {
				Send_Error_Message_TO_Client(socket, receivedMessage
						+ " File Already Exist : " + receivedMessage);
				

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Send_Error_Message_TO_Client(socket, "No Permission to Read Write");

		} 
	}

	public void createDIRECTORY(Socket socket, String receivedMessage) {
		// TODO Auto-generated method stub
		File directory = new File(receivedMessage);
       
        	
		if (!directory.exists()) {
			
			try {
				p = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				stdin = new DataOutputStream(p.getOutputStream());

				stdin.writeBytes("mkdir " + "'"+receivedMessage+"'" + "\n");
				stdin.flush();
				stdin.writeBytes("exit\n");
				stdin.flush();

				bufferRD = new BufferedReader(new InputStreamReader(
						p.getErrorStream()));

				s = bufferRD.readLine();
				if (s == null) {
					try {

						Log.e("createDIR", "Directory Created Successfully");
						Send_Result_Message_TO_Client(socket,
								"Directory Created Successfully");
						bufferRD.close();
					} catch (IOException ioexception) {
						Log.e("createDIR", "Error in createDIR.");
						ioexception.printStackTrace();
						Send_Error_Message_TO_Client(socket,
								"No Permission to Read and Write");
					}
				} else {
					Send_Error_Message_TO_Client(socket,
							"Directory Already Exist: " + receivedMessage);
				}

				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Send_Error_Message_TO_Client(socket,
						"No Permission to Read and Write");
			}

		} else {
			
			if(directory.isDirectory()){
				
				Log.e("Directory Already Exist", "Exist");
				Send_Error_Message_TO_Client(socket, "Directory Already Exist: "
						+ receivedMessage);
				
			}else{
			
				Log.e("Directory Name File Already Exist", "File Exist");

				
				try {
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("mkdir " + "'"+receivedMessage+"'" + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getErrorStream()));

					s = bufferRD.readLine();
					if (s == null) {
						try {

							Log.e("createDIR", "Directory Created Successfully");
							Send_Result_Message_TO_Client(socket,
									"Directory Created Successfully");
							bufferRD.close();
						} catch (IOException ioexception) {
							Log.e("createDIR", "Error in createDIR.");
							ioexception.printStackTrace();
							Send_Error_Message_TO_Client(socket,
									"No Permission to Read and Write");
						}
					} else {
						Send_Error_Message_TO_Client(socket,
								"Directory Already Exist: " + receivedMessage);
					}

					
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Send_Error_Message_TO_Client(socket,
							"No Permission to Read and Write");
				}

			
				Send_Error_Message_TO_Client(socket, "Directory name File Already Exist: "
						+ receivedMessage);
				
			}
			
		}
      

	}

	public void renameFILE_DIRECTORY(Socket socket, String oldName,
			String newName) {

		File file = new File(oldName);
		File file2 = new File(newName);

		try {

			if (file.exists()) {
				boolean success = file.renameTo(file2);
				if (!success) {
					Send_Error_Message_TO_Client(socket, "Error in Renameing");
				} else {
					Send_Result_Message_TO_Client(socket, "Rename Successfull");
				}
			} else {
				Send_Result_Message_TO_Client(socket, oldName
						+ "File Does not Exist");
			}
		} catch (Exception ex) {
			Send_Result_Message_TO_Client(socket, oldName
					+ "No Permission to Modify");

		}
	}

	public void zipIt(String zipFile) {

		byte[] buffer = new byte[1024];

		try {

			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);


			for (String file : ListFiles.fileList) {

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

	public void generateFileList(File node) {
		try {
			// add file only
			if (node.isFile()) {
				fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
			}

			if (node.isDirectory()) {
				String[] subNote = node.list();
				for (String filename : subNote) {
					generateFileList(new File(node, filename));
				}
			}
		} catch (Exception ex) {
			Log.e("Exception in ", "Generate File List");
		}
	}

	private String generateZipEntry(String file) {
		return file.substring(MyService.SOURCE_FOLDER.length() + 1,
				file.length());
	}

	public void Save_File(Socket socket, String fileName,
			StringBuffer lineBuffer) {
		// TODO Auto-generated method stub

		File f = new File(fileName);
		boolean canRead = false;
		boolean canWrite = false;

		try {
			if (f.exists()) {
				if (f.canRead()) {
					canRead = true;
				} else {
				}
				if (f.canWrite()) {
					canWrite = true;

					try {

						out = new BufferedWriter(new FileWriter(f, false));

						out.append(lineBuffer);

						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					Send_Result_Message_TO_Client(socket,
							"Override Complets Complete");

				} else {

					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());

					stdin.writeBytes("chmod 777 " + fileName + "\n");

					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();


					bufferRD = new BufferedReader(new InputStreamReader(
							p.getErrorStream()));

					s = bufferRD.readLine();
					if (s == null) {
						try {
							canWrite = true;
							Log.e("SAVE", "Error in Chmod.");

							bufferRD.close();
						} catch (IOException ioexception) {
							Log.e("SAVE", "Error in Chmod.");
							ioexception.printStackTrace();

						}
					}

					try {
						out = new BufferedWriter(new FileWriter(f, false));
						out.append(f.getName() + "\n" + lineBuffer);

						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					Send_Result_Message_TO_Client(socket,
							"Overrite Complets Complete");

				}

			} else {
				Send_Error_Message_TO_Client(socket, "File Does not Exist."
						+ fileName);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (Exception ex) {
			//ex.printStackTrace();
		}
	}

	public void Change_Mod_File_Directory(String sOURCE_FOLDER) {
		// TODO Auto-generated method stub
		Log.e("Permission ", "-");
		try {
			p = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			stdin = new DataOutputStream(p.getOutputStream());

			stdin.writeBytes("chmod -R 777 " + sOURCE_FOLDER + "\n");

			stdin.flush();
			stdin.writeBytes("exit\n");
			stdin.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			String line;
			while ((line = br.readLine()) != null) {
			}
			br.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	void Send_Result_Message_TO_Client(Socket socket, String msg) {

		try {
			socket.setSendBufferSize(10000);
			socket.setTcpNoDelay(true);

			OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(Auth.getTokn() + "#" + msg);
			os.flush();
			bw.flush();
			socket.close();

			Log.e("Result Message: ", "Send Complet");
			
		} catch (Exception ex) {
			//ex.printStackTrace();
		}

	}

	void Send_Error_Message_TO_Client(Socket socket, String msg) {

		try {
			socket.setSendBufferSize(1000);
			socket.setTcpNoDelay(true);

			OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(Auth.getTokn() + "#" + msg);
			os.flush();
			bw.flush();
			socket.close();

			Log.e("Error Message: ", "Send Complet");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void Push_File(Socket socket, String fileToPush,
			String fileDestinationtoPush, Context context) {
		// TODO Auto-generated method stub

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		boolean canRead = false;
		boolean canWrite = false;
		File tempFile = new File("/sdcard/POC/" + fileToPush);
		try {
			if (tempFile.createNewFile()) {
				if (tempFile.exists()) {
					if (tempFile.canWrite() && tempFile.canRead()) {
						canRead = true;
						canWrite = true;

						prefs.edit()
								.putString("pushFile",
										tempFile.toString().trim()).commit();
						prefs.edit()
								.putString("destPath",
										fileDestinationtoPush.toString().trim())
								.commit();
						prefs.edit().putBoolean("sendingFile", true).commit();
						Log.e("Temp File Ready to Write", "FRead: " + canRead
								+ " FWrite: " + canWrite);
						Send_Result_Message_TO_Client(socket,
								"FILE READY TO PUSH");

					} else {
						Send_Result_Message_TO_Client(socket,
								"NO PERMISSION TO READ AND WRITE");

					}
				} else {
					Send_Error_Message_TO_Client(socket,
							"TEMP FILE DOES NOT EXIST" + tempFile);
				}

			} else {
				Send_Error_Message_TO_Client(socket,
						"TEMP FILE ALREADY EXIST IN /POC/" + tempFile);

			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Send_Error_Message_TO_Client(socket,
					"NO PERMISSION TO READ AND WRITE: " + tempFile);

		}
	}

	public void IsAvailable(Socket socket, String fileName, String destination,
			Context context) {
		// TODO Auto-generated method stub

		File file = new File(destination + fileName);
		boolean canRead = false;
		boolean canWrite = false;

		if (file.exists()) {
			if (file.canRead())
				canRead = true;
			if (file.canWrite())
				canWrite = true;
			
			Send_Result_Message_TO_Client(socket, "FILE ALREADY EXIST");
		} else {
			Log.e("File : ", "FILE DOES NOT EXIST");
			Send_Error_Message_TO_Client(socket, "FILE DOES NOT EXIST");
		}

	}

	public void Write_ToFile(Socket socket, Context context) {
		// TODO Auto-generated method stub
		try {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			String tempFile = prefs.getString("pushFile", "");
			String destPath = prefs.getString("destPath", "");
			prefs.edit().putBoolean("sendingFile", false).commit();

			
			FileOutputStream fos = new FileOutputStream(tempFile);

			int count = 0;
			InputStream is = socket.getInputStream();

			int buffSize = socket.getReceiveBufferSize();
			byte[] buffer = new byte[buffSize];

			while ((count = is.read(buffer)) >= 0) {
				fos.write(buffer, 0, count);
			}

			is.close();
			fos.close();
			socket.close();

			
			p = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			stdin = new DataOutputStream(p.getOutputStream());

			stdin.writeBytes("cp -r " + tempFile + " " + destPath + "\n");
			stdin.flush();
			stdin.writeBytes("exit\n");
			stdin.flush();

			bufferRD = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			s = bufferRD.readLine();
			if (s == null) {
				try {
			
					bufferRD.close();
				} catch (IOException ioexception) {
					ioexception.printStackTrace();

				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block

			Send_Error_Message_TO_Client(socket, "No Permission to Read Write");
		}
	}

	@SuppressLint("SimpleDateFormat")
	public void Get_Current_Screen(Context context) {

		// TODO Auto-generated method stub
		Process sh;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
		currentDateandTime = sdf.format(new Date());
		System.out.println("currentDate: " + currentDateandTime);
		currentScreen = "screen_" + currentDateandTime + ".png";
		System.out.println("ScreenName: " + currentDateandTime);
		prefs.edit().putString("currentScreen", "/sdcard/POC/" + currentScreen)
				.commit();

		try {
			sh = Runtime.getRuntime().exec("su", null, null);
			String comando = "/system/bin/screencap -p " + "/sdcard/POC/"
					+ currentScreen + " \n";

			DataOutputStream os = new DataOutputStream(sh.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			int i = sh.waitFor();
			Thread.sleep(1000);

			bufferRD = new BufferedReader(new InputStreamReader(
					sh.getErrorStream()));

			s = bufferRD.readLine();
			if (s == null) {
				try {
					bufferRD.close();
				} catch (IOException ioexception) {
					
					ioexception.printStackTrace();

				}

			}
			sh.destroy();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();


		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("IOException", "");
		}

	}
	
	private void getapkdb(String directory){


		try {
			if (new File(directory).exists()
					&& new File(directory).isDirectory()) {

				listString = new StringBuffer();
				p = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				stdin = new DataOutputStream(p.getOutputStream());
				stdin.writeBytes("ls -R " + directory + "\n");
				stdin.flush();
				stdin.writeBytes("exit\n");
				stdin.flush();

				bufferRD = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String line = null;

				while ((line = bufferRD.readLine()) != null) {
				

					
					Filename myHomePage = new Filename(line,
								'/', '.');

						if (myHomePage.extension().equals("db")) {


							String temp = path+line;
							String dbfilepath = temp.replaceAll(":", "/");
							apkdbfiles.add(dbfilepath);
						}else{
								if(line.toString().contains(":"))
								{

									path=line.toString();
								}else{
								
									}
							
						}
					
				}
					

			} else {

				System.out.println("Directory - " + directory + " Not Exist");

			}
		} catch (NullPointerException e) {
		
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	private void getFeatureList(ArrayList lis) {


		for (int i = 0; i < lis.size(); i++) {

			try {

				listString = new StringBuffer();				
				p = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });      
				stdin = new DataOutputStream(p.getOutputStream());
				stdin.writeBytes("ls -l " + lis.get(i).toString() + "\n");
				stdin.flush();
				stdin.writeBytes("exit\n");
				stdin.flush();

				bufferRD = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String line = null;

				while ((line = bufferRD.readLine()) != null) {

					databasesList.add(lis.get(i).toString() + " "
							+ line.toString());
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	private void getDatabaseList(ArrayList lis) {
		try {
			for (int i = 0; i < lis.size(); i++) {
				String[] splited = lis.get(i).toString().split("\\s+");
				for (int j = 0; j < splited.length; j++) {
					if (j == 0) {

						String dd=splited[j];
                        String filename=dd.substring(dd.lastIndexOf("/")+1,dd.length());
						
						listString.append(filename);
						listString.append("\t"+splited[j]);

					} else if (j == 4) {


						listString.append("\t" + splited[j]);

					} else if(j == 5){
				

						listString.append("\t" + splited[j]);

					} else if(j==6){
						
						listString.append("\t" + splited[j]);
						
					}else{
						
					}

					Thread.sleep(100);

				}
				listString.append("<br>");
			}

			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}