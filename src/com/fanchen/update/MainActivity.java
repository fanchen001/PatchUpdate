package com.fanchen.update;

import com.fanchen.update.service.UpdateService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent mIntent = new Intent(this,UpdateService.class);
		mIntent.putExtra("Key_App_Name", "update.patch");
		mIntent.putExtra("Key_Down_Url", "http://hb.1000eb.com/file/update.patch?sid=f039b974bf0246cc85e3f5b152a8df97&id=1fh4u&n=update.patch&t=XwBf%2FPprs%2FR0w%2BuiJ75tJHxkSuUCy5gM1JOFGZb4oMmDjwj5JkL%2BaV0Vmta1dR8l6AxHyli8sqGvoez7OVG1PQ%3D%3D&key=0418C98EA5EBFBE36D219F07E319FEF2&e=7stj16YqNuKvb3mqTAfbRR8v9dwM%2FSvkHo9T9NMjq0M%3D&delay=0");
		startService(mIntent);
	}

}
