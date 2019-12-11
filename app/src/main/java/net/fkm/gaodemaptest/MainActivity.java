package net.fkm.gaodemaptest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;

import net.fkm.gaodemaptest.utils.L;
import net.fkm.gaodemaptest.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity implements LocationSource, AMapLocationListener {

    private static final int WRITE_COARSE_LOCATION_REQUEST_CODE = 0;

    @BindView(R.id.map)
    MapView mapView;
    @BindView(R.id.tv_city)
    TextView tv_city;
    @BindView(R.id.btn_check_in)
    TextView btn_check_in;

    private AMap aMap;//地图对象
    private Circle circle;
    private LatLng locLatLng = null; // 定位坐标
    private LatLng comLatLng = null; // 公司坐标
    private float radius = 200;

    //定位需要的声明
    private AMapLocationClient mLocationClient = null;//定位发起端
    private AMapLocationClientOption mLocationOption = null;//定位参数
    private OnLocationChangedListener mListener = null;//定位监听器

    //标识，用于判断是否只显示一次定位信息和用户重新定位
    private boolean isFirstLoc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        // 启动新的线程
        new TimeThread().start();
        // 显示地图，必须要写
        mapView.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initView() {

        //获取地图对象
        aMap = mapView.getMap();
        //设置显示定位按钮 并且可以点击
        UiSettings settings = aMap.getUiSettings();
        //设置定位监听
        aMap.setLocationSource(this);
        // 是否显示定位按钮
        settings.setMyLocationButtonEnabled(true);
        // 是否显示地图方向盘
        settings.setCompassEnabled(true);
        // 是否可触发定位并显示定位层
        aMap.setMyLocationEnabled(true);

        //定位的小图标 默认是蓝点，其实就是一张图片
//        MyLocationStyle myLocationStyle = new MyLocationStyle();
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.icon_point));
//        myLocationStyle.radiusFillColor(R.color.transparent1);
//        myLocationStyle.strokeColor(R.color.transparent1);
//        aMap.setMyLocationStyle(myLocationStyle);

        // 判断是否为Android 6.0 以上的系统版本，如果是，需要动态添加权限
        if (Build.VERSION.SDK_INT >= 23) {
            showPermissions();
        } else {
            // 开始定位
            initLoc();
        }

    }

    private void initData() {

    }

    @OnClick({R.id.btn_check_in})
    void click(View v) {
        switch (v.getId()) {
            case R.id.btn_check_in:
                if (locLatLng != null && comLatLng != null) {
                    float distance = AMapUtils.calculateLineDistance(locLatLng, comLatLng);
                    L.i("连点之间的距离：" + distance);
                    if (distance <= radius) {
                        // TODO 这里模拟把打卡的信息提交到服务器，服务器并且把打卡成功信息返回给客户端
                        ToastUtil.showToastLong("打卡成功");
                    } else {
                        ToastUtil.showToastLong("当前位置不打卡范围内，打卡失败");
                    }
                } else {
                    ToastUtil.showToastLong("位置初始化异常，打卡失败");
                }
                break;
            default:
                break;
        }
    }

    // 定位
    private void initLoc() {

        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(3000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    // 定位回调函数
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {

        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间
                amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                amapLocation.getCountry();//国家信息
                amapLocation.getProvince();//省信息
                amapLocation.getCity();//城市信息
                amapLocation.getDistrict();//城区信息
                amapLocation.getStreet();//街道信息
                amapLocation.getStreetNum();//街道门牌号信息
                amapLocation.getCityCode();//城市编码
                amapLocation.getAdCode();//地区编码
                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    //设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(amapLocation);
                    //添加图钉
                    aMap.addMarker(getMarkerOptions(amapLocation));
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(amapLocation.getAddress());
                    tv_city.setText(amapLocation.getCity());
//                    buffer.append(amapLocation.getCountry() + "" + amapLocation.getProvince() + "" + amapLocation.getCity() + "" + amapLocation.getProvince() + "" + amapLocation.getDistrict() + "" + amapLocation.getStreet() + "" + amapLocation.getStreetNum());
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    L.i("定位地址：" + buffer.toString());
                    // 记录当前定位的坐标
                    locLatLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    isFirstLoc = false;
                }
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }

    // 自定义一个图钉，并且设置图标，当我们点击图钉时，显示设置的信息
    private MarkerOptions getMarkerOptions(AMapLocation amapLocation) {
        //设置图钉选项
        MarkerOptions options = new MarkerOptions();
        //图标
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_company));
        // 记录公司的坐标，这里的公司坐标是随机生成
        comLatLng = new LatLng(amapLocation.getLatitude() + ((new Random().nextDouble()) / 500),
                amapLocation.getLongitude() + ((new Random().nextDouble()) / -500));
        options.position(comLatLng);
        // 绘制圆圈
        drawCircle(comLatLng);
        StringBuffer buffer = new StringBuffer();
        buffer.append(amapLocation.getAddress());
        //标题
//        options.title(buffer.toString());
        options.title("公司：酷公司，用钉钉");
        //子标题
        options.snippet("地址：" + amapLocation.getProvince() + amapLocation.getCity() +
                amapLocation.getDistrict() + amapLocation.getStreet());
        //设置多少帧刷新一次图片资源
        options.period(60);

        return options;

    }

    /**
     * 绘制圆圈
     *
     * @param latLng
     */
    public void drawCircle(LatLng latLng) {
        if (circle != null) {
            circle = null;
        }
        circle = aMap.addCircle(new CircleOptions()
                .center(latLng).radius(radius)
                .fillColor(R.color.transparent1).strokeColor(R.color.transparent1)
                .strokeWidth(5));
    }

    //激活定位
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;

    }

    //停止定位
    @Override
    public void deactivate() {
        mListener = null;
    }


    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    /**
     * Android 6.0 以上的版本的定位方法
     */
    public void showPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE
            }, WRITE_COARSE_LOCATION_REQUEST_CODE);
        } else {
            // 开始定位
            initLoc();
        }
    }

    // Android 6.0 以上的版本申请权限的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case WRITE_COARSE_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 开始定位
                    initLoc();
                } else {
                    // 没有获取到权限，做特殊处理
                    ToastUtil.showToastLong("获取位置权限失败，请手动开启");
                }
                break;
            default:
                break;
        }
    }

    class TimeThread extends Thread {
        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1;  // 消息(一个整型值)
                    mHandler.sendMessage(msg); // 每隔1秒发送一个msg给mHandler
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    // 在主线程里面处理消息并更新UI界面
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    long sysTime = System.currentTimeMillis(); // 获取系统时间
                    CharSequence sysTimeStr = DateFormat.format("HH:mm:ss", sysTime); // 时间显示格式
                    btn_check_in.setText(String.format("上/下班打卡\n%s", sysTimeStr)); // 实时更新时间
                    break;
                default:
                    break;
            }
            return false;
        }
    });

}