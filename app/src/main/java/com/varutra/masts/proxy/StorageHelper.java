/** 
 * @Class Name :  StorageHelper.java
 */

package com.varutra.masts.proxy;

/**
 * @author     :  Varutra Consulting Pvt. Ltd.
 * @Create On  :  Jun 16, 2014 6:23:19 PM
 * @License    :  Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights reserved.
 */
import android.os.Environment;

public class StorageHelper {
	// Storage states
	private boolean externalStorageAvailable, externalStorageWriteable;

	/**
	 * Checks the external storage's state and saves it in member attributes.
	 */
	private void checkStorage() {
		String state = Environment.getExternalStorageState();

		if (state.equals(Environment.MEDIA_MOUNTED)) {
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			externalStorageAvailable = externalStorageWriteable = false;
		}
	}

	/**
	 * Checks the state of the external storage.
	 * 
	 * @return True if the external storage is available, false otherwise.
	 */
	public boolean isExternalStorageAvailable() {
		checkStorage();

		return externalStorageAvailable;
	}

	/**
	 * Checks the state of the external storage.
	 * 
	 * @return True if the external storage is writeable, false otherwise.
	 */
	public boolean isExternalStorageWriteable() {
		checkStorage();

		return externalStorageWriteable;
	}

	/**
	 * Checks the state of the external storage.
	 * 
	 * @return True if the external storage is available and writeable, false
	 *         otherwise.
	 */
	public boolean isExternalStorageAvailableAndWriteable() {
		checkStorage();

		if (!externalStorageAvailable) {
			return false;
		} else if (!externalStorageWriteable) {
			return false;
		} else {
			return true;
		}
	}
}