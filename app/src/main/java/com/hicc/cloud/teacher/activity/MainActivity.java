package com.hicc.cloud.teacher.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.hicc.cloud.R;
import com.hicc.cloud.teacher.bean.Clas;
import com.hicc.cloud.teacher.bean.Division;
import com.hicc.cloud.teacher.bean.ExitEvent;
import com.hicc.cloud.teacher.bean.Grade;
import com.hicc.cloud.teacher.bean.PhoneInfo;
import com.hicc.cloud.teacher.bean.Professional;
import com.hicc.cloud.teacher.db.StudentInfoDB;
import com.hicc.cloud.teacher.db.UpdateFile;
import com.hicc.cloud.teacher.fragment.BaseFragment;
import com.hicc.cloud.teacher.fragment.FriendFragment;
import com.hicc.cloud.teacher.fragment.HomeFragment;
import com.hicc.cloud.teacher.fragment.InformationFragment;
import com.hicc.cloud.teacher.utils.ConstantValue;
import com.hicc.cloud.teacher.utils.Logs;
import com.hicc.cloud.teacher.utils.PhoneInfoUtil;
import com.hicc.cloud.teacher.utils.SpUtils;
import com.hicc.cloud.teacher.utils.ToastUtli;
import com.hicc.cloud.teacher.view.MyTabLayout;
import com.hicc.cloud.teacher.view.ScrollViewPager;
import com.hicc.cloud.teacher.view.TabItem;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import okhttp3.Call;

/**
 * 主页
 */

public class MainActivity extends AppCompatActivity implements MyTabLayout.OnTabClickListener{
    private MyTabLayout mTabLayout;
    BaseFragment fragment;
    ScrollViewPager mViewPager;
    ArrayList<TabItem>tabs;
    private ProgressDialog progressDialog;
    private BmobQuery<UpdateFile> bmobQuery = new BmobQuery<UpdateFile>();
    private File file = new File(ConstantValue.downloadpathName);
    private BmobFile mBmobfile;
    private static Boolean isExit = false;
    private EditText et_search;
    private boolean isCheck = true;
    private final String URL = "http://suguan.hicc.cn/hicccloudt/getCode";
    private StudentInfoDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = StudentInfoDB.getInstance(this);

        initView();
        initData();

        // 检测更新
        checkVersionCode();

        // 加载数据到数据库中
        creatData();

