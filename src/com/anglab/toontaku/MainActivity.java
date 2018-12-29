package com.anglab.toontaku;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.android.vending.billing.IInAppBillingService;
import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.anglab.toontaku.ListViewer.ListAdapterWithButton;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// �����׸� 33BB77 (�۾�, ������)
// ������� 333333 (�۾�)
// ������ FFFFFF
// ���1 F4F4F4 (����ȸ�����)
// ���2 E4E4E4 (����)

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnTouchListener,OnClickListener  {
	WebView mWebView;
	Spinner cbo_no;
    private NotesDbAdapter dbAdapter;
    static int gv_currentViewId = -1; // ���� ȭ�� ����

	List<HashMap<String, String>> list = new ArrayList<>();
	List<HashMap<String, String>> gv_backList = new ArrayList<>(); // �ڷΰ��� ��ư ����

	HashMap<String, String> gv_setting   = new HashMap<String, String>(); // ���α׷��������� VER���ù���, URL���α׷� URL,VIEW_FIRST_YNùȸ���� �����ΰ� ����ȸ���� �����ΰ�, THUMB_YN ������� ǥ���Ұ��ΰ�
	HashMap<String, String> gv_imgViewer = new HashMap<String, String>(); // ����Ʈ�� ���� ��ȸ �ּ�
	HashMap<String, String> gv_thumbComn = new HashMap<String, String>(); // ����Ʈ�� ����� ��ȸ �ּ�
	HashMap<String, int[]>  gv_buttons   = new HashMap<String, int[]>();  // ��ġ�� ��ư ���� �ٲ�����Ѱ��� ����
	static HashMap<String, String> gv_setView = new HashMap<String, String>(); // �������µ� �ʿ��� ����. CID,SITE,LST_VIEW_NO,MAX_NO,CBO_INDEX

	String vMode = "0"; // 0���������, 1����Ʈ��Ͽ�����Ŭ��, 2������Ͽ����� Ŭ��, 3ȸ��������ȸ(2�����Ͽ�����Ŭ�����Ľ���ȴ�)
	String gv_nav = ""; // ������̼� ���� ���� (���� ���� ����)
	String gv_isNewDate = ""; // ����Ͻú��� �� ����Ǹ� �� �ð� ������ �Խù��� NEW

	String gv_svrUrlOrg = "http://toontaku.dothome.co.kr/toontaku/"; // �⺻�߿� �⺻.
	String gv_pgmName = "a.php";
	String gv_svrUrl = gv_svrUrlOrg; // ���� �ּ�. �Ŀ� setting���� ��̷� �о���°ɷ� ��ü.
	ArrayList<String> gv_svrList = new ArrayList<String>(); // �������� ���α׷� �ּ�
	int gv_svrUrlArrayIndex = 0; // �������� �ּ� ��� ���� �ε���

	IInAppBillingService mService;
	IabHelper mHelper;
	String[] gv_sectionG ,gv_sectionV; // ���ǰ��� ���
	Context gv_this = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.loading);

        dbAdapter = new NotesDbAdapter(this);
        dbAdapter.open();

        /**********************************************/
        if ( "����ǥ���̸鰳�߼�����".equals("no") ) { //TODO ���߼����� ������ true
            //dbAdapter.fn_dbClear(); // ��� �ʱ�ȭ. ONLY TEST!!
        	gv_svrUrlOrg = "http://anglab.dothome.co.kr/toontaku/";
        	gv_pgmName = "c.php";

        	//gv_svrUrlOrg = "http://anglab.url.ph/toontaku/";
        	//gv_pgmName = "a.php";
        }
        /**********************************************/

        // ������� ����
        // ��Ű���� ��������� ����. ( => ��� ������ �ȵ���̵忡�� �۵���Ű�� ����.)
        Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        intent.setPackage("com.android.vending"); // ������å�̶��� ks20160728
        bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
 
        //bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApsZz3yn0tSyZqQedUXj8U8aHvP3UniO+ugJk/10tqcgfIiMH5WVkWOdTR+ZW0/40qfiC3iToZMyJkanFPrsI9LUA2AiWU23Q2Gqg0YvvVhPVoEROKb1KFfydN5xlWsc367EF42MFWZmCqRtv3I4mimaHQ1lp0rS18rknTluc8EcI2SCx71GN4pD5LyRMaxBxJXZUwlpURAmmGKFYFlrARuzowrVrrCktIZR5tynDbBkMhSRV/HPYS/mRk4zg3UEWW//c4AvanqSry6ZYgr0c5aG218TiDOpciaz7ML3VB/Rvl9g2Tn8lMvOjUm2MMqhfzIm8r7G434SIe7F9+Db/jwIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                	// ���ſ���ó�� ( �佺Ʈ�ϳ� ���� �����˾� �����Ű�� �ǰڽ��ϴ� )
                }
                AlreadyPurchaseItems();    // AlreadyPurchaseItems(); �޼���� ���Ÿ���� �ʱ�ȭ�ϴ� �޼����Դϴ�. v3���� �Ѿ���鼭 ���ű���� ��� ���� �Ǵµ� �籸�� ������ ��ǰ( ���ӿ����� ���ΰ����������� ) ������ �������־�� �մϴ�.  �� �޼���� ��ǰ ������ Ȥ�� �Ŀ� �ݵ�� ȣ���ؾ��մϴ�. ( �籸�Ű� �Ұ����� 1ȸ�� �������ǰ�� ȣ���ϸ� �ȵ˴ϴ� )
            }
        });
		// ������� ��
	    (new initLoadings()).execute();
	}

	// "V0" : [������������]�� ����� �Ķ���ͷ� ���� ������Ʈ ����(THUMB_NAIL, MAX_NO)�� �����´�. (TB_WT003�� �Ȱ����´�.)
	// "V1" : �߰��� �����̳�, �ϰῩ�κ���, ���� ���� �̺�ư�� �ִ� ���� ������ �����´�. ���� �����, ������ȸ�� �ֽ�ȭ �߽߰õ �ߵ�. ����Ʈ ��Ͽ��� ����������� ���� �ߵ�. LOCAL_LST_UPD_DH
	// "2" : ������ ������Ʈ��� TB_WT002 ������ �����´�.
	// UPD_MYLIST : [������������]�� ����� �Ķ���ͷ� ���� updateBot�� ��ȯ�Ѵ�. �����ϸ� SITE_SEARCHER(siteSearcher)�� UPD_ALL(updateBot�� ALL)�� ��ȯ�Ѵ�.
	// SITE_SEARCHER : [������������]�� updateBot�� �����Ҷ�(UPD_MYLIST) �ߵ�. siteSearcher.php�� ȣ���Ѵ�.
	// UPD_ALL : ���� ���� ���ǿ��� updateBot.php�� UPD_ALL �ɼ����� ȣ��
	private List<HashMap<String, String>> getXmldata(String pMode, String pParam) {
		List<HashMap<String, String>> vList = new ArrayList<>();
		try {
			String vParam = "";
			String vLstUpdDh = "";
			Long vLstUpdDhLong = (long)0;
			InputStream in = null;

			if ( "-1".equals(pParam) // �ʱ� �����̿��� ���� ���� �д´�.
			  && ( "V2".equals(pMode) || "2".equals(pMode) || "3".equals(pMode) || "4".equals(pMode) ) ) {
				in = getAssets().open("xml_list_" + pMode + ".xml");
			} else {
				vParam = pParam;
				// �Ķ���� ����
				if ( "2".equals(pMode) || "4".equals(pMode) || "V0".equals(pMode) // ���״� pParam�� �ȵ��´�.
				  || "3".equals(pMode) || "V1".equals(pMode) ) { // pParam = ID_SEQ // pParam = ����Ʈ
					vLstUpdDh = inqXmlParam(pMode, vParam);
					if ( "-1".equals(vLstUpdDh) ) return vList;
					if ( "V0".equals(pMode) ) vParam = fn_inqMyListToString(); 

				} else if ( "V2".equals(pMode) ) { // �����߰�
					vLstUpdDh = getSetting("FST_INS_DH");
					if ( "".equals(vLstUpdDh) ) return vList;

				} else if ( "UPD_MYLIST".equals(pMode) ) { // ��������� / �������������� ������Ʈ Ȯ��
					vParam = fn_inqMyListToString(); 
					if ( "".equals(vParam) ) return vList;

				} else {
					vLstUpdDh = vParam; // pParam�� ���� lst_upd_dh�� ������.
				}

				try { vLstUpdDhLong = Long.valueOf(vLstUpdDh); } catch(Exception e) { }
				if ( aUtil.chkTimer("getXmldata/" + pMode + "/" + vParam + "/" + vLstUpdDh, 10 * 60) ) { // ks20141130 10�� ���� ȣ������������ ����
					if ( "6".equals(pMode) || "VB".equals(pMode)  ) { //TODO VB����� 6�� �α������� �Ź� Ʈ���� �Ͼ��. DB�� ������ ���ؼ��϶�.
					} else {
						return vList; // �Ź��� Ʈ���� �ִ°��� ȸ���ϱ� �����̴϶�.
					}
				}
				Log.d("����", "getXmldata/" + pMode + "/" + vParam + "/" + vLstUpdDh);
				String vProgram = gv_pgmName;
				if ( "1".equals(pMode) // 1��������Ʈupdate �Ⱦ���.
				  || "4".equals(pMode) || "6".equals(pMode) || "2".equals(pMode)  // 4���� 6��õ���� 2����Ʈ����Ʈ
				  || "3".equals(pMode)  // 3ī��ȸ������ ����ȸ�� ��ȸ�ô� ���۾�. ���ÿ� ����� ������ �����Ͽ� ���� ����Ʈ�� �μ�Ʈ�Ѵ�. Ʈ���� ���� ����
				  || "V0".equals(pMode) // V0��������������������Ʈ LST_UPD_DH|ID_SEQ/ID_SEQ/ID_SEQ/...
				  || "V1".equals(pMode) // V1����Ʈ��������Ʈ�� LST_UPD_DH|SITE
				  || "VB".equals(pMode) // VB������������ ��������� �������� ��ȸ.
				  || "V2".equals(pMode) ) {
				} else if ( "UPD".equals(pMode) || "UPD_MYLIST".equals(pMode) // UPD cid������ƮȮ��  / ������������ ������Ʈ 
						 || "UPD_ALL".equals(pMode) ) { // ��ü ������Ʈ
					vProgram = "updateBot.php";
				} else if ( "SITE_SEARCHER".equals(pMode) ) { // �ű�����Ž�� ks20150119
					vProgram = "siteSearcher.php";
				} else { // erró��
					return vList;
				}
				URL url = new URL(gv_svrUrl + vProgram + "?pMode=" + pMode + "&pMyNm=" + getSetting("MY_NM") + "&pAppVer=" + getSetting("APP_VER") + "&pLstUpdDh=" + vLstUpdDh + "&pParam=" + vParam);
				Log.d("url", "url : " +url);
				in = url.openStream();
			}

			XmlPullParserFactory fatorry = XmlPullParserFactory.newInstance();
			fatorry.setNamespaceAware(true);
			XmlPullParser xpp = fatorry.newPullParser();
			xpp.setInput(in, "utf-8"); // xpp.setInput(in, "euc-kr");

			int eventType = xpp.getEventType();
			String tagname = "";
			String vFirstTagName = ""; // ���� �±׳����� �����Ѵ�. list�� ��Ȱ�� ������ ����.
			HashMap<String, String> data = new HashMap<String, String>();

			while ( eventType != XmlPullParser.END_DOCUMENT ) {
				if ( eventType == XmlPullParser.START_TAG ) {
					tagname = xpp.getName();
				} else if ( eventType == XmlPullParser.TEXT ) { // �±׺��� ����
					/**** LMultiData �������� �����ϱ����� �ļ�. ũ�� �Ű澵 �ʿ� ����. ****/
					if ( vFirstTagName.equals("") ) vFirstTagName = tagname;
					if ( vFirstTagName.equals(tagname) ) {
						if ( !data.isEmpty() ) {
							if ( !data.isEmpty() && "3".equals(pMode) && !data.containsKey("ID_SEQ") ) data.put("ID_SEQ", vParam);
							vList.add(new HashMap<String, String>(data));
							data.clear();
						}
					}
					/********/
					if ( "F".equals(tagname) && !"-1".equals(pParam) ) { // F:LST_UPD_DH �� ��� ���ڰ� ����Ǿ��ִ�. ���� Ǯ� �Է� ks20140416
						data.put(aUtil.sectionFind(tagname), Long.valueOf(xpp.getText()) + vLstUpdDhLong + ""); // �̰� �߿��ѰŴ�.
					} else {
						data.put(aUtil.sectionFind(tagname), xpp.getText()); // �̰� �߿��ѰŴ�.
					}
				} else if ( eventType == XmlPullParser.END_TAG ) {
				}
				eventType = xpp.next();
			}

			if ( !data.isEmpty() && "3".equals(pMode) && !data.containsKey("ID_SEQ") ) data.put("ID_SEQ", vParam);
			if ( !data.isEmpty() ) vList.add(new HashMap<String, String>(data));
//Log.d("list", vList.toString());
			if (  "1".equals(pMode) ||  "2".equals(pMode)  // 1��������Ʈ update / 2����Ʈ ����Ʈ  
			  ||  "3".equals(pMode) ||  "4".equals(pMode)  // 3ī��ȸ������ / 4����
			  || "V0".equals(pMode) || "V1".equals(pMode)  // ��������ϸ� ������Ʈ // ����Ʈ�� ������Ʈ
			  || "V2".equals(pMode) ) { // �ű�����������Ʈ
				dbAdapter.updList(pMode, vList);    // ����DB�� ������Ʈ�Ұ������� ����
			} else if ( "UPD".equals(pMode) ) { // ������Ʈ(Ư������)
				//if ( vList.size() < 0 && vList.get(0).containsKey("UPD") ) vRtn = (String) vList.get(0).get("UPD"); // ������Ʈ���� ������ Y. �ƴϸ�N
			}
		} catch (Exception e) {
			e.printStackTrace();
			gv_svrUrlArrayIndex++;
			aUtil.delXmlTimer();
			//if ( gv_svrUrlArrayIndex < 2){//gv_svrList.size() ) { // �ѹ��� �ٵ��Ƽ� ������ ��
			if ( gv_svrUrlArrayIndex < gv_svrList.size() ) { // �ѹ��� �ٵ��Ƽ� ������ ��
				gv_svrUrl = gv_svrList.get(gv_svrUrlArrayIndex); // �������� ���α׷� �ּ�
				return getXmldata(pMode, pParam);
			} else { // �ѹ��� �� ������ ����. ����Ʈ ó������ ������ �̹�Ʈ�������� nulló��
				alert(R.string.str_netErr);
				gv_svrUrlArrayIndex = 0;
				gv_svrUrl = gv_svrList.get(gv_svrUrlArrayIndex); // �������� ���α׷� �ּ�
			}
		}
		return vList;
	}

	// ������ �������� �ʿ��� 6���� ������ �����Ѵ�.
	// CID,ID_SEQ,SITE,LST_VIEW_NO,MAX_NO,CBO_INDEX
	public void fn_setViewSetting(String pTag, String pValue) {
		gv_setView.put(pTag, pValue); // gv_setView �� �ִ� ���� �� ���� ������ �ϰ� �̷��� ¥�ô�.
	}

	private void fn_viewerSetting(final String pParam) {
		fn_saveBack("fn_viewerSetting", pParam);
		if ( !"3".equals(vMode) ) ((TextView)findViewById(R.id.txt_nav)).setText(gv_nav);

		fn_chgContentView(R.layout.view_mode);

		if ( "5".equals(vMode) ) { // �������� ȣ���� ��ũ ������ �ε���
			fn_menuSlide();
			if ( isMenuExpanded ) fn_menuSlide(); // ������̼ǹٰ� ���������� ����־�.
			mWebView.loadUrl(pParam);
		} else {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fn_convArrayList("NAME"));
			cbo_no = (Spinner)findViewById(R.id.cbo_no);
			cbo_no.setAdapter(adapter);
			cbo_no.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					if ( gv_setView.containsKey("CBO_INDEX") && (arg2 + "").equals(gv_setView.get("CBO_INDEX")) ) return;
					fn_goSite(pParam, arg2);
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
			});

			int index = fn_findListRow("LINK_CODE", gv_setView.get("LST_VIEW_NO"));
			if ( index < 0 ) { // �˻��� ����� ���� = LST_VIEW_NO �� ���� = ùȭ���ͺ��ô�.
				index = list.size() - 1;
			} else if ( index > 0 ) {
				if ( !"3".equals(vMode) ) index = index - 1; // ������Ʈ�� ȭ�� ������, �ֱٺ� �� ���� ȸ���� �����ش�.
			}
			fn_goSite(pParam, index);
		}
	}

	// ȸ���� pParam�� �־� ȣ���ϸ� gv_cid�� ���� �ش� ȸ�� ������ �����ش�.
	public void fn_goSite(String pIdSeq, int pIndex) {
		if ( pIndex < 0 ) { // �ֱٰǿ��� ����ȭ Ŭ��. ������Ʈ Ȯ���Ѵ�.
			if ( aUtil.chkTimer("������Ʈ�ߺ�"+pIdSeq, 10 * 60) ) { // 120��
				if ( aUtil.chkTimer("ToastClickTimer", 2) ) return; // ���� �ڷ� Ŭ������ 2�� �̳���
				alert(R.string.str_atLast);
				return;
			}
			(new NewIsThere()).execute(pIdSeq); //ks20141123 ������Ʈ üũ
			return;
		} else if ( pIndex >= cbo_no.getCount() ) { // ���ʰǿ��� ����ȭ Ŭ��
			alert(R.string.str_atFirst);
			return;
		}

		fn_setViewSetting("CBO_INDEX", pIndex + "");
		String vLinkCode = getList(pIndex, "LINK_CODE"); // ���� no �۷ι� ����
		cbo_no.setSelection(pIndex);

		if ( "kakao".equals(gv_setView.get("SITE")) ) { // īī�� ������ 5�����ۿ� ������. ��� �������ؼ� ��Ű�� ���������.
			CookieManager.getInstance().removeAllCookie(); // ks20140627
		}

		fn_setViewSetting("LST_VIEW_NO", vLinkCode); // �ֱ� ��ȸ�� LinkCode�� ������Ʈ�Ѵ�.
		
		String vUrl = gv_imgViewer.get(gv_setView.get("SITE")).replace("$cid", gv_setView.get("CID")).replace("$no", vLinkCode);

		if ( "naver".equals(gv_setView.get("SITE")) || "daum".equals(gv_setView.get("SITE")) ) { // ��å�� �ܺ� ������ ����Ʈ
			fn_updLstViewNo();

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(vUrl));
			startActivity(intent);
		} else { // �ξۺ�����
			mWebView.loadUrl(vUrl);
		}

		//Log.d("webview url", gv_imgViewer.get(gv_setView.get("SITE")).replace("$cid", gv_setView.get("CID")).replace("$no", vLinkCode));
	}

	// pArrList �迭�� �Ѿ�� ���� ������ �����Ͽ� ����Ʈ�� �Ѵ�.
	// ������̼ǹٿ� pNav ���� �����Ѵ�. pTF �� true�� ���ξ��� �ƴϸ� �̾��
	public void fn_listAdapter(String pMode) {
		fn_chgContentView(R.layout.activity_main);
		TextView txt_nav = (TextView)findViewById(R.id.txt_nav);

		findViewById(R.id.img_arrow).setVisibility(View.GONE);
		findViewById(R.id.txt_noMyList).setVisibility(View.GONE);
		findViewById(R.id.txt_section_main).setVisibility(View.GONE);
		if ( "0".equals(pMode) ) {
			findViewById(R.id.main_search).setVisibility(View.GONE);
			txt_nav.setText(getResources().getString(R.string.app_name) + " > " + getResources().getString(R.string.str_myList));

			if ( list.size() == 0 ) { // ���� ���� ���� ������ �߰��϶�� �ȳ�
				findViewById(R.id.img_arrow).setVisibility(View.VISIBLE);
				findViewById(R.id.txt_noMyList).setVisibility(View.VISIBLE);
			}
		} else if ( "1".equals(pMode) ) {
			findViewById(R.id.main_search).setVisibility(View.VISIBLE);
			txt_nav.setText(getResources().getString(R.string.app_name) + " > " + getResources().getString(R.string.str_search));
		} else if ( "2".equals(pMode) ) {
			findViewById(R.id.main_search).setVisibility(View.VISIBLE);
			txt_nav.setText(gv_nav);
			fn_setSection();
		} else if ( "3".equals(pMode) ) {
			findViewById(R.id.main_search).setVisibility(View.GONE);
			txt_nav.setText(gv_nav);
		} else if ( "4".equals(pMode) ) {
			findViewById(R.id.main_search).setVisibility(View.VISIBLE);
			txt_nav.setText(gv_nav);
			fn_setSection();
			if ( list.size() == 0 ) { // �˻��ߴµ� ����� ����!
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("NAME", getResources().getString(R.string.str_noSearchData));
				list.add(0, data); // �˻��� ����� �����ϴ� ����
			}
		} else if ( "5".equals(pMode) ) { // ����
			findViewById(R.id.main_search).setVisibility(View.GONE);
			txt_nav.setText(getResources().getString(R.string.app_name) + " > " + getResources().getString(R.string.str_setting));

		} else if ( "6".equals(pMode) ) { // ��õ���� ks20141116
			findViewById(R.id.main_search).setVisibility(View.GONE);
			txt_nav.setText(getResources().getString(R.string.app_name) + " > " + getResources().getString(R.string.str_favo));
		}

		gv_isNewDate = aUtil.getDataCal(); // ������Ʈ ���� ���� ����. ���� ������ 24�ð���.
		ListAdapterWithButton<String> adapter = new ListAdapterWithButton<String>(this, list);
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);
	}

	/*****************************
	 * ��Ƽ������ ����
	 *****************************/
    /**
     * �޴� ��Ƽ������
     */
    class BackgroundTask extends AsyncTask<String, String , String[]> {
        protected void onPreExecute() {
    		CDialog.showProgress(gv_this);
        }
 
        // vMode 0������������ 1����Ʈ 2����Ʈ��������� 3����ȸ����� 4�˻� 5���� 6�α� 
        protected String[] doInBackground(String ... values) {
        	String vMode  = values[0];
        	String vParam = values[1];

        	List<HashMap<String, String>> vList = new ArrayList<>();
        	if ( "3".equals(vMode) || "6".equals(vMode) ) { 
                publishProgress(getResources().getString(R.string.str_confirmUpdating));
  				vList = getXmldata(vMode, vParam);
        	} else if ( "2".equals(vMode) ) {
                publishProgress(getResources().getString(R.string.str_confirmUpdating));
  				vList = getXmldata("V1", vParam);
        	} else if ( "0".equals(vMode) ) {
                publishProgress(getResources().getString(R.string.str_confirmUpdating));
  				vList = getXmldata("V0", "");
      		} else {
      			//Log.d("������ �Ȱ�", "������ �Ȱ�");
      		}

            publishProgress("");
    		if ( "6".equals(vMode) && !vList.isEmpty() ) { // �α�����
    			StringBuffer vStr = new StringBuffer();
    			for ( int i = 0; i < vList.size(); i++ ) {
    				vStr.append("SELECT '").append(vList.get(i).get("ID_SEQ")).append("' AS ID_SEQ, ").append(i).append(" AS SORT UNION ALL ");
    			}
        		list = dbAdapter.inqSql(vMode, vStr.toString()); 
    		} else {
        		list = dbAdapter.inqSql(vMode, vParam);
    		}
        	return values;
        }

        protected void onPostExecute(String[] values) {
			if ( gv_currentViewId != R.layout.activity_main ) fn_chgContentView(R.layout.activity_main);

        	String vMode1 = values[0];
        	String vParam = values[1];

    		if ( "0".equals(vMode) && "3".equals(vMode1) ) { // ��������Ͽ��� ���ȭ���� ȣ��
    			fn_viewerSetting(vParam); // �� �����Ѵ�.
    		} else {
    			fn_saveBack(vMode1, vParam); // �ڷΰ��� ��ư ���� ���� ��
    			fn_listAdapter(vMode1);
    		}
    		if ( "1".equals(vMode1) ) ((EditText)findViewById(R.id.edt_search)).setText("");
    		if ( "3".equals(vMode1) ) { // ȸ�� ��ȸ�Ҷ� ������ MAX_NO�� Ȯ���غ��� ks20160802
    			if ( !list.isEmpty() && list.size() > 0 ) { //TODO �̰� �³�...ks20160802
    				dbAdapter.updList("QUERY_TB_LC001", list); // ����DB�� ������Ʈ�Ұ������� ����
    			}
    		}

    		vMode = vMode1;
    		if ( !gv_saveHist ) whenBackTouch(); // �ڷΰ��� ��ư���� ������ ���̶��
    		CDialog.hideProgress();
        }

        protected void onProgressUpdate(String ... values) {
        	if ( "".equals(values[0]) ) {
        		CDialog.showProgress(gv_this);
        	} else {
        		CDialog.showProgress(gv_this, values[0]);
        	}
    	}
        protected void onCancelled() { }
    }

    /**
     * �ֽ�ȭ���� ������ư �������� ��Ƽ������ ks20141201
     */
    class NewIsThere extends AsyncTask<String, String , String[]> {
        protected void onPreExecute() {
    		CDialog.showProgress(gv_this, getResources().getString(R.string.str_confirmUpdating));
        }
 
        protected String[] doInBackground(String ... values) {
        	String vIdSeq = values[0];
        	
        	List<HashMap<String, String>> vUpdYN = getXmldata("UPD", vIdSeq); // ������Ʈ Ȯ�� �ѹ� �����ֽð�
        	String vYN = NVL(vUpdYN.get(0).get("UPD"), "N");

			if ( "Y".equals(vYN) ) { // update �Ǿ��ٸ�
				aUtil.delXmlTimer();
	        	getXmldata("3", vIdSeq); // ������Ʈ ����Ʈ �ٽ� �ҷ����ֽð�. 
				getXmldata("V1", gv_setView.get("SITE")); // ks20140905 ����Ʈ�ֽ�ȭ
				list = dbAdapter.inqSql("3", vIdSeq); // ���ε�
			}
        	return new String[] { vIdSeq, vYN };
        }
 
        protected void onPostExecute(String[] values) {
        	String vIdSeq = values[0];
        	String vYN = values[1];

        	if ( "Y".equals(vYN) ) {
    			ArrayAdapter<String> adapter = new ArrayAdapter<String>(gv_this, android.R.layout.simple_spinner_item, fn_convArrayList("NAME"));
    			cbo_no = (Spinner)findViewById(R.id.cbo_no);
    			cbo_no.setAdapter(adapter);
    			fn_goSite(vIdSeq, fn_findListRow("LINK_CODE", gv_setView.get("LST_VIEW_NO")) - 1);
                alert(R.string.str_confirmUpdated);

                // ks20141203 ������ ���ο� ���� �߰������� ��ü ������Ʈ Ȯ���� �ߵ��ȴ�.
    			Thread thread = new Thread(null, doBackgroundThreadProcessing, "Background");
    			thread.start();

                // ks20150119 ����Ʈ��ó�� ������. ����Ʈ�� �ű� �������ֳ�
    			Thread thread1 = new Thread(null, doBackgroundThreadProcessingSearcher, "Background");
    			thread1.start();
        	} else {
                alert(R.string.str_atLast);
        	}
    		CDialog.hideProgress();
        }

        protected void onProgressUpdate(String ... values) { }
        protected void onCancelled() { }
    }

    /**
     * �ý��� �ʱ�ȭ ks20141201
     */
    class initLoadings extends AsyncTask<String, String , String> {
        protected void onPreExecute() {
    		if ( android.os.Build.VERSION.SDK_INT > 9 ) {
    			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    			StrictMode.setThreadPolicy(policy);
    		}
    		fn_setSetting();
    		if ( gv_svrList.size() > 0 ) gv_svrUrl = gv_svrList.get(gv_svrUrlArrayIndex); // ���� �ε�ÿ��� 0��° row�� �⺻.
        }
 
        protected String doInBackground(String ... values) {
        	gv_sectionG = getResources().getStringArray(R.array.gv_sectionG);
        	gv_sectionV = getResources().getStringArray(R.array.gv_sectionV);
            gv_buttons.put(""+ R.id.btn_myList, new int[]{R.drawable.ic_mn_mylist, R.drawable.ic_mn_mylist_b});
            gv_buttons.put(""+ R.id.btn_search, new int[]{R.drawable.ic_mn_add, R.drawable.ic_mn_add_b});
            gv_buttons.put(""+ R.id.btn_menuSetting, new int[]{R.drawable.ic_mn_setting, R.drawable.ic_mn_setting_b});
            gv_buttons.put(""+ R.id.btn_bottomPrevMax, new int[]{R.drawable.ic_pre_max, R.drawable.ic_pre_max_b});
            gv_buttons.put(""+ R.id.btn_bottomPrev, new int[]{R.drawable.ic_pre, R.drawable.ic_pre_b});
            gv_buttons.put(""+ R.id.btn_bottomNextMax, new int[]{R.drawable.ic_next_max, R.drawable.ic_next_max_b});
            gv_buttons.put(""+ R.id.btn_nav_smp, new int[]{R.drawable.ic_next, R.drawable.ic_next_b});
            gv_buttons.put(""+ R.id.btn_bottomReload, new int[]{R.drawable.ic_reload, R.drawable.ic_reload_b});
            gv_buttons.put(""+ R.id.btn_nav_menu, new int[]{R.drawable.ic_menu, R.drawable.ic_menu_b});
            gv_buttons.put(""+ R.id.btn_bottomMyList, new int[]{R.drawable.ic_mn_mylist, R.drawable.ic_mn_mylist_b});
            gv_buttons.put(""+ R.id.btn_bottomSearch, new int[]{R.drawable.ic_mn_add, R.drawable.ic_mn_add_b});
            gv_buttons.put(""+ R.id.btn_favo        , new int[]{R.drawable.ic_mn_favo, R.drawable.ic_mn_favo_b}); // �߰� ks20141116
            gv_buttons.put(""+ R.id.btn_bottomAdd   , new int[]{R.drawable.ic_add, R.drawable.ic_add_b});

        	//Log.d("����", getSetting("APP_VER") + " / " + aUtil.getVersionName(gv_this));

    		// ���� �������� �Ͼ�°� "1".equals("1") || 
            if ( !getSetting("APP_VER").equals(aUtil.getVersionName(gv_this)) ) { // ���� �������� �Ǿ��ٸ�?
    			//if ( "-1".equals(data.get("VER004")) ) { // ���� ������ ����
    	        //���� ���Ϸ� �Է� �� ������ ȣ���Ѵ�.
                publishProgress("1 / 3");
                dbAdapter.fn_dbClear(); // ��� �ʱ�ȭ. ks20151227 �߸��� ���� ������ �ٲ㺸����.
    			getXmldata("2" , "-1"); // LC002����Ʈ����
    	        getXmldata("V2", "-1"); // LC001������� insert. ������ �߰��Ǵ� ��찡 ���� �ʾƼ� �� ����.
    	        getXmldata("4" , "-1"); // LC004�����׸�
                publishProgress("2 / 3");
    			getXmldata("3" , "-1"); // ȸ�����. �α��ִ�, �� ���� ������ �־�״�.
                publishProgress("3 / 3");
                setSetting("APP_VER", aUtil.getVersionName(gv_this)); // APP_VER ���ù���,  WEB_VER �ֽŹ���(�������� ������Ʈ�Ǿ��ִ�.)
            } else {
            	try {
					Thread.sleep(500);
	                publishProgress("1");
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}

            publishProgress("2");
        	List<HashMap<String, String>> vList = getXmldata("4", "");
        	if ( !vList.isEmpty() ) fn_setSetting();

            getXmldata("V0", ""); // ks20141030
            getXmldata("2" , "");

    		fn_setSiteInfo();  // ����Ʈ ������ �ε�

    		list = dbAdapter.inqSql("0", "");
    		return list.size() + "";
        }
 
        protected void onProgressUpdate(String ... values) {
        	if ( "1".equals(values[0]) ) {
    			String vName = getSetting("MY_NM"); // N�������� �̸�_�̸��Ϸù�ȣ...�� ������.
    			if ( vName.indexOf("_") > -1 ) vName = vName.split("_")[0];
            	alert("Hi! " + vName + "!"); //alert(R.string.app_name);
        	} else if ( "2".equals(values[0]) ) {
	    		CDialog.showProgress(gv_this);
        	} else {
	    		CDialog.showProgress(gv_this, getResources().getText(R.string.str_firstLoading) + "\n" + values[0]);
        	}
    	}

        protected void onPostExecute(String values) {
			Thread thread = new Thread(null, doBackgroundThreadUpdChkMyList, "Background");
			thread.start(); // ks20141225 �������������� ������Ʈ Ȯ��
			Thread thread1 = new Thread(null, doBackgroundThreadUpdChkNewToons, "Background");
			thread1.start(); // ks20150121 �ű������˻�

        	if ( "0".equals(values) ) {
        		fn_menu("1", "");
        	} else {
        		fn_menu("0", "");
        	}

        	alert(""); // �佺Ʈ �������.
    		CDialog.hideProgress();
        }
        protected void onCancelled() { }
    }

	// ks20150111 ���ο� ���� �μ�Ʈ(�űԻ���Ʈ ��Ͻ� �ε� ���ϴ� ���� ����). �ϰ�,use_yn�� ������Ʈ�ȴ�.
	private Runnable doBackgroundThreadUpdChkNewToons = new Runnable() {
		@Override
		public void run() { getXmldata("V2", ""); }
	};

	// ����������������Ʈ ������Ʈ Ȯ��
	private Runnable doBackgroundThreadUpdChkMyList = new Runnable() {
		@Override
		public void run() {
        	List<HashMap<String, String>> vList = getXmldata("UPD_MYLIST", "");
        	if ( vList.isEmpty() ) return;
    		if ( !"0".equals(vList.get(0).get("UPD")) ) {
                // ���ο� ���� �߰ߵǸ� ��ü ������Ʈ
    			Thread thread = new Thread(null, doBackgroundThreadProcessing, "Background");
    			thread.start();
                // ks20150119 ����Ʈ��ó�� ������. ����Ʈ�� �ű� �������ֳ�
    			Thread thread1 = new Thread(null, doBackgroundThreadProcessingSearcher, "Background");
    			thread1.start();
    		}
		}
	};

	// ��׶��� ó�� �޼��带 �����ϴ� Runnable
	private Runnable doBackgroundThreadProcessing = new Runnable() {
		@Override
		public void run() {
        	List<HashMap<String, String>> vList = getXmldata("UPD_ALL", "");
        	if ( vList.isEmpty() ) return;
    		if ( !"0".equals(vList.get(0).get("UPD")) ) {
				aUtil.delXmlTimer(); // ���� aUtil.delXmlTimer() ȣ���� �� �ð��� �����Ŀ��� �ٽ� �����.
    	        getXmldata("V0", ""); // ks20141203 ������ �������� ������Ʈ�Ǿ����� �ϴ� ������������ ������Ʈ
    		}
		}
	};

	// ks20150119 ����Ʈ��ó �ߵ�
	private Runnable doBackgroundThreadProcessingSearcher = new Runnable() {
		@Override
		public void run() { getXmldata("SITE_SEARCHER", ""); }
	};
	/*****************************
	 * ��Ƽ������ ��
	 *****************************/

	/*****************************
	 * OnClick ����
	 *****************************/
	// ListViewer �� Ŭ�� �̺�Ʈ���� �´�.
	public void fn_listOnClick(int pPosition) {
		gv_nav = ((TextView) findViewById(R.id.txt_nav)).getText() + " > " + getList(pPosition, "NAME");
		if ( "0".equals(vMode) ) { // 0���������
			gv_setView.clear();
			fn_setViewSetting("CID"   , getList(pPosition, "CID"   ));
			fn_setViewSetting("ID_SEQ", getList(pPosition, "ID_SEQ"));
			fn_setViewSetting("SITE"  , getList(pPosition, "SITE"  ));
			fn_setViewSetting("MAX_NO", getList(pPosition, "MAX_NO"));
			fn_setViewSetting("LST_VIEW_NO", getList(pPosition, "LST_VIEW_NO"));
			fn_menu("3", gv_setView.get("ID_SEQ"));

		} else if ( "1".equals(vMode) ) { // 1����Ʈ��Ͽ�����Ŭ��
			gv_setView.clear();
			fn_setViewSetting("SITE", getList(pPosition, "SITE"));
			fn_menu("2", getList(pPosition, "SITE"));

		} else if ( "2".equals(vMode) || "4".equals(vMode) ) { // 2������Ͽ����� Ŭ�� 4�˻� 
			fn_setViewSetting("CID"   , getList(pPosition, "CID"   ));
			fn_setViewSetting("ID_SEQ", getList(pPosition, "ID_SEQ"));
			fn_setViewSetting("MAX_NO", getList(pPosition, "MAX_NO"));
			if ( "4".equals(vMode) ) fn_setViewSetting("SITE", getList(pPosition, "SITE"));
			fn_menu("3", gv_setView.get("ID_SEQ"));

		} else if ( "3".equals(vMode) ) { // 3ȸ������Ʈ����Ŭ��
			fn_setViewSetting("LST_VIEW_NO", getList(pPosition, "LINK_CODE"));
			fn_viewerSetting(gv_setView.get("ID_SEQ"));

		} else if ( "5".equals(vMode) ) { // ����
			String vSelMode = getList(pPosition, "SEL_MODE");
			// vSelMode ���� LINK:���ͳ����������̵� COMBO:�޺� SWITCH:����ġ ���� �ִ�.
			if ( "LINK".equals(vSelMode) ) {
				if ( "REVIEW".equals(getList(pPosition, "SET_ID")) ) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.anglab.toontaku"));
					startActivity(i);
				} else {
					fn_viewerSetting(getList(pPosition, "SET_VALUE"));
				}
			} else if ( "intent".equals(vSelMode) ) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getList(pPosition, "SET_VALUE")));
				startActivity(i);
			} else if ( "donation".equals(vSelMode) ) {
				fn_donation();
			} else if ( "function".equals(vSelMode) ) {
				if ( "DB_CLEAR".equals(getList(pPosition, "SET_ID")) ) { // DBŬ����
					fn_dbClear();
				} else if ( "BACKUP".equals(getList(pPosition, "SET_ID")) ) { // ���/����
					fn_backupRestore();
				}
			} else if ( "CheckBox".equals(vSelMode) ) {
				if ( !"".equals(getList(pPosition, "SET_CONT")) ) {
					aUtil.alertD(this, getList(pPosition, "SET_CONT"));
				}
			} else {
			}
		} else if ( "6".equals(vMode) ) { // 6��õ ���� ks20141116
			fn_setViewSetting("CID"   , getList(pPosition, "CID"   ));
			fn_setViewSetting("ID_SEQ", getList(pPosition, "ID_SEQ"));
			fn_setViewSetting("MAX_NO", getList(pPosition, "MAX_NO"));
			fn_setViewSetting("SITE"  , getList(pPosition, "SITE"));
			fn_menu("3", gv_setView.get("ID_SEQ"));

		} else { // ����
		}
	}

	@Override
    public boolean onTouch(View v, MotionEvent event) {
		if ( gv_buttons.containsKey( "" + v.getId() ) ) { // ��ư ���ϴ� �͵��̸�
			int[] vKeys = gv_buttons.get( "" + v.getId() );
	        if ( event.getAction() == MotionEvent.ACTION_DOWN ) { // ��ư�� ������ ���� ��
	        	findViewById(v.getId()).setBackgroundResource(vKeys[0]);
	        } else if ( event.getAction() == MotionEvent.ACTION_UP ) { //��ư���� ���� ������ �� 
	        	findViewById(v.getId()).setBackgroundResource(vKeys[1]);
	        }
		}
		return false;
	}

    @Override
	public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_bottomMyList:
        	fn_menu("0", ""); // �� ����������� �̵�
            break;
        case R.id.btn_bottomSearch:
        	fn_menu("1", "");
            break;
        case R.id.btn_bottomAdd:
        	fn_bottomAddSub(gv_setView.get("ID_SEQ"));
            break;
        case R.id.btn_bottomPrevMax:
			fn_goSite(gv_setView.get("ID_SEQ"), cbo_no.getCount() - 1);
            break;
        case R.id.btn_bottomPrev:
			fn_goSite(gv_setView.get("ID_SEQ"), cbo_no.getSelectedItemPosition() + 1);
            break;
        case R.id.btn_bottomNextMax:
			fn_goSite(gv_setView.get("ID_SEQ"), 0);
            break;
        case R.id.btn_myList:
        	fn_menu("0", ""); // �� ����������� �̵�
            break;
        case R.id.btn_search:
        	fn_menu("1", "");
            break;
        case R.id.btn_favo:
        	fn_menu("6", ""); // ��õ����
            break;
        case R.id.btn_menuSetting:
        	fn_menu("5", "");
            break;
        case R.id.btn_nav_menu:
        	fn_menuSlide();
            break;
        case R.id.btn_nav_smp:
			fn_goSite(gv_setView.get("ID_SEQ"), cbo_no.getSelectedItemPosition() - 1);
            break;
        case R.id.btn_bottomReload:
			mWebView.reload();
            break;
	    }
    }
	/*****************************
	 * OnClick ��
	 *****************************/

	/*****************************
	 * UTIL ����
	 *****************************/
    String gv_searchWord = "";
	// ȭ���� �ٲ۴�.
	public void fn_chgContentView(int pLayoutResId) {
		if ( gv_currentViewId == pLayoutResId ) return; // ����ȭ��� �ٲ�ȭ���� ���ٸ� ����.

		// �������� �� â�� �����۾� ���� //
		String vNav = "";
		if ( gv_currentViewId == R.layout.activity_main ) {
			vNav = ((TextView)findViewById(R.id.txt_nav)).getText().toString();
		} else if ( gv_currentViewId == R.layout.view_mode ) {
		}
		// �������� �� â�� �����۾� �� //

		gv_currentViewId = pLayoutResId;
		setContentView(gv_currentViewId);

		// ���� ������ ȭ���� �����۾� ���� //
		if ( gv_currentViewId == R.layout.activity_main ) {

			findViewById(R.id.img_arrow).setVisibility(View.GONE);
			findViewById(R.id.txt_noMyList).setVisibility(View.GONE);
			
			findViewById(R.id.btn_myList).setOnTouchListener(this);
			findViewById(R.id.btn_search).setOnTouchListener(this);
			findViewById(R.id.btn_favo).setOnTouchListener(this);
			findViewById(R.id.btn_menuSetting).setOnTouchListener(this);

			findViewById(R.id.btn_myList).setOnClickListener(this);
			findViewById(R.id.btn_search).setOnClickListener(this);
			findViewById(R.id.btn_favo).setOnClickListener(this);
			findViewById(R.id.btn_menuSetting).setOnClickListener(this);

			EditText edt_search = (EditText)findViewById(R.id.edt_search);
			edt_search.setOnKeyListener(new OnKeyListener(){
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ( keyCode == KeyEvent.KEYCODE_ENTER ) { // ���ʹ����� �˻� ȣ��
						fn_goSearch();
						return true;
					} else {
						//���� �Է½ø��� �˻�. �� ���� ��� ���� ks20141116
						//if ( gv_searchWord.equals(((EditText) findViewById(R.id.edt_search)).getText().toString()) ) {
						//	return false;
						//} else {
						//	fn_goSearch();
						//	return true;
						//}
						return false;
					}
				}
				} );

			ListView listView = (ListView) findViewById(R.id.list);
			listView.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					// ����Ʈ ������ ���� ǥ�ø� ����
					if ( !"2".equals(vMode) && !"4".equals(vMode) ) return;
					TextView txt_section_main = (TextView) findViewById(R.id.txt_section_main);
					String vSection = fn_sectionWords(firstVisibleItem);
					if ( !txt_section_main.getText().equals(vSection) ) {
						txt_section_main.setText(fn_sectionWords(firstVisibleItem));
					}
				}
			});

		} else if ( gv_currentViewId == R.layout.view_mode ) {
			((TextView)findViewById(R.id.txt_view_nav)).setText(vNav);

			if ( "Y".equals(getSetting("NAV_HIDDEN")) &&  isMenuExpanded ) fn_menuSlide(); // ������̼ǹٰ� ���������� ����־�.
			if ( "N".equals(getSetting("NAV_HIDDEN")) && !isMenuExpanded ) fn_menuSlide(); // ������̼ǹٰ� ���������� ����־�.

			mWebView = (WebView) findViewById(R.id.web_main);
			mWebView.getSettings().setJavaScriptEnabled(true); // �ڹٽ�ũ��Ʈ  on
			mWebView.setWebViewClient(new CWebViewClient(this));

	        findViewById(R.id.btn_bottomAdd    ).setOnTouchListener(this);
	        findViewById(R.id.btn_bottomMyList ).setOnTouchListener(this);
	        findViewById(R.id.btn_bottomSearch ).setOnTouchListener(this);
	        findViewById(R.id.btn_bottomPrevMax).setOnTouchListener(this);
	        findViewById(R.id.btn_bottomPrev   ).setOnTouchListener(this);
	        findViewById(R.id.btn_bottomNextMax).setOnTouchListener(this);
	        findViewById(R.id.btn_bottomReload ).setOnTouchListener(this);
	        findViewById(R.id.btn_nav_menu     ).setOnTouchListener(this);
	        findViewById(R.id.btn_nav_smp      ).setOnTouchListener(this);

	        findViewById(R.id.btn_bottomAdd    ).setOnClickListener(this);
	        findViewById(R.id.btn_bottomMyList ).setOnClickListener(this);
	        findViewById(R.id.btn_bottomSearch ).setOnClickListener(this);
	        findViewById(R.id.btn_bottomPrevMax).setOnClickListener(this);
	        findViewById(R.id.btn_bottomPrev   ).setOnClickListener(this);
	        findViewById(R.id.btn_bottomNextMax).setOnClickListener(this);
	        findViewById(R.id.btn_bottomReload ).setOnClickListener(this);
	        findViewById(R.id.btn_nav_menu     ).setOnClickListener(this);
	        findViewById(R.id.btn_nav_smp      ).setOnClickListener(this);

	        if ( fn_isInMyList(gv_setView.get("ID_SEQ")) ) {
	        	gv_buttons.put("" + R.id.btn_bottomAdd, new int[]{R.drawable.ic_sub, R.drawable.ic_sub_b});
	        	findViewById(R.id.btn_bottomAdd).setBackgroundResource(R.drawable.ic_sub_b);
	        } else {
	        	gv_buttons.put("" + R.id.btn_bottomAdd, new int[]{R.drawable.ic_add, R.drawable.ic_add_b});
	        	findViewById(R.id.btn_bottomAdd).setBackgroundResource(R.drawable.ic_add_b);
	        }

	        if ( "5".equals(vMode) ) { // ������ ��ũ�� ������̼� ��ư ����
	        	findViewById(R.id.ll_menuBottom_smp).setVisibility(View.GONE);
	        } else {
	        	findViewById(R.id.ll_menuBottom_smp).setVisibility(View.VISIBLE);
	        }
		} else if ( gv_currentViewId == R.layout.loading ) {
		}
		// ���� ������ ȭ���� �����۾� �� //
	}

	// pTagName �� ���� list �κ��� ArrayList�� �����Ѵ�.
	public ArrayList<String> fn_convArrayList(String pTagName) {
		ArrayList<String> vArrayList = new ArrayList<String>();
		for ( int i = 0; i < list.size(); i++ ) {
			vArrayList.add(getList(i, pTagName));
		}
		return vArrayList;
	}

	// list �� pTagName�� ���� pValue�� ���� ������ �ִ� row�� �����Ѵ�.
	public int fn_findListRow(String pTagName, String pValue) {
		if ( pTagName == null || pValue == null ) return -1;
		for ( int i = 0; i < list.size(); i++ ) {
			if ( pValue.equals(getList(i, pTagName)) ) {
				return i;
			}
		}
		return -1;
	}

	// thumbNail url�� ������´�.
	public String fn_getUrl(int pPosition) {
		String vSite = NVL(getList(pPosition, "SITE"), gv_setView.get("SITE")); // list�� SITE�� ������ ���� ������ �۷ι��¿��� ������ ����.
		String vCode = NVL(getList(pPosition, "MAX_NO"), getList(pPosition, "LINK_CODE")); // "3".equals(vMode)? getList(pPosition, "LINK_CODE") : getList(pPosition, "MAX_NO"); // ȸ������. ȸ������Ʈ�� LINK_CODE��, ����������̸� MAX_NO�� �����´�.
		String vCid  = NVL(getList(pPosition, "CID"), gv_setView.get("CID"));
		String vThumbUrl = (( getList(pPosition, "THUMB_NAIL").indexOf("http") == 0 )? "" : gv_thumbComn.get(vSite) ) + getList(pPosition, "THUMB_NAIL");
		return (vThumbUrl.replace("$cid", vCid).replace("$no", vCode));
	}

	// list�� pPosition ���� pTagName ���� ��ȯ
	public String getList(int pPosition, String pTagName) {
		return NVL((String) list.get(pPosition).get(pTagName));
	}
	public String NVL(String pStr1) { return NVL(pStr1, ""); }
	public String NVL(String pStr1, String pStr2) {
		if ( pStr1 == null || pStr1.trim().length() == 0 ) {
			if ( pStr2 == null || pStr2.trim().length() == 0 ) pStr2 = "";
			return pStr2;
		}
		return pStr1;
	}

	public String fn_getMode() { return vMode; }
	public String getSetting(String pParam) { return NVL(gv_setting.get(pParam), ""); }

    private void fn_bottomAddSub(String pIdSeq) {
        if ( fn_isInMyList(pIdSeq) ) {
        	fn_delMyList(pIdSeq); // ��������Ͽ��� ����
        } else {
        	fn_insMyList(pIdSeq);
        }
	}

	// ���� �˻�
	public void fn_goSearch() {
		gv_searchWord = ((EditText) findViewById(R.id.edt_search)).getText().toString().trim();
		if ( "".equals(gv_searchWord) ) return;
		gv_nav = getResources().getString(R.string.app_name) + " > " + getResources().getString(R.string.str_search) + " > '" + ((EditText) findViewById(R.id.edt_search)).getText().toString() + "'" + getResources().getString(R.string.str_searchWords);
		fn_menu("4", gv_searchWord);
	}

    // �ڷΰ��� ��ư ����
	boolean gv_saveHist = true; // �ڷΰ��� �̷��� ������ true
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if ( keyCode == KeyEvent.KEYCODE_BACK ) {
    		if ( gv_backList.isEmpty() || gv_backList.size() < 2 ) { // �� ���� �޴��϶� �ڷι�ư�� �ι� Ŭ���ϸ� ���� ����ȴ�.
    			if ( aUtil.chkTimer("BackButton", 2) ) { // 2�ʸ��� Ŭ���ϸ� �ý�������
    		        finish();
    		        System.exit(0);
    		        android.os.Process.killProcess(android.os.Process.myPid());
    		    } else {
    		        Toast.makeText(this, R.string.str_oneMoreFinish, Toast.LENGTH_SHORT).show();
    		    }
    		} else {
    			HashMap<String, String> data = (HashMap<String, String>) gv_backList.get(gv_backList.size() - 2);
    			gv_saveHist = false;
    			fn_menu(NVL(data.get("vMode")), NVL(data.get("vParam")));
    		}
    	}
		return false;
	}

    public void whenBackTouch() {
		gv_saveHist = true;

		//HashMap<String, String> data1 = (HashMap<String, String>) gv_backList.get(gv_backList.size() - 2); // vMode, vParam
		HashMap<String, String> data = (HashMap<String, String>) gv_backList.get(gv_backList.size() - 1); // vPos, vNav
		gv_backList.remove(gv_backList.size() - 1);

		int vPos = Integer.valueOf(data.get("vPos"));
		if ( vPos > -1 && list.size() > vPos ) ((ListView)findViewById(R.id.list)).setSelection(vPos);
		((TextView)findViewById(R.id.txt_nav)).setText(data.get("vNav"));
    }

    // �ڷΰ��� ��ư�� �����Ѵ�.
    public void fn_saveBack(String pMode, String pParam) {
    	if ( !gv_saveHist ) return;
		if ( "0".equals(pMode) || "1".equals(pMode) ) gv_backList.clear(); // ����������Ʈ �϶��� ���ư �ʱ�ȭ(���̻� �ڷΰ�������)

		if ( gv_backList.size() > 0 ) {
			HashMap<String, String> dataCompare = (HashMap<String, String>) gv_backList.get(gv_backList.size() - 1);
			if ( dataCompare.get("vMode").equals(pMode) && dataCompare.get("vParam").equals(pParam) ) return; // ���� �޴� �ι� Ŭ���� �н�
		}

		HashMap<String, String> data = new HashMap<String, String>();
		data.put("vMode" , pMode );
		data.put("vParam", pParam);
		data.put("vNav"  , ((TextView)findViewById(R.id.txt_nav)).getText().toString());
		data.put("vPos"  , ((ListView)findViewById(R.id.list)).getFirstVisiblePosition() + "");
		gv_backList.add(new HashMap<String, String>(data));
    }
    
	Toast mToast = null; // �޽��������� ���� �������� �佺Ʈ ����.
    public void alert(String pMsg) {
    	try {
	    	if ( "".equals(pMsg) ) {
	    		if ( mToast != null ) mToast.cancel();
	    	} else {
		    	if ( mToast != null ) {
					mToast.setText(pMsg);
				} else {
					mToast = Toast.makeText(gv_this, pMsg, Toast.LENGTH_SHORT);
				}
				mToast.show();
	    	}
		} catch(Exception e) {}
    }
    public void alert(int pMsg) {
    	try {
			if ( mToast != null ) {
				Log.d("����", getResources().getText(pMsg)+"");
				mToast.setText(pMsg);
				Log.d("����", "��������");
			} else {
				mToast = Toast.makeText(gv_this, pMsg, Toast.LENGTH_SHORT);
			}
			mToast.show();
    	} catch(Exception e) {}
    }
	/*****************************
	 * UTIL ��
	 *****************************/

	/*****************************
	 * ���/���� ����
	 *****************************/
	public void fn_backupRestore() { // ��� ����
		//TODO
		String vContext = String.format(getResources().getString(R.string.str_backup), getSetting("MY_NM"));
		final EditText ev = new EditText(this);
		ev.setSingleLine();
	    AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
	    alt_bld.setMessage(vContext);
	    alt_bld.setCancelable(false);
	    alt_bld.setPositiveButton(R.string.str_backupYes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if ( "".equals(ev.getText().toString()) ){
							alert(R.string.str_backupKey);
							fn_backupRestore();
							return;
						}
						fn_restoreGo(ev.getText().toString());
					}
				});
	    alt_bld.setNegativeButton(R.string.str_backupNo, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
	    alt_bld.setTitle(R.string.str_backupTitle);
	    alt_bld.setView(ev);
	    alt_bld.show();
	}
	
	public void fn_restoreGo(String pRstKey) {
		List<HashMap<String, String>> vList1 = getXmldata("VB", pRstKey); // ������Ʈ ����Ʈ �ٽ� �ҷ����ֽð�. 

 		if ( !vList1.isEmpty() ) { // �α�����
 			StringBuffer vStr = new StringBuffer();
 			for ( int i = 0; i < vList1.size(); i++ ) {
 				vStr.append("SELECT '").append(vList1.get(i).get("ID_SEQ")).append("' AS ID_SEQ, ").append(i).append(" AS SORT UNION ALL ");
 			}
 			final List<HashMap<String, String>> vList = dbAdapter.inqSql("6", vStr.toString()); // ��õ���� ��������, ������ ������� �˾Ƴ��µ� ������ ���. 

 			vStr.setLength(0);
 			if ( vList.size() == 0 ) {
				aUtil.alertD(this, getResources().getString(R.string.str_restoreEmpty));
				return;
 			} else if ( vList.size() == 1 ) {
 				vStr.append("[").append(vList.get(0).get("NAME")).append("]");
 			} else if ( vList.size() >= 2 ) {
 				vStr.append("[").append(vList.get(0).get("NAME")).append("], [").append(vList.get(1).get("NAME")).append("]");
 			}

 			String vMsg = String.format(getResources().getString(R.string.str_restore), vStr, vList.size());

 			AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
 			alt_bld.setMessage(vMsg)
 					.setCancelable(false)
 					.setPositiveButton(R.string.str_yes,
 							new DialogInterface.OnClickListener() {
 								public void onClick(DialogInterface dialog, int id) {
 									for ( int i = 0; i < vList.size(); i++ ) {
 	 				            		dbAdapter.insLC000(vList.get(i).get("ID_SEQ"), fn_inqSort("MAX"), "-1");
 									}
 									alert(R.string.str_comp);
 								}
 							})
 					.setNegativeButton(R.string.str_no,
 							new DialogInterface.OnClickListener() {
 								public void onClick(DialogInterface dialog, int id) {
 									dialog.cancel();
 								}
 							});
 			AlertDialog alert = alt_bld.create();
 			alert.setTitle(R.string.str_backupTitle);
 			alert.show();
 		} else {
			aUtil.alertD(this, getResources().getString(R.string.str_restoreEmpty));
 		}
	}
	/*****************************
	 * ���/���� ���� ��
	 *****************************/

	/*****************************
	 * ������ ����
	 *****************************/
    private static boolean isMenuExpanded = true;
    private void fn_menuSlide() {
    	LinearLayout pLayout = (LinearLayout) findViewById(R.id.ll_menuBottom);
        FrameLayout.LayoutParams leftMenuLayoutPrams = (FrameLayout.LayoutParams) pLayout.getLayoutParams();
		enableDisableViewGroup((ViewGroup) findViewById(R.id.ll_menuBottomRelative), !isMenuExpanded);

        if ( isMenuExpanded ) {
            isMenuExpanded = false;
            new OpenAnimation( pLayout, 0, 0.0f, 0, 0.0f
		                     , Animation.INFINITE, 0
		                     , Animation.INFINITE, leftMenuLayoutPrams.height);
        } else {
            isMenuExpanded = true;
            new OpenAnimation( pLayout, 0, 0.0f, 0, 0.0f
                             , Animation.INFINITE, leftMenuLayoutPrams.height
                             , Animation.INFINITE, 0);
        }
    }

	/**
	 * ���� ������ �����Ѵ�. ���� ��� ����� enable ������ �����ȴ�.
	 */
	public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
		int childCount = viewGroup.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View view = viewGroup.getChildAt(i);
			view.setEnabled(enabled);
			if (view instanceof ViewGroup) {
				enableDisableViewGroup((ViewGroup) view, enabled);
			}
		}
	}
	/*****************************
	 * ������ ��
	 *****************************/

	/*****************************
	 * ����Ʈ�� ���� ���� ����
	 *****************************/
	ArrayList<Integer> gv_sections = new ArrayList<Integer>();
	public int fn_findSection(String pName) {
        for ( int i = 1; i < gv_sectionG.length; i++) {
            if ( pName.compareTo(gv_sectionG[i]) < 0 ) return i - 1;
        }
        return 0;
	}

	public void fn_setSection() {
		if ( list.size() == 0 ) { 
			findViewById(R.id.txt_section_main).setVisibility(View.GONE);
			return;
		}
		gv_sections.clear();
		findViewById(R.id.txt_section_main).setVisibility(View.VISIBLE);
		for ( int i = 0; i < list.size(); i++ ) {
			gv_sections.add(fn_findSection(getList(i, "NAME")));
		}
	}

	public String fn_sectionWords(int pPosition) {
		if ( pPosition < 0 ) return "";
		if ( gv_sections.size() <= pPosition ) return "";
		return gv_sectionV[(Integer) gv_sections.get(pPosition)];
	}

	public String fn_sectionWordsEmpty(int pPosition) {
		String vRtn = fn_sectionWords(pPosition);
		if ( pPosition == 0 ) return vRtn;
		return vRtn.equals(fn_sectionWords(pPosition - 1))? "" : vRtn;
	}
	/*****************************
	 * ����Ʈ�� ���� ���� ��
	 *****************************/

	/*****************************
	 * SQLite DB ��Ʈ�� ���� ����
	 *****************************/
	/** �� �޴��� ������ȸ */
	public void fn_menu(String pMode, String pParam) {
	    (new BackgroundTask()).execute(new String[] {pMode, pParam});
	}

	/** ���α׷� �⺻���� �ε� */
	public void fn_setSetting() {
        List<HashMap<String, String>> vList = dbAdapter.inqSql("TB_LC004", "");
        for ( HashMap<String, String> data : vList ) {
    		if ( "svrList".equals(data.get("SEL_MODE")) ) { // ������� ����Ʈ
	   			 /* http://toontaku.dothome.co.kr/toontaku/
			        http://kyusiks.dothome.co.kr/toontaku/
			        http://anglab.dothome.co.kr/toontaku/
			        x http://anglab.url.ph/toontaku/ 
			                     ���� 3���� �̷��� ����Ʈ�� ��� */
    			gv_svrList.add(data.get("SET_VALUE")); // SET_NM �� ��� ����Ʈ�� ǥ�ð� �ȵȴ�.
    		} else {
        		gv_setting.put(data.get("SET_ID"), data.get("SET_VALUE"));
    		}
        }
	}

	/** ����Ʈ�� ���� �ε� */
	public void fn_setSiteInfo() {
        List<HashMap<String, String>> vList = dbAdapter.inqSql("TB_LC002", "");
        if ( vList.size() <= 0 ) return;
        for ( HashMap<String, String> data : vList ) {
    		gv_imgViewer.put(data.get("SITE"), data.get("IMG_VIEWER"));
	    	gv_thumbComn.put(data.get("SITE"), data.get("THUMB_COMN"));
        }
	}

	// ���� ��Ͽ� �߰�
	public void fn_insMyList(final String pIdSeq) {
        String vName = "";
		int index = fn_findListRow("ID_SEQ", pIdSeq);
        if ( index >= 0 && index < list.size() ) {
        	vName = getList(index, "NAME");
        	if ( !"".equals(vName) ) vName = "[" + vName +"]\n\n";
        }

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.str_rusure);
        dialogBuilder.setMessage(vName + getResources().getString(R.string.str_gudokAdd)); // �����߰��Ͻðڽ��ϱ�
        dialogBuilder.setNegativeButton(R.string.str_no, null);
        dialogBuilder.setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
        		if ( gv_currentViewId == R.layout.activity_main ) { // ����Ʈ���� Ŭ��
            		dbAdapter.insLC000(pIdSeq, fn_inqSort("MAX"), "-1");

            		// ������ ���ġ�� ���� ȭ�� ���ε�
        			HashMap<String, String> data = (HashMap<String, String>) gv_backList.get(gv_backList.size() - 1);
        			data.put("vPos", ((ListView)findViewById(R.id.list)).getFirstVisiblePosition() + "");
        			data.put("vNav", ((TextView)findViewById(R.id.txt_nav)).getText().toString());
        			gv_backList.add(new HashMap<String, String>(data));
        			gv_saveHist = false;
        			fn_menu(NVL(data.get("vMode")), NVL(data.get("vParam")));

        		} else { // ���忡�� Ŭ��
            		dbAdapter.insLC000(pIdSeq, fn_inqSort("MAX"), gv_setView.get("LST_VIEW_NO"));
    	        	gv_buttons.put("" + R.id.btn_bottomAdd, new int[]{R.drawable.ic_sub, R.drawable.ic_sub_b});
    	        	findViewById(R.id.btn_bottomAdd).setBackgroundResource(R.drawable.ic_sub_b);
        		}
            }
        });
        dialogBuilder.show();
	}

	// ���� ��Ͽ� �ش�� ����
	public void fn_delMyList(final String pIdSeq) {
        String vName = "";
		int index = fn_findListRow("ID_SEQ", pIdSeq);
        if ( index >= 0 && index < list.size() ) {
        	vName = getList(index, "NAME");
        	if ( !"".equals(vName) ) vName = "[" + vName +"]\n\n";
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.str_rusure);
        dialogBuilder.setMessage(vName + getResources().getString(R.string.str_gudokCancle));
        dialogBuilder.setNegativeButton(R.string.str_no, null);
        dialogBuilder.setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
        		dbAdapter.delLC000(pIdSeq);

        		if ( gv_currentViewId == R.layout.activity_main ) { // ����Ʈ���� Ŭ��
            		// ������ ���ġ�� ���� ȭ�� ���ε�
            		int vTempPos = ((ListView)findViewById(R.id.list)).getFirstVisiblePosition();
            		fn_menu("0", ""); // �� ����������� �̵�
    				((ListView)findViewById(R.id.list)).setSelection(vTempPos);
        		} else { // ���忡�� Ŭ��
    	        	gv_buttons.put("" + R.id.btn_bottomAdd, new int[]{R.drawable.ic_add, R.drawable.ic_add_b});
    	        	findViewById(R.id.btn_bottomAdd).setBackgroundResource(R.drawable.ic_add_b);
        		}
            }
        });
        dialogBuilder.show();
	}

	// �ֱ� �� ȭ.
	public void fn_updLstViewNo() {
		dbAdapter.updLstViewNo(gv_setView.get("ID_SEQ"), gv_setView.get("LST_VIEW_NO"));
	}

	/**TODO ����? ���α׷� �⺻���� �ε� */
	public String fn_inqSort(String pMinMax) {
		HashMap<String, String> data = dbAdapter.inqSql("TB_LC000_SORT_MAX", "").get(0);
        String vReturn = "MAX".equals(pMinMax)? data.get("SORT_MAX") : data.get("SORT_MIN");
        return vReturn; // ��Ʈ �ƽ� : ��Ʈ ��
	}

	/** MyList�� IdSeq�� '/' �����ڷ�. �ؽ�Ʈ���·�. ks20141101 */
	public String fn_inqMyListToString() {
        List<HashMap<String, String>> vList = dbAdapter.inqSql("0", "");
        if ( vList.size() <= 0 ) return "";

        StringBuffer rtn = new StringBuffer();
        for ( HashMap<String, String> data : vList ) {
        	rtn.append(data.get("ID_SEQ")).append("/");
        }
        rtn.append("_"); // ���� �а��ִ� LST_VIEW_NO�� �ٿ��ش�.
        for ( HashMap<String, String> data : vList ) {
        	rtn.append(data.get("LST_VIEW_NO")).append("/");
        }
        return rtn.toString();
	}

	public String inqXmlParam(String pMode, String pParam) {
		List<HashMap<String, String>> vList = dbAdapter.inqSql("S_" + pMode, pParam);
		if ( vList.isEmpty() ) return "";
		return vList.get(0).get("COL1");
	}

	public boolean fn_isInMyList(String pIdSeq) { // ID_SEQ�� myList�� �ֳ�? ������ true
		return !"0".equals(inqXmlParam("fn_isInMyList", pIdSeq)); // �� ���� ����Ʈ�� ������ true ������(0) false
	}

	/** ������ ���� */
	public void setSetting(String pSetId, String pSetValue) {
		dbAdapter.updSettingValue(pSetId, pSetValue);
		fn_setSetting();
	}

	/** DBŬ���� */
	private void fn_dbClear() {
		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
		alt_bld.setMessage(R.string.str_dbClear)
				.setCancelable(false)
				.setPositiveButton(R.string.str_yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dbAdapter.delLC000All();
							}
						})
				.setNegativeButton(R.string.str_no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = alt_bld.create();
		alert.setTitle(R.string.str_rusure);
		alert.show();
	}
	/*****************************
	 * SQLite DB ��Ʈ�� ���� ��
	 *****************************/

	/*****************************
	 * �������� ���� ����
	 *****************************/
	ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
 
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mServiceConn != null ) {
            unbindService(mServiceConn);
        }
    }

    public void AlreadyPurchaseItems() {
        try {
            Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> purchaseDataList = ownedItems
                        .getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                String[] tokens = new String[purchaseDataList.size()];
                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = purchaseDataList.get(i);
                    JSONObject jo = new JSONObject(purchaseData);
                    tokens[i] = jo.getString("purchaseToken");
                    // ���⼭ tokens�� ��� ���� ���ֱ�
                    mService.consumePurchase(3, getPackageName(), tokens[i]);
                }
            }
            // ��ū�� ��� ���������� ���� �޼��� ó��
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Buy(String id_item) {
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), id_item, "inapp", "test");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
 
            if ( pendingIntent != null ) {
            	startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
                //mHelper.launchPurchaseFlow(this, getPackageName(), 1001,  mPurchaseFinishedListener, "test");
            	// ���� ���� ����ȣ���� 2������ �ִµ� �������� ����ϸ� ����� onActivityResult �޼���� ����, �ؿ����� ����ϸ� OnIabPurchaseFinishedListener �޼���� ���ϴ�.  (�����ϼ���!)
            } else {
                // ������ �����ٸ�
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
	   if ( requestCode == 1001 ) {           
	      //int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
	      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
	      //String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
	 
	      if ( resultCode == RESULT_OK ) {
	         try {
	            JSONObject jo = new JSONObject(purchaseData);
	            String sku = jo.getString("productId");
	            Log.d("ff", "You have bought the " + sku + ". Excellent choice, adventurer!");
	          } catch (JSONException e) {
				Log.d("ff", "Failed to parse purchase data.");
				e.printStackTrace();
	          }
	      }
	   }
	}

	private void fn_donation() {
		String pContext = getResources().getString(R.string.str_donation);
		pContext = pContext.replace("\\n", "\n");
	    AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
	    alt_bld.setMessage(pContext)
				.setCancelable(false)
				.setPositiveButton(R.string.str_yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								fn_donationGo();
							}
						})
				.setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = alt_bld.create();
		alert.setTitle(R.string.str_donTitle);
		alert.show();
	}

	int gv_donItem = -1;
	private void fn_donationGo() {
		final String items[]  = getResources().getStringArray(R.array.itm_donName);
		final String itemId[] = getResources().getStringArray(R.array.itm_donId);

		gv_donItem = 0;
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle(R.string.str_donTitle);
		ab.setSingleChoiceItems(items, 0,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { // �� ����Ʈ�� ����������
						gv_donItem = whichButton;
					}
				})
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// OK ��ư Ŭ���� , ���⼭ ������ ���� ���� Activity �� �ѱ�� �ȴ�.
						if ( gv_donItem >= 0 && gv_donItem < itemId.length) {
							Buy(itemId[gv_donItem]);
						}
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Cancel ��ư Ŭ����
							}
						});
		ab.show();
	}
	/*****************************
	 * �������� ���� ��
	 *****************************/
}