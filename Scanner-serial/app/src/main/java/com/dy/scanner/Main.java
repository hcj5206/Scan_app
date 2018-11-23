package com.dy.scanner;

import com.smatek.uart.UartComm;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.BroadcastReceiver;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

import android.media.AudioManager;

import org.apache.http.conn.ConnectTimeoutException;


public class Main extends AppCompatActivity {

    public TextView Ip_show;
    public TextView Gun_id_show;
    public TextView version;
    public Button Login_but;
    public EditText User_id;
    public TextView Job_name;
    public TextView textbox;
    public TextView Heap;
    public TextView incoming;
    public TextView TextS6;
    public TextView DoorID;
    public TextView State;
    public TextView Gun_id;
    private Button but_arr;
    private Button but_arr_no;
    private Button err_button;
    private Button exit_button;
    private Button manual_button;
    private boolean mRunning = false;
    public TextView IP;
    public Receiver receiver = new Receiver();
    public Download_rec download_rec = new Download_rec();
    public int state = 0;
    public int uart_fd;
    public int position = 0;
    public int err_report = 0;
    public int show_time_main = 0;
    public int show_time_secondary = 0;
    public int job = 0;
    long file_id = 0;
    private String hard_id;
    private String  ver="Ver:180922a ";
    SharedPreferences ipsp;
    public  int Toedge = 0;
    public UartComm UC;
    private String recv_msg,but;
    private static String hexString="0123456789ABCDEF";
    private static String hexString1="0123456789abcdef";
    MediaPlayer mediaPlayer = new MediaPlayer();
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideBottomUIMenu();
        super.onCreate(savedInstanceState);


