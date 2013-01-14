/**
 * 
 */
package org.commcare.android.database;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabase.CursorFactory;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.commcare.android.database.user.models.ACase;
import org.commcare.android.database.user.models.FormRecord;
import org.commcare.android.database.user.models.GeocodeCacheModel;
import org.commcare.android.database.user.models.SessionStateDescriptor;
import org.commcare.android.db.legacy.LegacyCommCareUpgrader;
import org.commcare.android.javarosa.AndroidLogEntry;
import org.commcare.android.javarosa.DeviceReportRecord;
import org.commcare.android.logic.GlobalConstants;
import org.commcare.android.models.User;
import org.commcare.resources.model.Resource;
import org.javarosa.core.model.instance.FormInstance;

import android.content.Context;

/**
 * @author ctsims
 *
 */
public class CommCareOpenHelper extends SQLiteOpenHelper {
	
	
	/*
	 * Version History:
	 * 28 - Added the geocaching table
	 * 29 - Added Logging table. Made SSD FormRecord_ID unique
	 * 30 - Added validation, need to pre-flag validation
	 */
	
    private static final int DATABASE_VERSION = 30;
    private Context context;
    
    public CommCareOpenHelper(Context context) {
    	this(context, null);
    }

	public CommCareOpenHelper(Context context, CursorFactory factory) {
        super(context, GlobalConstants.CC_DB_NAME, factory, DATABASE_VERSION);
        this.context = context;
	}
	
	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	@Override
	public void onCreate(SQLiteDatabase database) {
		
		try {
			database.beginTransaction();
			
			TableBuilder builder = new TableBuilder(ACase.STORAGE_KEY);
			builder.addData(new ACase());
			builder.setUnique(ACase.INDEX_CASE_ID);
			database.execSQL(builder.getTableCreateString());
			
			builder = new TableBuilder(User.STORAGE_KEY);
			builder.addData(new User());
			database.execSQL(builder.getTableCreateString());
			
			builder = new TableBuilder(FormRecord.class);
			database.execSQL(builder.getTableCreateString());
			
			builder = new TableBuilder(SessionStateDescriptor.class);
			database.execSQL(builder.getTableCreateString());
			
			builder = new TableBuilder(GeocodeCacheModel.STORAGE_KEY);
			builder.addData(new GeocodeCacheModel());
			database.execSQL(builder.getTableCreateString());
			
			builder = new TableBuilder(AndroidLogEntry.STORAGE_KEY);
			builder.addData(new AndroidLogEntry());
			database.execSQL(builder.getTableCreateString());
			
			builder = new TableBuilder(DeviceReportRecord.STORAGE_KEY);
			builder.addData(new DeviceReportRecord());
			database.execSQL(builder.getTableCreateString());
			
			
			database.setVersion(DATABASE_VERSION);
					
			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
	}

	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		LegacyCommCareUpgrader upgrader = new LegacyCommCareUpgrader(context);
		
		//Evaluate success here somehow. Also, we'll need to log in to
		//mess with anything in the DB, or any old encrypted files, we need a hook for that...
		upgrader.doUpgrade(database, oldVersion, newVersion);
	}

}