        // 注册监听退出登录的事件
        EventBus.getDefault().register(this);
    }

    // TODO 每次登陆都发送
    private void getPhoneInfo() {
        final PhoneInfo phoneInfo = PhoneInfoUtil.getPhoneInfo(this);
        phoneInfo.save(this, new SaveListener() {
            @Override
            public void onSuccess() {
                SpUtils.putBoolSp(getApplicationContext(),ConstantValue.FIRST_UP_PHONE_INFO,false);
                Logs.i("手机品牌："+phoneInfo.getPhoneBrand());
                Logs.i("手机型号："+phoneInfo.getPhoneBrandType());
                Logs.i("系统版本："+phoneInfo.getAndroidVersion());
                Logs.i("cpu型号："+phoneInfo.getCpuName());
                Logs.i("IMEI："+phoneInfo.getIMEI());
                Logs.i("IMSI："+phoneInfo.getIMSI());
                Logs.i("手机号："+phoneInfo.getNumer());
                Logs.i("运营商："+phoneInfo.getServiceName());
            }

            @Override
            public void onFailure(int i, String s) {
                // 加载失败  下次进入应用重新加载
                SpUtils.putBoolSp(getApplicationContext(),ConstantValue.FIRST_UP_PHONE_INFO,true);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ExitEvent event) {
        finish();
    }

    private void creatData() {
        if(SpUtils.getBoolSp(this,ConstantValue.FIRST_DATA,true)){
            // 发送GET请求
            OkHttpUtils
                    .get()
                    .url(URL)
                    .addParams("code", "16")
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {
                            Logs.i(e.toString());
                            // 加载失败  下次进入应用重新加载
                            SpUtils.putBoolSp(getApplicationContext(),ConstantValue.FIRST_DATA,true);
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            Logs.i(response);
                            // 解析json
                            getJsonInfo(response);
                        }
                    });


        }

        if(SpUtils.getBoolSp(this,ConstantValue.FIRST_UP_PHONE_INFO,true)){
            // 获取手机信息
           // getPhoneInfo();
        }
    }

    // 解析json
    private void getJsonInfo(final String response) {
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean sucessed = jsonObject.getBoolean("sucessed");
                    if(sucessed){
                        JSONObject data = jsonObject.getJSONObject("data");
                        for(int i=13; i <= 16; i++){
                            JSONArray Grade = data.getJSONArray("Description_"+i);
                            for(int j=0; j < Grade.length(); j++){
                                JSONObject info = Grade.getJSONObject(j);

                                // 年级
                                com.hicc.cloud.teacher.bean.Grade grade = new Grade();
                                int GradeCode = info.getInt("GradeCode");
                                grade.setGradeCode(GradeCode);
                                db.saveGrade(grade);

                                // 学部
                                Division division = new Division();
                                String DivisionDes = info.getString("DivisionDescription");
                                division.setDivisionDes(DivisionDes);
                                if(!DivisionDes.equals("null")){
                                    int DivisionCode = info.getInt("DivisionCode");
                                    division.setDivisionCode(DivisionCode);
                                }
                                int gradeCode = info.getInt("GradeCode");
                                division.setGradeCode(gradeCode);
                                db.saveDivision(division);

                                // 专业
                                Professional professional = new Professional();
                                String ProfessionalDes = info.getString("ProfessionalDescription");
                                professional.setProfessionalDes(ProfessionalDes);
                                if(!ProfessionalDes.equals("null")){
                                    int ProfessionCode = info.getInt("ProfessionalId");
                                    professional.setProfessionalCode(ProfessionCode);
                                    int divisionCode = info.getInt("DivisionCode");
                                    professional.setDivisionCode(divisionCode);
                                }
                                db.saveProfessional(professional);

                                // 班级
                                Clas clas = new Clas();
                                int classCode = info.getInt("Nid");
                                clas.setClassCode(classCode);
                                String ClassDes = info.getString("ClassDescription");
                                clas.setClassDes(ClassDes);
                                clas.setGradeCode(gradeCode);
                                String classQQGroup = info.getString("ClassQQGroup");
                                clas.setClassQQGroup(classQQGroup);
                                if(!ProfessionalDes.equals("null")){
                                    int professionCode = info.getInt("ProfessionalId");
                                    clas.setProfessionalCode(professionCode);
                                }
                                db.saveClass(clas);
                            }
                        }
                        Logs.i("加载数据成功");
                        // 下次进入应用不在加载
                        SpUtils.putBoolSp(getApplicationContext(),ConstantValue.FIRST_DATA,false);
                    }else{
                        // 加载失败  下次进入应用重新加载
                        SpUtils.putBoolSp(getApplicationContext(),ConstantValue.FIRST_DATA,true);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    // 加载失败  下次进入应用重新加载
                    SpUtils.putBoolSp(getApplicationContext(),ConstantValue.FIRST_DATA,true);
                }
            }
        }.start();
    }

    private void initView(){
        mTabLayout=(MyTabLayout)findViewById(R.id.tablayout);
        mViewPager=(ScrollViewPager)findViewById(R.id.viewpager);
        // 设置viewpager是否禁止滑动
        mViewPager.setNoScroll(false);

        // 搜索框
        et_search = (EditText) findViewById(R.id.et_search);
        et_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isCheck){
                    et_search.setHint("");
                    isCheck = !isCheck;
                }else{
                    et_search.setHint("搜索");
                    isCheck = !isCheck;
                }
            }
        });

        // 推送消息记录
        ImageView iv_content = (ImageView) findViewById(R.id.iv_content);
        iv_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtli.show(getApplicationContext(),"努力开发中");
            }
        });

        // 添加
        ImageView iv_add = (ImageView) findViewById(R.id.iv_add);
        iv_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtli.show(getApplicationContext(),"努力开发中");
            }
        });
    }

    private void initData(){
        tabs=new ArrayList<TabItem>();
        tabs.add(new TabItem(R.drawable.selector_tab_home, R.string.tab_home, HomeFragment.class));
        tabs.add(new TabItem(R.drawable.selector_tab_friend, R.string.tab_friend, FriendFragment.class));
        tabs.add(new TabItem(R.drawable.selector_tab_infomation, R.string.tab_information, InformationFragment.class));
        mTabLayout.initData(tabs, this);
        mTabLayout.setCurrentTab(0);

        final FragAdapter adapter = new FragAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mTabLayout.setCurrentTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    // tab的点击事件
    @Override
    public void onTabClick(TabItem tabItem) {
        mViewPager.setCurrentItem(tabs.indexOf(tabItem));
    }

    // Fragment适配器
    public class FragAdapter extends FragmentPagerAdapter {
        public FragAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0) {
            try {
                return tabs.get(arg0).tagFragmentClz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

    }

    // 检测更新
    private void checkVersionCode() {
        if(bmobQuery != null){
            // TODO 旧版本方法
            bmobQuery.findObjects(this, new FindListener<UpdateFile>() {
                @Override
                public void onSuccess(List<UpdateFile> list) {
                    for (UpdateFile updatefile : list) {
                        // 如果服务器的版本号大于本地的  就更新
                        if(updatefile.getVersion() > getVersionCode()){
                            BmobFile bmobfile = updatefile.getFile();
                            mBmobfile = bmobfile;
                            // 文件路径不为null  并且sd卡可用
                            if(file != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                                // 展示下载对话框
                                showUpDataDialog(updatefile.getDescription(),bmobfile,file);
                            }
                        }
                    }
                }

                @Override
                public void onError(int i, String s) {
                    Log.i("Bmob文件传输","查询失败："+s);
                }
            });
        }
    }

    // 显示更新对话框
    protected void showUpDataDialog(String description, final BmobFile bmobfile, final File file) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        //设置对话框左上角图标
        builder.setIcon(R.mipmap.ic_launcher);
        //设置对话框标题
        builder.setTitle("发现新版本");
        //设置对话框内容
        builder.setMessage(description);
        //设置积极的按钮
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //请求权限
                    requestCameraPermission();
                } else {
                    //下载apk
                    downLoadApk(bmobfile, file);
                    // 显示一个进度条对话框
                    showProgressDialog();
                }
            }
        });
        /*//设置消极的按钮
        builder.setNegativeButton("暂不更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });*/
        //监听取消按钮  强制更新
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            //当点击返回的按钮时执行
            @Override
            public void onCancel(DialogInterface dialog) {
                //TODO 退出应用
                finish();
                System.exit(0);
            }
        });

        builder.show();
    }

    // 下载的进度条对话框
    protected void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIcon(R.mipmap.ic_launcher);
        progressDialog.setTitle("下载安装包中");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        //progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
    }

    // 下载文件
    private void downLoadApk(BmobFile bmobfile, final File file) {
        //调用bmobfile.download方法
        // TODO 旧版本方法
        bmobfile.download(this, file, new DownloadFileListener() {
            @Override
            public void onSuccess(String s) {
                ToastUtli.show(getApplicationContext(),"下载成功,保存路径:"+ ConstantValue.downloadpathName);
                Log.i("Bmob文件下载","下载成功,保存路径:"+ConstantValue.downloadpathName);
                installApk(file);
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(int i, String s) {
                ToastUtli.show(getApplicationContext(),"下载失败："+s+","+s);
                Log.i("Bmob文件下载","下载失败："+i+","+s);
                progressDialog.dismiss();
            }
        });
    }

    // 安装应用
    protected void installApk(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        //文件作为数据源
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent,0);
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkVersionCode();
    }

    // 获取本应用版本号
    private int getVersionCode() {
        // 拿到包管理者
        PackageManager pm = getPackageManager();
        // 获取包的基本信息
        try {
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            // 返回应用的版本号
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 请求权限
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    // 请求权限结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //下载apk
                downLoadApk(mBmobfile, file);
                // 显示一个进度条对话框
                showProgressDialog();
            } else {
                ToastUtli.show(getApplicationContext(),"请求写入文件");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // 监听返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            exitBy2Click();
        }
        return false;
    }

    // 双击退出程序
    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // 准备退出
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // 取消退出
                }
            }, 2000); // 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务

        } else {
            finish();
            System.exit(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
