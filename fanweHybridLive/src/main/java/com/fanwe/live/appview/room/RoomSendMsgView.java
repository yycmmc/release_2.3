package com.fanwe.live.appview.room;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.fanwe.hybrid.dao.InitActModelDao;
import com.fanwe.hybrid.http.AppHttpUtil;
import com.fanwe.hybrid.http.AppRequestCallback;
import com.fanwe.hybrid.http.AppRequestParams;
import com.fanwe.hybrid.model.InitActModel;
import com.fanwe.library.adapter.http.model.SDResponse;
import com.fanwe.library.utils.SDObjectBlocker;
import com.fanwe.library.utils.SDResourcesUtil;
import com.fanwe.library.utils.SDToast;
import com.fanwe.library.utils.SDViewUtil;
import com.fanwe.live.IMHelper;
import com.fanwe.live.R;
import com.fanwe.live.common.CommonInterface;
import com.fanwe.live.dao.UserModelDao;
import com.fanwe.live.model.App_pop_msgActModel;
import com.fanwe.live.model.UserModel;
import com.fanwe.live.model.custommsg.CustomMsgText;
import com.fanwe.live.view.SDSlidingButton;
import com.fanwe.live.view.SDSlidingButton.SelectedChangeListener;
import com.ta.util.netstate.TANetWorkUtil;
import com.tencent.TIMMessage;
import com.tencent.TIMValueCallBack;

public class RoomSendMsgView extends RoomView
{

    public RoomSendMsgView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    public RoomSendMsgView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public RoomSendMsgView(Context context)
    {
        super(context);
        init();
    }

    private static final int MAX_INPUT_LENGTH = 38;

    /**
     * 相同消息拦截间隔
     */
    private static final int DUR_BLOCK_SAME_MSG = 5 * 1000;
    /**
     * 消息拦截间隔
     */
    private static final int DUR_BLOCK_MSG = 2 * 1000;

    private SDSlidingButton sl_btn;
    private ImageView iv_pop_msg_handle;
    private EditText et_content;
    private TextView tv_send;

    private int popMsgDiamonds = 1;
    private String strContent;
    private boolean isPopMsg = false;

    private SDObjectBlocker sendBlocker;

    @Override
    protected int onCreateContentView()
    {
        return R.layout.frag_live_send_msg;
    }

    @Override
    protected void init()
    {
        super.init();
        sl_btn = find(R.id.sl_btn);
        iv_pop_msg_handle = find(R.id.iv_pop_msg_handle);
        et_content = find(R.id.et_content);
        tv_send = find(R.id.tv_send);

        sendBlocker = new SDObjectBlocker();
        sendBlocker.setMaxEqualsCount(1);
        sendBlocker.setBlockObjectDuration(DUR_BLOCK_SAME_MSG);
        sendBlocker.setBlockDuration(DUR_BLOCK_MSG);

        invisible();
        register();
    }

    private void register()
    {
        InitActModel model = InitActModelDao.query();
        if (model != null)
        {
            popMsgDiamonds = model.getBullet_screen_diamond();
        }

        initSDSlidingButton();

        et_content.setOnKeyListener(new OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_ENTER)
                {
                    return true;
                }
                return false;
            }
        });

        tv_send.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (validateContent())
                {
                    sendMessage();
                }
            }
        });
    }

    private void initSDSlidingButton()
    {
        sl_btn.setSelectedChangeListener(new SelectedChangeListener()
        {
            @Override
            public void onSelectedChange(SDSlidingButton view, boolean selected)
            {
                if (selected)
                {
                    iv_pop_msg_handle.setImageResource(R.drawable.bg_live_pop_msg_handle_selected);
                    if (getLiveInfo().isCreater())
                    {
                        et_content.setHint(SDResourcesUtil.getString(R.string.live_send_msg_hint_normal));
                    } else
                    {
                        et_content.setHint("开启大喇叭，" + popMsgDiamonds + "钻石/条");
                    }
                    isPopMsg = true;
                } else
                {
                    iv_pop_msg_handle.setImageResource(R.drawable.bg_live_pop_msg_handle_normal);
                    et_content.setHint(SDResourcesUtil.getString(R.string.live_send_msg_hint_normal));
                    isPopMsg = false;
                }
            }
        });
    }

    public void setContent(String content)
    {
        if (content == null)
        {
            content = "";
        }
        et_content.setText(content);
        et_content.setSelection(et_content.getText().length());
        SDViewUtil.showInputMethod(et_content, 100);
    }

    private boolean validateContent()
    {
        if (!TANetWorkUtil.isNetworkAvailable(getActivity()))
        {
            SDToast.showToast("无网络");
            return false;
        }

        strContent = et_content.getText().toString().trim();
        if (TextUtils.isEmpty(strContent))
        {
            SDToast.showToast("请输入内容");
            return false;
        }

        if (strContent.contains("\n"))
        {
            strContent = strContent.replace("\n", "");
        }

        if (strContent.length() > MAX_INPUT_LENGTH)
        {
            strContent = strContent.substring(0, MAX_INPUT_LENGTH);
        }

        if (!getLiveInfo().isCreater())
        {
            if (sendBlocker.block())
            {
                SDToast.showToast("发送太频繁");
                return false;
            }
            if (sendBlocker.blockObject(strContent))
            {
                SDToast.showToast("请勿刷屏");
                return false;
            }
        }

        return true;
    }

    protected void sendMessage()
    {
        if (isPopMsg)
        {
            AppRequestParams params = CommonInterface.requestPopMsgParams(getLiveInfo().getRoomId(), strContent);
            AppHttpUtil.getInstance().post(params, new AppRequestCallback<App_pop_msgActModel>()
            {
                @Override
                protected void onSuccess(SDResponse resp)
                {
                    if (actModel.isOk())
                    {
                        // 扣费
                        UserModel user = UserModelDao.query();
                        if (user != null)
                        {
                            user.payDiamonds(popMsgDiamonds);
                            UserModelDao.insertOrUpdate(user);
                        }
                    } else
                    {
                        CommonInterface.requestMyUserInfo(null);
                    }
                }

                @Override
                protected void onError(SDResponse resp)
                {
                    CommonInterface.requestMyUserInfo(null);
                }
            });
        } else
        {
            String groupId = getLiveInfo().getGroupId();
            if (TextUtils.isEmpty(groupId))
            {
                return;
            }

            sendBlocker.saveLastTime();

            final CustomMsgText msg = new CustomMsgText();
            msg.setText(strContent);
            IMHelper.sendMsgGroup(groupId, msg, new TIMValueCallBack<TIMMessage>()
            {

                @Override
                public void onSuccess(TIMMessage timMessage)
                {
                    IMHelper.postMsgLocal(msg, timMessage.getConversation().getPeer());
                }

                @Override
                public void onError(int code, String desc)
                {
                    if (code == 80001)
                    {
                        SDToast.showToast("该词已被禁用");
                    }
                }
            });
        }
        et_content.setText("");
    }

    @Override
    protected boolean onTouchDownOutside(MotionEvent ev)
    {
        invisible();
        return true;
    }

    @Override
    public boolean onBackPressed()
    {
        invisible();
        return true;
    }

    @Override
    public void onInvisible(View view)
    {
        SDViewUtil.hideInputMethod(et_content);
        super.onInvisible(view);
    }

    @Override
    public void onGone(View view)
    {
        SDViewUtil.hideInputMethod(et_content);
        super.onGone(view);
    }

    @Override
    public void onVisible(View view)
    {
        SDViewUtil.showInputMethod(et_content);
        super.onVisible(view);
    }
}