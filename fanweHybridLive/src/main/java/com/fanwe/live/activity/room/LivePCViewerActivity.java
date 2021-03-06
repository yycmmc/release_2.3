package com.fanwe.live.activity.room;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.fanwe.hybrid.event.EUnLogin;
import com.fanwe.library.adapter.SDPagerAdapter;
import com.fanwe.library.adapter.http.handler.SDRequestHandler;
import com.fanwe.library.common.SDHandlerManager;
import com.fanwe.library.common.SDSelectManager;
import com.fanwe.library.customview.SDViewPager;
import com.fanwe.library.dialog.SDDialogConfirm;
import com.fanwe.library.dialog.SDDialogCustom;
import com.fanwe.library.listener.SDVisibilityStateListener;
import com.fanwe.library.utils.SDResourcesUtil;
import com.fanwe.library.utils.SDToast;
import com.fanwe.library.utils.SDViewUtil;
import com.fanwe.library.view.SDTabUnderline;
import com.fanwe.library.view.select.SDSelectViewManager;
import com.fanwe.live.LiveInformation;
import com.fanwe.live.R;
import com.fanwe.live.appview.LiveVideoView;
import com.fanwe.live.appview.room.ARoomPCBaseControlView;
import com.fanwe.live.appview.room.RoomContributionView;
import com.fanwe.live.appview.room.RoomPCLiveView;
import com.fanwe.live.appview.room.RoomPCMessageView;
import com.fanwe.live.appview.room.RoomSendGiftView;
import com.fanwe.live.appview.room.RoomView;
import com.fanwe.live.dao.UserModelDao;
import com.fanwe.live.event.EImOnForceOffline;
import com.fanwe.live.event.EOnCallStateChanged;
import com.fanwe.live.event.EScrollChangeRoomComplete;
import com.fanwe.live.model.App_get_videoActModel;
import com.fanwe.live.model.UserModel;
import com.fanwe.live.model.custommsg.CustomMsgEndVideo;
import com.fanwe.live.model.custommsg.CustomMsgStopLive;
import com.fanwe.live.model.custommsg.CustomMsgViewerJoin;
import com.sunday.eventbus.SDEventManager;
import com.ta.util.netstate.TANetWorkUtil;
import com.tencent.TIMCallBack;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import java.util.ArrayList;
import java.util.List;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * 推流直播间观众界面 ---PC端直播
 */
