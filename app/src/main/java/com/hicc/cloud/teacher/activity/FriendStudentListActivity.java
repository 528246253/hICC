package com.hicc.cloud.teacher.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hicc.cloud.R;
import com.hicc.cloud.teacher.bean.Student;
import com.hicc.cloud.teacher.utils.Logs;
import com.hicc.cloud.teacher.utils.ToastUtli;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

/**
 * Created by Administrator on 2016/10/26/026.
 */
public class FriendStudentListActivity extends AppCompatActivity {
    private List<Student> mStudentList = new ArrayList<>();
    private ImageView iv_back;
    private ProgressDialog progressDialog;
    private ListView lv_student;
    private String URL = "http://suguan.hicc.cn/hicccloudt/getInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friendstudentlist);

        int timesCode =  getIntent().getIntExtra("timescode",0);
        int divisionCode =  getIntent().getIntExtra("divisionCode",0);
        int professionalCode =  getIntent().getIntExtra("professionalCode",0);
        int classCode =  getIntent().getIntExtra("classcode",0);


        initUI();

        initData(timesCode, divisionCode, professionalCode, classCode);
    }

    private void initData(int timesCode, int divisionCode, int professionalCode, int classCode) {
        showProgressDialog();
        // 发送GET请求
        OkHttpUtils
                .get()
                .url(URL)
                .addParams("timescode", String.valueOf(timesCode))
                .addParams("divisionCode", String.valueOf(divisionCode))
                .addParams("professionalCode", String.valueOf(professionalCode))
                .addParams("classcode", String.valueOf(classCode))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        closeProgressDialog();
                        Logs.i(e.toString());
                        ToastUtli.show(getApplicationContext(),"服务器繁忙，请重新查询");
                    }

                    @Override
                    public void onResponse(String response, int id) {

                        Logs.i(response);
                        // 解析json
                        Logs.i("解析json");
                        getJsonInfo(response);
                    }
                });
    }

    private void getJsonInfo(final String response) {
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean sucessed = jsonObject.getBoolean("sucessed");
                    if(sucessed) {
                        Logs.i("开始解析");

                        JSONArray data = jsonObject.getJSONArray("data");

                        for (int i = 0; i < data.length(); i++) {
                            Student student = new Student();
                            JSONObject studentInfo = data.getJSONObject(i);
                            // 学生姓名
                            student.setStudentName(studentInfo.getString("StudentName"));
                            // 电话
                            student.setYourPhone(studentInfo.getString("YourPhone"));

                            mStudentList.add(student);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Logs.i("大小是："+ mStudentList.size());
                                lv_student.setAdapter(new MyAdapter());
                                closeProgressDialog();
                            }
                        });
                    }
                } catch (JSONException e) {
                    // 解析错误
                    e.printStackTrace();
                    ToastUtli.show(getApplicationContext(),"加载失败");
                    closeProgressDialog();
                }
            }
        }.start();
    }

    private void initUI() {
        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        lv_student = (ListView) findViewById(R.id.lv_student);

        lv_student.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logs.i(mStudentList.get(position).getYourPhone());
                // TODO 点击添加标签
            }
        });
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mStudentList.size();
        }

        @Override
        public Student getItem(int position) {
            return mStudentList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ClassListActivity.ViewHoulder viewHoulder;
            if(convertView == null){
                convertView = View.inflate(getApplicationContext(),R.layout.item_class,null);
                viewHoulder = new ClassListActivity.ViewHoulder();
                viewHoulder.tv_classdes = (TextView) convertView.findViewById(R.id.tv_classdes);
                convertView.setTag(viewHoulder);
            }
            viewHoulder = (ClassListActivity.ViewHoulder) convertView.getTag();
            viewHoulder.tv_classdes.setText(getItem(position).getStudentName());

            return convertView;
        }
    }

    static class ViewHoulder {
        TextView tv_classdes;
    }

    // 显示进度对话框
    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    return;
                }
            });
        }
        progressDialog.show();
    }

    // 关闭进度对话框
    public void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
