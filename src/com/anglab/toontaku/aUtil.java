package com.anglab.toontaku;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

@SuppressLint("SimpleDateFormat")
public class aUtil  extends Activity {
	static HashMap<String, Long> gv_timeChk = new HashMap<String, Long>(); // �ð� üũ ���� �ؽø�
	// true�� ����. false�� ����.
	// chkTimer("ggg", 120); // ���ݺ��� 120�� ���� ggg�� ȣ�������� true
	public static boolean chkTimer(String pTimerId, int pMinuteTerm ) {
		if ( gv_timeChk.containsKey(pTimerId) ) {
			Long vChkCal = System.currentTimeMillis() - (pMinuteTerm * 1000); // �ʴ�����ȯ �� ���
			Log.d("chkTimer", pTimerId + " : " + vChkCal + " / " +  gv_timeChk.get(pTimerId) + " : " + (vChkCal > gv_timeChk.get(pTimerId)));
			if ( vChkCal <= gv_timeChk.get(pTimerId) ) return true;
		}
		gv_timeChk.put(pTimerId, System.currentTimeMillis());
		return false;
	}
	
	public static int delXmlTimer() {
		ArrayList<String> vArrayList = new ArrayList<String>();
		for ( Entry<String, Long> entry : gv_timeChk.entrySet() ) {
	        String key = entry.getKey();
	        if ( key.indexOf("getXmldata/") == 0 ) vArrayList.add(key);
    		//Log.d("delXmlTimer", key);
	        //Long value = entry.getValue();
		}

		for ( String vRemovekey : vArrayList ) {
			gv_timeChk.remove(vRemovekey);
    		//Log.d("gv_timeChk.remove", vRemovekey);
		}
		return vArrayList.size();
	}

	// 24�ð��� �Ͻø� ����(is_new �Ǵܿ� ���)
	public static String getDataCal() {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss"); 
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, -1); 
		return fmt.format(calendar.getTime());
	}

    public static String getVersionName(Context context) {
        try {
            PackageInfo pi= context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }

    public static String[] vValueBefore = { "A"  , "B"      , "C"     , "D"         , "E"        , "F"         , "G"     , "H"   , "I"       
			                              , "J"       , "K"     , "L"        , "M"   , "N"   , "O"         , "P"         , "Q"     , "R"
			                              , "S"     , "T"      , "U"};
    public static String[] vValueAfter  = { "CID", "COMP_YN", "ID_SEQ", "IMG_VIEWER", "LINK_CODE", "LST_UPD_DH", "MAX_NO", "NAME", "SEL_MODE"
			                              , "SET_CONT", "SET_ID", "SET_VALUE", "SITE", "SORT", "THUMB_COMN", "THUMB_NAIL", "USE_YN", "CNT"
			                              , "ARTIST", "SELL_YN", "ORG_UPD_DH"};
	// Ʈ������ ���̱����� ������ ���� �ٲ��. ġȯ���ִ� ��Ȱ
    public static String sectionFind(String pTagname) {
		for ( int i = 0; i < vValueBefore.length; i++ ) {
			if ( pTagname.equals(vValueBefore[i]) ) return vValueAfter[i];
		}
		return pTagname;
	}
    
    public static void alertD(Context context, String pStr) { // ���̾�α� ��
	    AlertDialog.Builder alt_bld = new AlertDialog.Builder(context);
	    alt_bld.setMessage(pStr)
				.setCancelable(false)
				.setPositiveButton(R.string.str_yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = alt_bld.create();
		alert.setTitle(R.string.str_alert);
		alert.show();
    }
}