        ipsp = getSharedPreferences("Mainserver_ip", Context.MODE_PRIVATE);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                player.seekTo(0);
            }
        });
        AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.scan);
        try {
            mediaPlayer.setDataSource(file.getFileDescriptor(),
                    file.getStartOffset(), file.getLength());
            file.close();
            mediaPlayer.setVolume(100,100);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            Log.w("Sound", ioe);
            mediaPlayer = null;
        }
        StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        String ANDROID_ID = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        Log.w("State", "Id:" + ANDROID_ID);
        hard_id = ANDROID_ID;
        UC = new UartComm();
        uart_fd = UC.uartInit("/dev/ttyS3");
        UC.setOpt(uart_fd, 9600, 8, 0, 1);
        IntentFilter filter = new IntentFilter("android.intent.DECODE");
        registerReceiver(receiver, filter);

        mRunning = true;
        new Thread(new Serial_rec()).start();
        String initstr = post("0", "3");
        if(initstr.contains(":")) {
            initmain();
            if(initstr.contains("<br />"))
            {
                initstr = initstr.substring(0,initstr.length()-6);
            }
            String spilt[] = initstr.split(":");
            incoming.setText(spilt[0]);
            textbox.setText(spilt[1]);
            Job_name.setText(spilt[2]);

            if(spilt[3].length()>5)
            {
                String Return = spilt[3];
                Heap.setVisibility(View.VISIBLE);
                Heap.setText(Return.split("&")[0]);
                State.setVisibility(View.VISIBLE);
                State.setText(Return.split("&")[1]);
                String color = Return.split("&")[2];
                switch (color) {
                    case "r":
                        Heap.setTextColor(Color.RED);
                        break;
                    case "g":
                        Heap.setTextColor(Color.GREEN);
                        break;
                    case "b":
                        Heap.setTextColor(Color.BLUE);
                        break;
                    case "y":
                        Heap.setTextColor(Color.YELLOW);
                        break;
                    case "bk":
                        Heap.setTextColor(Color.BLACK);
                        break;
                }
                String show_time = Return.split("&")[3];
                show_time_main = Integer.parseInt(show_time.split(",")[0]);
                show_time_secondary = Integer.parseInt(show_time.split(",")[1]);
                DoorID.setVisibility(View.VISIBLE);
                DoorID.setText(Return.split("&")[4]);
                if(Return.split("&")[5].contains("1"))
                {
                    Toedge = 1;
                    but_arr.setVisibility(View.VISIBLE);
                    but_arr.setOnClickListener(listener);
                    if (Return.split("&")[0].contains("交付模压？"))
                    {   Toedge = 3;
                        but_arr_no.setVisibility(View.VISIBLE);
                        but_arr_no.setOnClickListener(listener);
                    }

                }
                if(show_time_main>0)
                    new Thread(new Wait_after_rec()).start();
                if(show_time_secondary>0)
                    new Thread(new Wait_after_rec_secondary()).start();
            }
            state = 1;
        }
        else if(initstr.contains("error0"))
        {
            initlogin();
            showInputDialog();
        }
        else if(initstr.contains("update"))
        {
            if(initstr.contains("<br />"))
            {
                initstr = initstr.substring(0,initstr.length()-6);
            }
            update_app(initstr.split("&")[1]);
        }
        else
        {
            initlogin();
            //showLoginDialog();
        }
    }

    private void initlogin()
    {
        setContentView(R.layout.login);
        getSupportActionBar().hide();
        Ip_show = (TextView)findViewById(R.id.IP_show);
        version = (TextView)findViewById(R.id.version);
        Gun_id_show = (TextView)findViewById(R.id.Gun_id_show);
        Login_but = (Button)findViewById(R.id.login_button);
        User_id = (EditText)findViewById(R.id.Job_id_box);
        Gun_id_show.setText(hard_id);
        version.setText(ver);
        Ip_show.setText(ipsp.getString("Ip", "192.168.31.250"));
        Ip_show.setOnClickListener(ip_onclck);
        Login_but.setOnClickListener(login_click_listener);
    }

    private void initmain(){
        setContentView(R.layout.main);
        getSupportActionBar().hide();
        Job_name = (TextView)findViewById(R.id.Text_job_show);
        exit_button = (Button) findViewById(R.id.but_exit);
        exit_button.setOnClickListener(exit_but_listener);
        textbox = (TextView) findViewById(R.id.Text_job_id);
        incoming = (TextView) findViewById(R.id.Text_name_show);
        Heap = (TextView) findViewById(R.id.Text_main_show);
        TextS6 = (TextView) findViewById(R.id.textS6);
        DoorID = (TextView) findViewById(R.id.DoorID);
        State = (TextView) findViewById(R.id.Text_sec_show);
        Gun_id = (TextView) findViewById(R.id.gun_id);
        but_arr = (Button) findViewById(R.id.but_is_arr);//确定
        but_arr_no = (Button) findViewById(R.id.but_no_arr);//取消
        err_button = (Button) findViewById(R.id.Errorbutton);
        manual_button = (Button) findViewById(R.id.Manual_type_button);
        manual_button.setOnClickListener(manual_button_listener);
        err_button.setOnClickListener(err_but_listener);
        Gun_id.setText(hard_id);
        version = (TextView)findViewById(R.id.version);
        version.setText(ver);
        /*IP.setText(ipsp.getString("Ip", "192.168.31.250"));
        IP.setOnClickListener(ip_onclck);*/
    }

    private void showInputDialog() {
    /*@setView 装入一个EditView
     */
        final EditText editText = new EditText(Main.this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        editText.setText(ipsp.getString("Ip", "192.168.31.250"));
        inputDialog.setTitle("请确认服务器ip").setView(editText);
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Ip_show.setText(editText.getText().toString());
                        ipsp.edit().putString("Ip", editText.getText().toString()).apply();
                        Toast.makeText(Main.this,
                                "服务器修改为：" + editText.getText().toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        inputDialog.show();
    }//修改ip

    private void showEnterDialog() {
    /*@setView 装入一个EditView
     */
        final EditText editText = new EditText(Main.this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setTitle("请输入零件编号").setView(editText);
        inputDialog.setPositiveButton("确定",
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.DECODE");
                        intent.putExtra("barcode", editText.getText().toString());
                        //发送广播
                        sendBroadcast(intent);
                    }
                });
        inputDialog.show();
    }//手动输入


    private void showErrorDialog() {
    /*@setView 装入一个EditView
     */
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setTitle("确认报错？");
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        err_report = 1;
                        Toast.makeText(Main.this,
                                "请扫描问题工件条码进行报错",
                                Toast.LENGTH_SHORT).show();
                        State.setText("请扫描问题工件条码进行报错");
                        State.setVisibility(View.VISIBLE);
                    }
                });
        inputDialog.show();
    }//报错

    private void showExitDialog() {
    /*@setView 装入一个EditView
     */
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setTitle("确认登出？");
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.DECODE");
                        intent.putExtra("barcode", "YG" + textbox.getText().toString());
                        //发送广播
                        sendBroadcast(intent);
                    }
                });
        inputDialog.show();
    }//登出



    private void showAdminLoginDialog(final String jobid,String[] jobs) {
        Resources res =getResources();
        final String[] strings = jobs;
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setCancelable(false);
        inputDialog.setTitle("请选择工位").setSingleChoiceItems(strings, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        job = which+1;
                    }
                });
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.DECODE");
                        intent.putExtra("barcode", "SU" + jobid);
                        //发送广播
                        sendBroadcast(intent);
                    }
                });
        inputDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        inputDialog.show();
    }//显示管理员选择工位界面


    private void showAdminLoginDialog_car(final String jobid,String[] jobs) {
        Resources res =getResources();
        final String[] strings = jobs;
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setCancelable(false);
        inputDialog.setTitle("请选择报错类型").setSingleChoiceItems(strings, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        job = which;
                    }
                });
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.DECODE");
                        intent.putExtra("barcode", "Car_error:" + jobid+"&"+job);
                        //发送广播
                        sendBroadcast(intent);
                    }
                });
        inputDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        inputDialog.show();
    }//显示小车报错类型信息


    private void showAdminLoginDialog_Admin(final String jobid,String[] jobs,String[] position) {
        Resources res =getResources();
        final String[] strings_position = position;
        final String[] strings = jobs;
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setCancelable(false);
        inputDialog.setTitle("请选择报错类型").setSingleChoiceItems(strings, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        job = which;
                    }
                });
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        showAdminLoginDialog_choose_error_position(jobid+"&"+job,strings_position);
                    }
                });
        inputDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        inputDialog.show();
    }//显示管理员报错信息


    private void showAdminLoginDialog_choose_error_position(final String jobid,String[] jobs) {
        Resources res =getResources();
        final String[] strings = jobs;
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(Main.this);
        inputDialog.setCancelable(false);
        inputDialog.setTitle("请选择报错类型").setSingleChoiceItems(strings, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        job = which;
                    }
                });
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.DECODE");
                        intent.putExtra("barcode", "Admin_error:" + jobid+"&"+job);
                        //发送广播
                        sendBroadcast(intent);
                    }
                });
        inputDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        inputDialog.show();
    }//显示管理员选择错误工位。
    protected void update_app(String link){

        Log.w("Update", "访问:" + link);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse("http://"+link);
        DownloadManager.Request request = new DownloadManager.Request(content_url);
        request.setDestinationInExternalPublicDir("/download/", "scan.apk");
        request.setNotificationVisibility(1);
        DownloadManager downloadManager= (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        file_id = downloadManager.enqueue(request);
        //Toast.makeText(this, "下载文件"+file_id, Toast.LENGTH_SHORT).show();
        IntentFilter download_filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(download_rec, download_filter);
        /*try {
            downloadManager.openDownloadedFile(file_id);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "文件打开失败", Toast.LENGTH_SHORT).show();
        }*/

        /*intent.setData(content_url);
        intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
        startActivity(intent);*/
    }//更新app

    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }//隐藏按钮

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    public class Wait_after_rec implements Runnable{

        @Override
        public void run() {
            try{
                Thread.sleep(show_time_main*1000);
                Main.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Heap.setVisibility(View.INVISIBLE);
                    }
                });
                //Heap.setVisibility(View.INVISIBLE);
            }catch(InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public class Wait_after_rec_secondary implements Runnable{

        @Override
        public void run() {
            try{
                Thread.sleep(show_time_secondary*1000);
                Main.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        State.setVisibility(View.INVISIBLE);
                    }
                });
            }catch(InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public class Serial_rec implements Runnable{

        @Override
        public void run() {
            int size;
            int[] buffer = new int[256];
            boolean flog_get=true;
            while(mRunning){
                size = UC.recv(uart_fd, buffer, 256);
                if(flog_get)
                {
                    but="";
                    recv_msg="";
                    flog_get=false;
                }
                if(size != 0) {
                    for(int i=0; i<size; i++) {
                        but += Integer.toHexString(buffer[i] &0xff);
                    }
                    if(but.charAt((but.length())-1)!='a')
                        continue;
                    but=but.substring(0, but.length() - 2);
                    recv_msg=decode(but);
                    if(recv_msg.contains("*"))
                    {
                        recv_msg=recv_msg.substring(1,recv_msg.length()-1);
                    }
//                    sendKeyCode(recv_msg+"\n");
			 			Log.w("Serial", but);
			 			Log.w("Serial", recv_msg);
                        Intent intent = new Intent();
                        intent.setAction("android.intent.DECODE");
                        intent.putExtra("barcode", recv_msg);
                    //发送广播
                        sendBroadcast(intent);
//                    mUIHandler.sendEmptyMessage(MSG_UPDATE);
                    flog_get=true;
                } //end if(size != 0)
            } //end while

        }
    }

    public class Download_rec extends BroadcastReceiver {
        @Override
        public void onReceive(Context contest, Intent intent) {
            long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (file_id == reference) {
                DownloadManager download= (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                try {
                    Toast.makeText(contest, "正在打开文件"+reference, Toast.LENGTH_SHORT).show();
                    Intent install_intent = new Intent(Intent.ACTION_VIEW);
                    // 由于没有在Activity环境下启动Activity,设置下面的标签
                    install_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    install_intent.setDataAndType(download.getUriForDownloadedFile(reference),
                            "application/vnd.android.package-archive");
                    contest.startActivity(install_intent);
                } catch (Exception e) {
                    Toast.makeText(contest, "文件打开失败！", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context contest, Intent intent) {

            if (mediaPlayer != null) {
                mediaPlayer.start();
            }

            //if(jobchoose.getSelectedItemPosition()!=0) {
                String Code_Scaned = intent.getExtras().getString("barcode");
                Log.w("Recevier", "接收到:" + Code_Scaned);
                String incomingstr;

                if(Toedge == 1)
                {
                    incomingstr = "error3";
                }
                else {
                    assert Code_Scaned != null;
                    if(Code_Scaned.contains("YG")||Code_Scaned.contains("yg")) //员工号
                    {
                        incomingstr = post(Code_Scaned.split("YG")[1],"1");
                    }
                    else if(Code_Scaned.contains("SU"))
                    {
                        incomingstr = post(Code_Scaned.split("SU")[1],"6");
                    }
                    else
                    {
                        if(state == 1) {
                            if (err_report == 0)
                            {
                                if(Code_Scaned.contains("Car_error")){
                                    incomingstr = post(Code_Scaned.split(":")[1], "7");

                                }
                                if(Code_Scaned.contains("Admin_error")){
                                    incomingstr = post(Code_Scaned.split(":")[1], "8");

                                }
                                else{
                                    incomingstr = post(Code_Scaned, "2");

                                }

                            }

                            else {
                                incomingstr = post(Code_Scaned, "5");
                                err_report = 0;
                            }
                        }
                        else
                        {
                            incomingstr = "error2";
                            Toast.makeText(contest, "请先登录！", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            Log.w("Receiver:",incomingstr);
                if(incomingstr.contains("<br />"))
                {
                    incomingstr = incomingstr.substring(0,incomingstr.length()-6);
                }
                if(incomingstr.equals("off")&&state==1) {
                    //password.setVisibility(View.VISIBLE);
                    Toast.makeText(contest, "信息提交成功，"+incoming.getText().toString()+"已下岗", Toast.LENGTH_SHORT).show();
                    incoming.setText("未授权");
                    textbox.setText("未授权");
                    /*TextS6.setVisibility(View.INVISIBLE);
                    Heap.setVisibility(View.INVISIBLE);
                    DoorID.setVisibility(View.INVISIBLE);
                    State.setVisibility(View.INVISIBLE);
                    err_button.setVisibility(View.INVISIBLE);
                    exit_button.setVisibility(View.INVISIBLE);
                    manual_button.setVisibility(View.INVISIBLE);*/
                    initlogin();
                    //showLoginDialog();
                    state = 0;
                }
                else if(incomingstr.equals("error3"))
                {
                    Toast.makeText(contest, "请送至下一工位", Toast.LENGTH_SHORT).show();
                    /*State.setText("请送至下一工位");
                    State.setVisibility(View.VISIBLE);*/
                }
                else if(incomingstr.equals("ok2")){
                    State.setText("已进入当前工序");
                    State.setVisibility(View.VISIBLE);

                }
                else if(incomingstr.contains("error_ok")){
                    State.setVisibility(View.INVISIBLE);
                    Toast.makeText(contest, "报错成功", Toast.LENGTH_SHORT).show();
                    if(incomingstr.contains("Car")){
                        String spilt[] = incomingstr.split(":");
                        showAdminLoginDialog_car(spilt[0],spilt[1].split("&"));
// 0901
                    }
                    if(incomingstr.contains("Admin"))   //管理人员报错机制
                    {
                        String spilt[] = incomingstr.split(":");
                        showAdminLoginDialog_Admin(spilt[0],spilt[1].split("&"),spilt[2].split("&"));
// 0901
                    }

                }
                else if(incomingstr.contains("error_error")){
                    State.setVisibility(View.INVISIBLE);
                    Toast.makeText(contest, "报错失败", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if(incomingstr.contains("RET"))
                    {
                        String Return = incomingstr.split("RET")[1];
                        Heap.setVisibility(View.VISIBLE);
                        Heap.setText(Return.split("&")[0]);
                        State.setVisibility(View.VISIBLE);
                        State.setText(Return.split("&")[1]);
                        String color = Return.split("&")[2];
                        switch (color) {
                            case "r":
                                Heap.setTextColor(Color.RED);
                                break;
                            case "g":
                                Heap.setTextColor(Color.GREEN);
                                break;
                            case "b":
                                Heap.setTextColor(Color.BLUE);
                                break;
                            case "y":
                                Heap.setTextColor(Color.YELLOW);
                                break;
                            case "bk":
                                Heap.setTextColor(Color.BLACK);
                                break;
                        }
                        String show_time = Return.split("&")[3];
                        show_time_main = Integer.parseInt(show_time.split(",")[0]);
                        show_time_secondary = Integer.parseInt(show_time.split(",")[1]);
                        DoorID.setVisibility(View.VISIBLE);
                        DoorID.setText(Return.split("&")[4]);
                        if(Return.split("&")[5].contains("1"))
                        {
                            Toedge = 1;
                            but_arr.setVisibility(View.VISIBLE);
                            but_arr.setOnClickListener(listener);
                            if (Return.split("&")[1].contains("交付模压？"))
                            {
                                but_arr_no.setVisibility(View.VISIBLE);
                                but_arr_no.setOnClickListener(listener);

                            }

                        }
                        if(show_time_main>0)
                            new Thread(new Wait_after_rec()).start();
                        if(show_time_secondary>0)
                            new Thread(new Wait_after_rec_secondary()).start();
                    }
                    if(incomingstr.contains("SU"))
                    {
                        String spilt[] = incomingstr.split(":");
                        showAdminLoginDialog(spilt[0],spilt[1].split("&"));
                    }
                    else if(incomingstr.contains(":")) {
                        initmain();
                        String spilt[] = incomingstr.split(":");
                        incoming.setText(spilt[0]);
                        textbox.setText(spilt[1]);
                        Job_name.setText(spilt[2]);
                        Log.w("Login",incomingstr);
                        /*State.setVisibility(View.INVISIBLE);
                        TextS6.setVisibility(View.VISIBLE);
                        err_button.setVisibility(View.VISIBLE);
                        exit_button.setVisibility(View.VISIBLE);
                        manual_button.setVisibility(View.VISIBLE);*/
                        //Toast.makeText(contest, "信息提交成功，" + incomingstr + "已上岗", Toast.LENGTH_SHORT).show();
                        state = 1;
                    }
                }
           // }
           // else
            //    Toast.makeText(contest, "请选择工位", Toast.LENGTH_SHORT).show();
        }
    }

    Button.OnClickListener listener = new Button.OnClickListener(){//创建监听对象
        public void onClick(View v){
            switch (v.getId()){
                case R.id.but_is_arr:
                    String incomingstr1;
                    incomingstr1=post("ok1", "4");

                    if(incomingstr1.contains("ok1"))
                    {
                        Toedge = 0;
                        but_arr.setVisibility(View.INVISIBLE);
                        but_arr_no.setVisibility(View.INVISIBLE);
                        Toast.makeText( getApplicationContext(),"已确认送达", Toast.LENGTH_SHORT).show();
                    }
                    if(incomingstr1.contains("尚未交付完成")) {

                        Heap.setVisibility(View.VISIBLE);
                        Heap.setText(incomingstr1.split("&")[0]);
                        State.setVisibility(View.VISIBLE);
                        State.setText(incomingstr1.split("&")[1]);
                        String color = incomingstr1.split("&")[2];
                        switch (color) {
                            case "r":
                                Heap.setTextColor(Color.RED);
                                break;
                            case "g":
                                Heap.setTextColor(Color.GREEN);
                                break;
                            case "b":
                                Heap.setTextColor(Color.BLUE);
                                break;
                            case "y":
                                Heap.setTextColor(Color.YELLOW);
                                break;
                            case "bk":
                                Heap.setTextColor(Color.BLACK);
                                break;
                        }
                            Toedge = 0;
                            but_arr.setVisibility(View.INVISIBLE);
                            but_arr_no.setVisibility(View.INVISIBLE);
                            Toast.makeText(getApplicationContext(), "已确认送达", Toast.LENGTH_SHORT).show();
                        }

                break;
                case R.id.but_no_arr:
                    String incomingstr2;
                    incomingstr2=post("no1", "4");

                    if(incomingstr2.contains("ok1"))
                    {
                        Toedge = 0;
                        but_arr.setVisibility(View.INVISIBLE);
                        but_arr_no.setVisibility(View.INVISIBLE);
                        Toast.makeText( getApplicationContext(),"请重新扫描", Toast.LENGTH_SHORT).show();
                    }


                break;


            }

        }

        public void Click_Cancel(View v){
            String incomingstr1;
            incomingstr1=post("ok1", "4");
            if(incomingstr1.contains("ok1"))
            {
                but_arr.setVisibility(View.INVISIBLE);
                Toedge = 0;
                Toast.makeText( getApplicationContext(),"已确认送达", Toast.LENGTH_SHORT).show();
            }
        }
    };

    Button.OnClickListener exit_but_listener = new Button.OnClickListener(){
        public void onClick(View v){
            showExitDialog();
        }
    };

    Button.OnClickListener manual_button_listener = new Button.OnClickListener(){
        public void onClick(View v){
            showEnterDialog();
        }
    };

    Button.OnClickListener err_but_listener = new Button.OnClickListener(){
        public void onClick(View v){
            showErrorDialog();
        }
    };

    Button.OnClickListener ip_onclck = new Button.OnClickListener(){
        public void onClick(View v){
            showInputDialog();
        }
    };

    Button.OnClickListener login_click_listener = new Button.OnClickListener(){
        public void onClick(View v){
            Intent intent = new Intent();
            intent.setAction("android.intent.DECODE");
            intent.putExtra("barcode", "YG"+User_id.getText().toString());
            //发送广播
            sendBroadcast(intent);
        }
    };


    public String post(String id,String func){
        String output = "error";
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("id=" + URLEncoder.encode(id, "UTF-8") + "&");
            buf.append("hardid=" + URLEncoder.encode(hard_id, "UTF-8") + "&");
            if(func.equals("6"))
            {
                buf.append("station=" + URLEncoder.encode(String.valueOf(job), "UTF-8") + "&");
            }
            buf.append("function=" + URLEncoder.encode(func, "UTF-8"));
            Log.w("Post", "Send:" + buf.toString());
            byte[] data = buf.toString().getBytes("UTF-8");
            String url_srt = "http://"+ipsp.getString("Ip", "192.168.31.250")+"/scan/scan-new.php";
            //String url_srt = "http://baidu.com";
            Log.w("Post", url_srt);
            URL url = new URL(url_srt);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.w("Post", "Connection opened!");
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true); //如果要输出，则必须加上此句
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            OutputStream out = conn.getOutputStream();
            Log.w("Post", "Method confirmed!");
            out.write(data);
            Log.w("Post", "已发送");
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));
                output = reader.readLine();
                Toast.makeText( getApplicationContext(),"扫码成功", Toast.LENGTH_SHORT).show();
                Log.w("Post", "Return:"+output);
            }
            else
                Toast.makeText(this, "信息提交失败，请检查网络", Toast.LENGTH_SHORT).show();
        } catch (SocketTimeoutException e){
            Log.e("Post", "Socket timeout");
            output = "error0";
            Toast.makeText(this, "信息提交失败，系统未返回信息，请启动主程序。", Toast.LENGTH_SHORT).show();
        } catch (ConnectTimeoutException e){
            Log.e("Post", "Connection timeout");
            output = "error0";
            Toast.makeText(this, "信息提交失败，请检查网络", Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            Log.e("Post", "E1"+e.getMessage());
            output = "error1";
            Toast.makeText(this, "信息提交失败，请检查网络", Toast.LENGTH_SHORT).show();
        }
        return output;
    }

    public static String decode(String bytes)
    {
        ByteArrayOutputStream baos=new ByteArrayOutputStream(bytes.length()/2);
        //将每2位16进制整数组装成一个字节`
        for(int i=0;i<bytes.length();i+=2)
            baos.write((hexString1.indexOf(bytes.charAt(i))<<4 |hexString1.indexOf(bytes.charAt(i+1))));
        String bb = "";
        try {
            bb = new String(baos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bb;
    }

}