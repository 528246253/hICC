package com.hicc.cloud.teacher.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.hicc.cloud.R;
import com.hicc.cloud.teacher.fragment.MarkThisTermFragment;
import com.hicc.cloud.teacher.utils.Logs;
import com.hicc.cloud.teacher.utils.ToastUtli;
import com.hicc.cloud.teacher.view.ScrollViewPager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

import com.hicc.cloud.teacher.fragment.MarkThisTermFragment;
import com.hicc.cloud.teacher.fragment.MarkHistoryTermFragment;

/**
 * Created by 野 on 2016/10/13.
 */

public class StudentMarkActivity extends AppCompatActivity {
    private ImageView iv_back;
    private EditText et_search;
    private ImageButton ib_search;
    private TextView tv_name;
    private TextView tv_class;
    private TextView tv_stu_num;
    private TextView tv_course;
    private TextView tv_teacher;
    private TextView tv_mark;
    private String URL = "http://suguan.hicc.cn/hiccphonet/getStudentInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentprofile_bak);

        initUI();

        // 搜索学号
        searchNum();
    }

    private void searchNum() {
        ib_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = et_search.getText().toString().trim();
                if (!num.equals("")) {
                    // 发送GET请求
                    OkHttpUtils
                            .get()
                            .url(URL)
                            .addParams("studentNum", num)
                            .build()
                            .execute(new StringCallback() {
                                @Override
                                public void onError(Call call, Exception e, int id) {
                                    Logs.i(e.toString());
                                    ToastUtli.show(getApplicationContext(), "服务器繁忙，请重新查询");
                                    et_search.setText("");
                                }

                                @Override
                                public void onResponse(String response, int id) {
                                    Logs.i(response);
                                    // 解析json
                                    getJsonInfo(response);
                                }
                            });
                } else {
                    ToastUtli.show(getApplicationContext(), "学号不能为空");
                }
            }
        });
    }

    private void getJsonInfo(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            boolean sucessed = jsonObject.getBoolean("sucessed");
            if (sucessed) {
                et_search.setText("");

                JSONObject data = jsonObject.getJSONObject("data");
                JSONObject dataInfo = data.getJSONObject("dataInfo");
                // 学生姓名
                String stuName = dataInfo.getString("StudentName");
                tv_name.setText("姓名：" + stuName);
                // 年级
                String grade = dataInfo.getString("GradeDescription");
                // 学号
                String stuNum = dataInfo.getString("StudentNu");
                tv_stu_num.setText("学号：" + stuNum);
                // 班级
                String classDes = dataInfo.getString("ClassDescription");
                tv_class.setText("班级：" + classDes);
                //课程名
                String course = dataInfo.getString("KCM");
                tv_course.setText("课程" +course);
                //任课老师
                String teacher = dataInfo.getString("SKJS");
                tv_teacher.setText("任课老师"+teacher);
                //成绩
                String mark = dataInfo.getString("KCCJ");
                tv_mark.setText("成绩"+mark);

            } else {
                tv_name.setText("姓名：");
                tv_stu_num.setText("学号：");
                tv_class.setText("班级：");
                ToastUtli.show(getApplicationContext(), "学号错误，请检查无误后输入");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        // 返回
        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        et_search = (EditText) findViewById(R.id.et_search);
        ib_search = (ImageButton) findViewById(R.id.ib_search);
        tv_name = (TextView) findViewById(R.id.tv_name);
        tv_class = (TextView) findViewById(R.id.tv_class);
        tv_stu_num = (TextView) findViewById(R.id.tv_stu_num);


        // 设置viewpager
        ScrollViewPager viewPager = (ScrollViewPager) findViewById(R.id.viewpager);
        // 设置viewpager是否禁止滑动
        viewPager.setNoScroll(false);
        setupViewPager(viewPager);

        // 设置tabs
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#9AB4E2"));
        tabLayout.setupWithViewPager(viewPager);
    }

    // 设置viewpager
    private void setupViewPager(ViewPager viewPager) {
        StudentMarkActivity.ViewPagerAdapter adapter = new StudentMarkActivity.ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new MarkThisTermFragment(), "本学期成绩");
        adapter.addFrag(new MarkHistoryTermFragment(), "历史成绩");

        viewPager.setAdapter(adapter);
    }

    // viewPager适配器
    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}