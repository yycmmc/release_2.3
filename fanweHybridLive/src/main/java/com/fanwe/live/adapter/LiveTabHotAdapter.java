package com.fanwe.live.adapter;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fanwe.library.adapter.SDViewHolderAdapter;
import com.fanwe.library.adapter.viewholder.SDViewHolder;
import com.fanwe.library.utils.SDViewBinder;
import com.fanwe.library.utils.SDViewUtil;
import com.fanwe.live.R;
import com.fanwe.live.activity.LiveTopicRoomActivity;
import com.fanwe.live.activity.LiveUserHomeActivity;
import com.fanwe.live.common.AppRuntimeWorker;
import com.fanwe.live.model.LiveRoomModel;
import com.fanwe.live.utils.GlideUtil;

import java.util.List;

public class LiveTabHotAdapter extends SDViewHolderAdapter<LiveRoomModel>
{

    public LiveTabHotAdapter(List<LiveRoomModel> listModel, Activity activity)
    {
        super(listModel, activity);
    }

    @Override
    public SDViewHolder<LiveRoomModel> onCreateViewHolder(int position, View convertView, ViewGroup parent)
    {
        return new ViewHolder();
    }

    @Override
    public void onBindData(int position, View convertView, ViewGroup parent, LiveRoomModel model, SDViewHolder<LiveRoomModel> holder)
    {

    }

    public static class ViewHolder extends SDViewHolder<LiveRoomModel>
    {

        ImageView iv_head;
        ImageView iv_head_small;
        TextView tv_nickname;
        TextView tv_city;
        TextView tv_watch_number;
        ImageView iv_room_image;
        TextView tv_topic;
        TextView tv_live_state;
        TextView tv_live_fee;

        RelativeLayout rl_gaming;

        @Override
        public int getLayoutId(int position, View convertView, ViewGroup parent)
        {
            return R.layout.item_live_tab_hot;
        }

        @Override
        public void findViews(int position, View convertView, ViewGroup parent)
        {
            iv_head = find(R.id.iv_head);
            iv_head_small = find(R.id.iv_head_small);
            tv_nickname = find(R.id.tv_nickname);
            tv_city = find(R.id.tv_city);
            tv_watch_number = find(R.id.tv_watch_number);
            iv_room_image = find(R.id.iv_room_image);
            tv_topic = find(R.id.tv_topic);
            tv_live_state = find(R.id.tv_live_state);
            tv_live_fee = find(R.id.tv_live_fee);
            rl_gaming = find(R.id.rl_gaming);
        }

        @Override
        public void onBindData(int position, View convertView, ViewGroup parent, final LiveRoomModel model)
        {
            GlideUtil.loadHeadImage(model.getHead_image()).into(iv_head);
            if (!TextUtils.isEmpty(model.getV_icon()))
            {
                SDViewUtil.show(iv_head_small);
                GlideUtil.load(model.getV_icon()).into(iv_head_small);
            } else
            {
                SDViewUtil.hide(iv_head_small);
            }
            SDViewBinder.setTextViewsVisibility(tv_live_state, model.getLiveState());
            SDViewBinder.setTextViewsVisibility(tv_live_fee, model.getLivePayFee());

            SDViewBinder.setTextView(tv_nickname, model.getNick_name());
            SDViewBinder.setTextView(tv_city, model.getCity());
            SDViewBinder.setTextView(tv_watch_number, model.getWatch_number());
            GlideUtil.load(model.getLive_image()).into(iv_room_image);

            if (model.getCate_id() > 0)
            {
                tv_topic.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(getActivity(), LiveTopicRoomActivity.class);
                        intent.putExtra(LiveTopicRoomActivity.EXTRA_TOPIC_ID, model.getCate_id());
                        intent.putExtra(LiveTopicRoomActivity.EXTRA_TOPIC_TITLE, model.getTitle());
                        getActivity().startActivity(intent);
                    }
                });
                SDViewBinder.setTextView(tv_topic, model.getTitle());
                SDViewUtil.show(tv_topic);
            } else
            {
                SDViewUtil.hide(tv_topic);
            }

            if (model.getIs_gaming() == 1)
            {
                SDViewUtil.show(rl_gaming);
            } else
            {
                SDViewUtil.hide(rl_gaming);
            }

            iv_head.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent intent = new Intent(getActivity(), LiveUserHomeActivity.class);
                    intent.putExtra(LiveUserHomeActivity.EXTRA_USER_ID, model.getUser_id());
                    intent.putExtra(LiveUserHomeActivity.EXTRA_USER_IMG_URL, model.getHead_image());
                    getActivity().startActivity(intent);
                }
            });
            iv_room_image.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    AppRuntimeWorker.joinRoom(model, getActivity());
                }
            });
        }
    }
}
