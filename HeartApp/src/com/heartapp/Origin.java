package com.heartapp;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Origin extends Activity {

	private LayoutInflater inflater;
	private LinearLayout llay,orgllay;
	private ScrollView SV;
	private SQLiteDatabase db;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		db = openOrCreateDatabase("HeartRateDB", MODE_WORLD_WRITEABLE, null);
		
		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		llay = (LinearLayout) inflater.inflate(R.layout.datas, null);
		SV = (ScrollView) inflater.inflate(R.layout.originbutton, null);
		
		orgllay = (LinearLayout) SV.findViewById(R.id.LinearLayout1);
		
		String sql = "select * from heartrate";
		Cursor cursor = db.rawQuery(sql, null);
		
		while(cursor.moveToNext()) {
			llay = (LinearLayout) inflater.inflate(R.layout.datas, null);
			
			TextView tvDate = (TextView) llay.findViewById(R.id.tv_date);
			tvDate.setText(cursor.getString(0));
			
			TextView tvRate = (TextView) llay.findViewById(R.id.tv_heartrate);
			tvRate.setText(cursor.getString(1));
			
			orgllay.addView(llay);

		}

		setContentView(SV);
		
	}
	
	
}