public class LivePCViewerActivity extends LivePCPlayActivity implements ARoomPCBaseControlView.PCControlViewClickListener,
        RoomPCMessageView.RoomMessageViewListener{

    private View mViewPortrait;//竖屏view

    private View mViewLandscape;//横屏view

    private LiveVideoView mViewVideo;

    private SDTabUnderline tab_room_chat;//聊天页面

    private SDTabUnderline tab_room_con;//贡献榜页面

    private SDTabUnderline tab_room_live;//直播列表页面

    private SDViewPager vpg_content;//fragment页面容器

    private RelativeLayout view_touch_scroll;

    private FrameLayout fl_container_gift;

    private FrameLayout fl_container_video;

    private FrameLayout.LayoutParams mParamsVideo;

    private RoomSendGiftView mViewGift;

    private DanmakuView danmaku_view;

    private DanmakuCallBack mCallBackDanmaku;

//    private int mCurrentOrientation = Configuration.ORIENTATION_PORTRAIT;

    private SDSelectViewManager<SDTabUnderline> selectViewManager = new SDSelectViewManager<>();

    private static final long DELAY_JOIN_ROOM = 1000;

    private RoomTabAdapter mAdapterTab;

    private ViewPager.OnPageChangeListener mListenerPageChange;

    private SDTabUnderline[] items = new SDTabUnderline[3];

    private ARoomPCBaseControlView mViewControl;

    private boolean showDanmaku;

    private DanmakuContext mContextDanmaku;

    /**
     * 直播的类型，仅用于观众时候需要传入0-热门;1-最新;2-关注(int)
     */
    public static final String EXTRA_LIVE_TYPE = "extra_live_type";
    /**
     * 私密直播的key(String)
     */
    public static final String EXTRA_PRIVATE_KEY = "extra_private_key";
    /**
     * 性别0-全部，1-男，2-女(int)
     */
    public static final String EXTRA_SEX = "extra_sex";
    /**
     * 话题id(int)
     */
    public static final String EXTRA_CATE_ID = "extra_cate_id";
    /**
     * 城市(String)
     */
    public static final String EXTRA_CITY = "extra_city";

    /**
     * 直播的类型0-热门;1-最新
     */
    protected int type;
    /**
     * 私密直播的key
     */
    protected String strPrivateKey;
    /**
     * 性别0-全部，1-男，2-女
     */
    protected int sex;
    /**
     * 话题id
     */
    protected int cate_id;
    /**
     * 城市
     */
    protected String city;

    @Override
    protected int onCreateContentView() {
        return R.layout.act_live_pc_viewer_port;
    }

    @Override
    public void setContentView(View view) {
        if(isOrientationPortrait()) {
            mViewPortrait = view;
        } else {
            mViewLandscape = view;
        }
        super.setContentView(view);
    }

    @Override
    protected void init(Bundle savedInstanceState)
    {
        super.init(savedInstanceState);
        type = getIntent().getIntExtra(EXTRA_LIVE_TYPE, 0);
        strPrivateKey = getIntent().getStringExtra(EXTRA_PRIVATE_KEY);
        sex = getIntent().getIntExtra(EXTRA_SEX, 0);
        city = getIntent().getStringExtra(EXTRA_CITY);
        if (validateParams(getRoomId(), getGroupId(), getCreaterId()))
        {
            requestRoomInfo();
        }
        mCallBackDanmaku = new DanmakuCallBack();
        initCommonView();
        initSDViewPager();
        initTabs();
    }

    /**
     * 横竖屏切换需要调用该方法重新实例化各组件
     */
    private void initCommonView() {
        if(mViewVideo == null) {
            initVideoView();
            initVideoParams();
        }
        fl_container_video = find(R.id.fl_container_video);
        fl_container_video.addView(mViewVideo, mParamsVideo);
//        setVideoView((LiveVideoView) find(R.id.view_video));
//        getPlayer().setRenderModeAdjustResolution();
        startPlayVideo();
        view_touch_scroll = find(R.id.view_touch_scroll);
        danmaku_view = find(R.id.danmaku_view);
        initOrientationView();
        initDanmakuView();
    }

    private void initVideoView() {
        mViewVideo = new LiveVideoView(this);
        setVideoView(mViewVideo);
        getPlayer().setRenderModeAdjustResolution();
    }

    private void initVideoParams() {
        mParamsVideo = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void startPlayVideo() {
        if(!getPlayer().isPlayerStarted()) {
            getPlayer().startPlay();
        } else {
            getPlayer().resume();
        }
    }

    private void initOrientationView() {
        if(isOrientationPortrait()) {
            setPlayerSize();
            initPortraitView();
        } else {
            initLandscapeView();
        }
    }

    private void setPlayerSize() {
        SDViewUtil.setViewHeight(view_touch_scroll,SDViewUtil.getScreenHeight()/3);
    }

    private void initDanmakuView() {
        danmaku_view.enableDanmakuDrawingCache(true);
        danmaku_view.setCallback(mCallBackDanmaku);
        mContextDanmaku = DanmakuContext.create();
        danmaku_view.prepare(parser, mContextDanmaku);
        danmaku_view.clear();
        danmaku_view.removeAllDanmakus(true);
        danmaku_view.resume();
    }

    private BaseDanmakuParser parser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };

    @Override
    public void onClickSwitchScreenOrientation(View view) {
        switchScreenOrientation();
    }

    @Override
    public void onClickPause(View view) {
        addDanmaku(System.currentTimeMillis()+"",false);
    }

    @Override
    public void onClickClose(View view) {
        exitRoom(true);
    }

    @Override
    public void onClickRefresh(View view) {
        //刷新，重载视频
    }

    @Override
    public void onClickLives(View view) {
        //房间列表
        //首先隐藏控制视图
        //右边滑出房间列表
        addDanmaku(System.currentTimeMillis()+"",true);
    }

    @Override
    public void onClickLock(View view) {
        //锁定控制层
        addDanmaku(System.currentTimeMillis()+"",false);
    }

    @Override
    public void onClickGift(View view) {
        //横屏显示礼物框
        addDanmaku(System.currentTimeMillis()+"",false);
    }

    @Override
    public void onClickDanmakuSwitch(View view) {
        //弹幕开关
        addDanmaku(System.currentTimeMillis()+"",false);
    }

    @Override
    public void showGiftViewPort(View view) {
        //竖屏显示礼物框
        SDViewUtil.hideInputMethod(view);
        addSendGiftView();
        mViewGift.show(true);
    }

    private void addSendGiftView() {
        if(mViewGift == null) {
            mViewGift = new RoomSendGiftView(this);
            mViewGift.addVisibilityStateListener(new SDVisibilityStateListener() {

                @Override
                public void onVisible(View view) {

                }

                @Override
                public void onGone(View view) {
                    SDViewUtil.removeViewFromParent(mViewGift);
                }

                @Override
                public void onInvisible(View view) {
                    SDViewUtil.removeViewFromParent(mViewGift);
                }
            });
        }
        mViewGift.requestData();
        mViewGift.bindUserData();
        replaceView(fl_container_gift, mViewGift);
    }

    @Override
    protected void onHideSendGiftView(View view) {
        super.onHideSendGiftView(view);
        if (mViewGift != null)
            mViewGift.invisible();
    }

    @Override
    protected boolean isSendGiftViewVisible() {
        return mViewGift != null && mViewGift.isVisible();
    }

    private class DanmakuCallBack implements DrawHandler.Callback {

        @Override
        public void prepared() {
            showDanmaku = true;
            danmaku_view.start();
        }

        @Override
        public void updateTimer(DanmakuTimer timer) {

        }

        @Override
        public void danmakuShown(BaseDanmaku danmaku) {

        }

        @Override
        public void drawingFinished() {

        }
    }
    /**
     * 向弹幕View中添加一条弹幕
     * @param content
     *          弹幕的具体内容
     * @param  withBorder
     *          弹幕是否有边框
     */
    private void addDanmaku(String content, boolean withBorder) {
        BaseDanmaku danmaku = mContextDanmaku.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL, mContextDanmaku);
        danmaku.text = content;
        danmaku.padding = 5;
        danmaku.textSize = 30;
        danmaku.textColor = Color.BLUE;
        danmaku.setTime(danmaku_view.getCurrentTime());
        if (withBorder) {
            danmaku.borderColor = Color.CYAN;
        }
        danmaku_view.addDanmaku(danmaku);
    }

    private void initPortraitView() {
        fl_container_gift = find(R.id.fl_container_gift);
        tab_room_chat = find(R.id.tab_room_chat);
        tab_room_con = find(R.id.tab_room_con);
        tab_room_live = find(R.id.tab_room_live);
        vpg_content = find(R.id.vpg_content);
        mViewControl = find(R.id.view_control_port);
        mViewControl.setPCControlViewClickListener(this);
        mViewControl.showControlView();
    }

    private void initLandscapeView() {
        mViewControl = find(R.id.view_control_land);
        mViewControl.setPCControlViewClickListener(this);
        mViewControl.showControlView();
    }

    private void switchScreenOrientation() {
        if(isOrientationLandscape())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if(isOrientationPortrait())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getPlayer().pause();
        fl_container_video.removeAllViews();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch(v.getId()) {
            case R.id.iv_pc_close :
                //关闭直播间
                exitRoom(true);
                break;
            case R.id.iv_pc_switch_fullscreen :
                switchScreenOrientation();
                break;
            case R.id.iv_pc_return :
                switchScreenOrientation();
                break;
            case R.id.iv_pc_pause:
                addDanmaku(System.currentTimeMillis()+"",false);
                break;
            default:
                break;
        }
    }

    private void initSDViewPager() {
        vpg_content.setOffscreenPageLimit(2);
        List<String> listModel = new ArrayList<>();
        listModel.add("");
        listModel.add("");
        listModel.add("");

        if(mListenerPageChange == null) {
            mListenerPageChange = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (selectViewManager.getSelectedIndex() != position) {
                        selectViewManager.setSelected(position, true);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            };
        }
        mAdapterTab = new RoomTabAdapter(listModel, this);
        vpg_content.addOnPageChangeListener(mListenerPageChange);
        vpg_content.setAdapter(mAdapterTab);
    }

    private void initTabs() {
        // 聊天
        String room_pc_tab_chat_text = getString(R.string.room_pc_tab_chat_text);
        tab_room_chat.setTextTitle(room_pc_tab_chat_text);
        Log.i("test",tab_room_chat.toString());
        tab_room_chat.getViewConfig(tab_room_chat.mIvUnderline).setBackgroundColorNormal(Color.TRANSPARENT)
                .setBackgroundColorSelected(SDResourcesUtil.getColor(R.color.main_color));
        tab_room_chat.getViewConfig(tab_room_chat.mTvTitle).setTextColorNormal(SDResourcesUtil.getColor(R.color.text_black)).setTextColorSelected(SDResourcesUtil.getColor(R.color.main_color));

        // 贡献榜
        String room_pc_tab_con_text = getString(R.string.room_pc_tab_con_text);
        tab_room_con.setTextTitle(room_pc_tab_con_text);
        tab_room_con.getViewConfig(tab_room_con.mIvUnderline).setBackgroundColorNormal(Color.TRANSPARENT).setBackgroundColorSelected(SDResourcesUtil.getColor(R.color.main_color));
        tab_room_con.getViewConfig(tab_room_con.mTvTitle).setTextColorNormal(SDResourcesUtil.getColor(R.color.text_black)).setTextColorSelected(SDResourcesUtil.getColor(R.color.main_color));

        // 直播
        String room_pc_tab_live_text = getString(R.string.room_pc_tab_live_text);
        tab_room_live.setTextTitle(room_pc_tab_live_text);
        tab_room_live.getViewConfig(tab_room_live.mIvUnderline).setBackgroundColorNormal(Color.TRANSPARENT).setBackgroundColorSelected(SDResourcesUtil.getColor(R.color.main_color));
        tab_room_live.getViewConfig(tab_room_live.mTvTitle).setTextColorNormal(SDResourcesUtil.getColor(R.color.text_black)).setTextColorSelected(SDResourcesUtil.getColor(R.color.main_color));

        items[0] = tab_room_chat;
        items[1] = tab_room_con;
        items[2] = tab_room_live;

        selectViewManager.setListener(new SDSelectManager.SDSelectManagerListener<SDTabUnderline>() {

            @Override
            public void onNormal(int index, SDTabUnderline item) {
            }

            @Override
            public void onSelected(int index, SDTabUnderline item) {
                switch (index) {
                    case 0:
                        vpg_content.setCurrentItem(0);
                        break;
                    case 1:
                        vpg_content.setCurrentItem(1);
                        break;
                    case 2:
                        vpg_content.setCurrentItem(2);
                        break;
                    default:
                        break;
                }
            }
        });
        selectViewManager.setItems(items);
        selectViewManager.setSelected(0, true);//默认选中聊天页面
    }

    private class RoomTabAdapter extends SDPagerAdapter<String> {

        RoomView view = null;

        RoomTabAdapter(List<String> listModel, Activity activity) {
            super(listModel, activity);
        }

        @Override
        public View getView(ViewGroup container, int position) {
            switch(position) {
                case 0:
                    view = getRoomPCMessageView();
                    break;
                case 1:
                    view = new RoomContributionView(getActivity());
                    break;
                case 2:
                    view = new RoomPCLiveView(getActivity());
                    break;
                default:
                    break;
            }
            return view;
        }
    }

    private RoomPCMessageView getRoomPCMessageView() {
        return new RoomPCMessageView(this, this);
    }

    @Override
    public void onChangeRoomComplete() {
        super.onChangeRoomComplete();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(isOrientationPortrait()) {
            if(mViewPortrait == null) {
                setContentView(R.layout.act_live_pc_viewer_port);
            } else {
                setContentView(mViewPortrait);
            }
            setNotFullScreen();
        } else {

            if(mViewLandscape == null) {
                setContentView(R.layout.act_live_pc_viewer_land);
            } else {
                setContentView(mViewLandscape);
            }
            setFullScreen();
            mViewLandscape.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        initCommonView();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if (intent != null)
        {
            int oldRoomId = getRoomId();
            int newRoomId = intent.getIntExtra(EXTRA_ROOM_ID, 0);
            if (newRoomId != oldRoomId)
            {
                setIntent(intent);
                exitRoom(false);
                init(null);
            } else
            {
                SDToast.showToast("已经在直播间中");
            }
        }
        super.onNewIntent(intent);
    }

    protected boolean validateParams(int roomId, String groupId, String createrId)
    {
        if (roomId <= 0)
        {
            SDToast.showToast("房间id为空");
            finish();
            return false;
        }

        if (isEmpty(groupId))
        {
            SDToast.showToast("聊天室id为空");
            finish();
            return false;
        }

        if (isEmpty(createrId))
        {
            SDToast.showToast("主播id为空");
            finish();
            return false;
        }
        setRoomId(roomId);
        setGroupId(groupId);
        setCreaterId(createrId);

        return true;
    }

    @Override
    protected void initIM()
    {
        super.initIM();

        final String groupId = getGroupId();
        getViewerIM().joinGroup(groupId, new TIMCallBack()
        {
            @Override
            public void onError(int code, String desc)
            {
                onErrorJoinGroup(code, desc);
            }

            @Override
            public void onSuccess()
            {
                LiveInformation.getInstance().enterRoom(getRoomId(), getGroupId(), getCreaterId());
                onSuccessJoinGroup(groupId);
            }
        });
    }

    /**
     * 加入聊天组失败
     */
    public void onErrorJoinGroup(int code, String desc)
    {
        showJoinGroupErrorDialog(code, desc);
    }

    protected void showJoinGroupErrorDialog(int code, String desc)
    {
        SDDialogConfirm dialog = new SDDialogConfirm(this);
        dialog.setTextContent("加入聊天组失败，是否重试");
        dialog.setCancelable(false);
        dialog.setmListener(new SDDialogCustom.SDDialogCustomListener()
        {

            @Override
            public void onDismiss(SDDialogCustom dialog)
            {
            }

            @Override
            public void onClickConfirm(View v, SDDialogCustom dialog)
            {
                initIM();
            }

            @Override
            public void onClickCancel(View v, SDDialogCustom dialog)
            {
                exitRoom(true);
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(danmaku_view != null && danmaku_view.isPrepared() && danmaku_view.isPaused()) {
            danmaku_view.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(danmaku_view != null && danmaku_view.isPrepared()) {
            danmaku_view.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        showDanmaku = false;
        if (danmaku_view != null) {
            danmaku_view.release();
            danmaku_view = null;
        }
    }

    @Override
    protected void onShowSendGiftView(View view) {
        super.onShowSendGiftView(view);

    }

    @Override
    protected void onSuccessJoinGroup(String groupId)
    {
        super.onSuccessJoinGroup(groupId);
        sendViewerJoinMsg();

//        setCanBindShowShareView(true);
        if (false)
        {
            onChangeRoomComplete();
            EScrollChangeRoomComplete eventChangeRoomComplete = new EScrollChangeRoomComplete();
            SDEventManager.post(eventChangeRoomComplete);
        }
    }

    /**
     * 发送观众加入消息
     */
    public void sendViewerJoinMsg() {
        if (!getViewerIM().isCanSendViewerJoinMsg())
        {
            return;
        }
        App_get_videoActModel actModel = getRoomInfo();
        if (actModel == null)
        {
            return;
        }
        UserModel user = UserModelDao.query();
        if (user == null)
        {
            return;
        }

        boolean sendViewerJoinMsg = true;
        if (!user.isProUser() && actModel.getJoin_room_prompt() == 0)
        {
            sendViewerJoinMsg = false;
        }

        if (sendViewerJoinMsg)
        {
            CustomMsgViewerJoin joinMsg = new CustomMsgViewerJoin();
            joinMsg.setSortNumber(actModel.getSort_num());

            getViewerIM().sendViewerJoinMsg(joinMsg, null);
        }
    }

    @Override
    public void onMsgEndVideo(CustomMsgEndVideo msg)
    {
        super.onMsgEndVideo(msg);
        exitRoom(false);
    }

    @Override
    public void onMsgStopLive(CustomMsgStopLive msg)
    {
        super.onMsgStopLive(msg);
        SDToast.showToast(msg.getDesc());
        exitRoom(true);
    }

    /**
     * 退出房间
     *
     * @param isNeedFinish
     */
    protected void exitRoom(boolean isNeedFinish)
    {
//        setCanBindShowShareView(false);
        destroyIM();
        getPlayer().stopPlay();
//        if (isNeedFinish)
//        {
//            addLiveFinish();
//        }
//
        if (isNeedFinish)
        {
            finish();
        }
    }

    /**
     * 观众付费模式不进去的时候退出
     */
//    @Override
//    protected void onExitRoom()
//    {
//        super.onExitRoom();
//        exitRoom(true);
//    }

    @Override
    protected void destroyIM()
    {
        super.destroyIM();
        getViewerIM().destroyIM();
        LiveInformation.getInstance().exitRoom();
    }

    @Override
    public void onChangeRoomActionComplete()
    {
        super.onChangeRoomActionComplete();
        requestRoomInfo();
        exitRoom(false);
    }

    @Override
    protected SDRequestHandler requestRoomInfo() {

        return getLiveBusiness().requestRoomInfo(getRoomId(), 0, type, 1, sex, cate_id, city, strPrivateKey);
    }

    @Override
    public void onLiveRequestRoomInfoSuccess(App_get_videoActModel actModel)
    {
        int rId = actModel.getRoom_id();
        String gId = actModel.getGroup_id();
        String cId = actModel.getUser_id();

        if (!validateParams(rId, gId, cId))
        {
            return;
        }

        super.onLiveRequestRoomInfoSuccess(actModel);

        getViewerBusiness().startJoinRoom(false);
    }

    @Override
    public void onLiveRequestRoomInfoError(App_get_videoActModel actModel)
    {
        super.onLiveRequestRoomInfoError(actModel);
        if (actModel.isVideoStoped())
        {
            addLiveFinish();
        } else
        {
            exitRoom(true);
        }
    }

    @Override
    public void onLiveRequestRoomInfoException(String msg)
    {
        super.onLiveRequestRoomInfoException(msg);

        SDDialogConfirm dialog = new SDDialogConfirm(this);
        dialog.setCancelable(false);
        dialog.setTextContent("请求直播间信息失败")
                .setTextCancel("退出").setTextConfirm("重试")
                .setmListener(new SDDialogCustom.SDDialogCustomListener()
                {
                    @Override
                    public void onClickCancel(View v, SDDialogCustom dialog)
                    {
                        exitRoom(true);
                    }

                    @Override
                    public void onClickConfirm(View v, SDDialogCustom dialog)
                    {
                        requestRoomInfo();
                    }

                    @Override
                    public void onDismiss(SDDialogCustom dialog)
                    {
                    }
                }).show();
    }

    @Override
    public void onLiveViewerStartJoinRoom(boolean delay)
    {
        super.onLiveViewerStartJoinRoom(delay);
        startJoinRoomRunnable(delay);
    }

    private void startJoinRoomRunnable(boolean delay)
    {
        if (delay)
        {
//            isInDelayJoinRoom = true;
            SDHandlerManager.getMainHandler().removeCallbacks(joinRoomRunnable);
            SDHandlerManager.getMainHandler().postDelayed(joinRoomRunnable, DELAY_JOIN_ROOM);
        } else
        {
            joinRoomRunnable.run();
        }
    }

    /**
     * 加入房间runnable
     */
    private Runnable joinRoomRunnable = new Runnable()
    {

        @Override
        public void run()
        {
//            isInDelayJoinRoom = false;
            initIM();

            if (getRoomInfo() != null)
            {
                String url = getRoomInfo().getPlay_url();
                playUrl(url);
            }
        }
    };

    private boolean restartPlay()
    {
        if (!TANetWorkUtil.isNetworkAvailable(LivePCViewerActivity.this))
        {
            SDToast.showToast("网络不可用");
            return false;
        }

        if (getRoomInfo() == null)
        {
            return false;
        }

        getPlayer().stopPlay();
        playUrl(getRoomInfo().getPlay_url());

        return true;
    }

    protected void playUrl(String playUrl)
    {
        if (validatePlayUrl(playUrl))
        {
            getPlayer().setUrl(playUrl);
            getPlayer().startPlay();
        }
    }

    @Override
    public void onPlayEvent(int event, Bundle param)
    {
        super.onPlayEvent(event, param);
        if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT)
        {
//            showNetDisconnectDialog();
        }
    }

    private SDDialogConfirm netDisconnectDialog;

    private void showNetDisconnectDialog()
    {
        if (netDisconnectDialog == null)
        {
            netDisconnectDialog = new SDDialogConfirm(this);
            netDisconnectDialog.setCancelable(false);
            netDisconnectDialog.setDismissAfterClick(false);
            netDisconnectDialog.setTextContent("已经与服务器断开")
                    .setTextCancel("退出")
                    .setTextConfirm("重新连接")
                    .setmListener(new SDDialogCustom.SDDialogCustomListener()
                    {
                        @Override
                        public void onClickCancel(View v, SDDialogCustom dialog)
                        {
                            dialog.dismiss();
                            exitRoom(true);
                        }

                        @Override
                        public void onClickConfirm(View v, SDDialogCustom dialog)
                        {
                            if (restartPlay())
                            {
                                dialog.dismiss();
                            }
                        }

                        @Override
                        public void onDismiss(SDDialogCustom dialog)
                        {

                        }
                    });
        }
        netDisconnectDialog.show();
    }

    private void dismissNetDisconnectDialog()
    {
        try
        {
            if (netDisconnectDialog != null)
            {
                netDisconnectDialog.dismiss();
            }
        } catch (Exception e)
        {
        }
    }

    @Override
    public void onPlayRecvFirstFrame(int event, Bundle param)
    {
        super.onPlayRecvFirstFrame(event, param);
//        hideLoadingVideo();
    }

    protected boolean validatePlayUrl(String playUrl)
    {
        if (TextUtils.isEmpty(playUrl))
        {
            SDToast.showToast("未找到直播地址");
            return false;
        }

        if (playUrl.startsWith("rtmp://"))
        {
            getPlayer().setPlayType(TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
        } else if ((playUrl.startsWith("http://") || playUrl.startsWith("https://")) && playUrl.endsWith(".flv"))
        {
            getPlayer().setPlayType(TXLivePlayer.PLAY_TYPE_LIVE_FLV);
        } else
        {
            SDToast.showToast("播放地址不合法");
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed()
    {
        if(isOrientationLandscape()) {
            switchScreenOrientation();
        } else
            showExitDialog();
    }

    @Override
    protected void onClickCloseRoom(View v)
    {
        exitRoom(true);
    }

    private void showExitDialog()
    {
        SDDialogConfirm dialog = new SDDialogConfirm(this);
        dialog.setTextContent("确定要退出吗？");
        dialog.setmListener(new SDDialogCustom.SDDialogCustomListener()
        {

            @Override
            public void onDismiss(SDDialogCustom dialog)
            {
            }

            @Override
            public void onClickConfirm(View v, SDDialogCustom dialog)
            {
                exitRoom(true);
            }

            @Override
            public void onClickCancel(View v, SDDialogCustom dialog)
            {
            }
        });
        dialog.show();
    }

    public void onEventMainThread(EUnLogin event)
    {
        exitRoom(true);
    }

    public void onEventMainThread(EImOnForceOffline event)
    {
        exitRoom(true);
    }

    public void onEventMainThread(EOnCallStateChanged event)
    {
        switch (event.state)
        {
            case TelephonyManager.CALL_STATE_RINGING:
                sdkEnableAudioDataVolume(false);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                sdkEnableAudioDataVolume(false);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                sdkEnableAudioDataVolume(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnect(TANetWorkUtil.netType type)
    {
        if (type == TANetWorkUtil.netType.mobile)
        {
            SDDialogConfirm dialog = new SDDialogConfirm(this);
            dialog.setTextContent("当前处于数据网络下，会耗费较多流量，是否继续？").setTextCancel("否").setTextConfirm("是").setmListener(new SDDialogCustom.SDDialogCustomListener()
            {

                @Override
                public void onDismiss(SDDialogCustom dialog)
                {
                }

                @Override
                public void onClickConfirm(View v, SDDialogCustom dialog)
                {
                }

                @Override
                public void onClickCancel(View v, SDDialogCustom dialog)
                {
                    exitRoom(true);
                }
            }).show();
        }
        super.onConnect(type);
    }

    @Override
    protected void sdkEnableAudioDataVolume(boolean enable)
    {
        getPlayer().setMute(!enable);
    }

}
