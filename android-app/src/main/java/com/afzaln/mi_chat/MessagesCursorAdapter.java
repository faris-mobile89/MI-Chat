package com.afzaln.mi_chat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.afzaln.mi_chat.R.color;
import com.afzaln.mi_chat.activity.ImageActivity;
import com.afzaln.mi_chat.provider.ProviderContract.MessagesTable;
import com.afzaln.mi_chat.resource.Message;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.apache.commons.lang3.StringUtils;

public class MessagesCursorAdapter extends CursorAdapter {

    private static final String TAG = MessagesCursorAdapter.class.getSimpleName();

    private static final int MOD_USER_ROLE = 2;
    private static final int ADMIN_USER_ROLE = 3;

    private final int mAdminNameColor;
    private final int mModNameColor;
    private final int mUserameColor;

    public MessagesCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mAdminNameColor = context.getResources().getColor(color.admin_name);
        mModNameColor = context.getResources().getColor(color.mod_name);
        mUserameColor = context.getResources().getColor(color.normal_text);

    }

    private int getItemViewType(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(MessagesTable.TYPE));
    }

    public long getItemDateTime(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return cursor.getLong(cursor.getColumnIndex(MessagesTable.DATETIME));
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View listItemView = null;

        switch (getItemViewType(cursor)) {
            case Message.NORMAL_TYPE:
                listItemView = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
                break;
            case Message.ACTION_TYPE:
                listItemView = LayoutInflater.from(context).inflate(R.layout.item_action, parent, false);
                break;
            case Message.ERROR_TYPE:
                listItemView = LayoutInflater.from(context).inflate(R.layout.item_action, parent, false);
                break;
        }

        ViewHolder holder = new ViewHolder(listItemView);
        listItemView.setTag(holder);
        return listItemView;
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        // Get username, timestamp and message from cursor
        String userName = cursor.getString(cursor.getColumnIndex(MessagesTable.USERNAME));
        int userRole = cursor.getInt(cursor.getColumnIndex(MessagesTable.USERROLE));
        String message = cursor.getString(cursor.getColumnIndex(MessagesTable.MESSAGE));
        String imgLinks = cursor.getString(cursor.getColumnIndex(MessagesTable.IMGLINKS));
        long timestamp = cursor.getLong(cursor.getColumnIndex(MessagesTable.DATETIME));

        final ViewHolder holder = (ViewHolder) view.getTag();
        setTextViews(holder, userName, userRole, message, timestamp);

        // TODO adjust action_list_item layout to accomodate images nicely
        if (getItemViewType(cursor) == Message.NORMAL_TYPE) {
            if (imgLinks != null) {
                final String[] imgLinksList = StringUtils.split(imgLinks, "|");
                holder.imagesButton.setVisibility(View.VISIBLE);
                holder.imagesButton.setOnClickListener(new ImagesButtonOnClickListener(holder.imageContainer, imgLinksList));

                holder.imagesButton.setText(context.getResources().getQuantityString(R.plurals.numberOfImages, imgLinksList.length, imgLinksList.length));
            } else {
                holder.imagesButton.setVisibility(View.GONE);
                holder.imageContainer.setVisibility(View.GONE);
            }
        }
    }

    private void setTextViews(ViewHolder holder, String userName, int userRole, String message, long timestamp) {
        holder.userNameView.setText(userName);
        formatUsername(userRole, holder.userNameView);
        holder.timestampView.setText(getDate(timestamp));
        if (message.isEmpty()) {
            holder.messageView.setVisibility(View.GONE);
        } else {
            holder.messageView.setVisibility(View.VISIBLE);
            holder.messageView.setText(Html.fromHtml(message));
        }
    }

    private void formatUsername(int userRole, TextView userNameView) {
        switch (userRole) {
            case ADMIN_USER_ROLE:
                userNameView.setTextColor(mAdminNameColor);
                break;
            case MOD_USER_ROLE:
                userNameView.setTextColor(mModNameColor);
                break;
            default:
                userNameView.setTextColor(mUserameColor);
                break;
        }
    }

    private CharSequence getDate(long timestamp) {
        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    static class ViewHolder {
        ViewHolder (View root) {
            layout = (RelativeLayout) root;
            userNameView = (TextView) root.findViewById(R.id.username);
            timestampView = (TextView) root.findViewById(R.id.timestamp);
            messageView = (TextView) root.findViewById(R.id.message);
            imagesButton = (Button) root.findViewById(R.id.show_images);
            imageContainer = (LinearLayout) root.findViewById(R.id.image_container);
        }

        RelativeLayout layout;
        TextView userNameView;
        TextView timestampView;
        TextView messageView;
        Button imagesButton;
        LinearLayout imageContainer;
    }

    private class ImagesButtonOnClickListener implements View.OnClickListener {

        private LinearLayout mContainer;
        private String[] mImgLinksList;

        private ImagesButtonOnClickListener(LinearLayout container, String[] imgLinksList) {
            mContainer = container;
            mImgLinksList = imgLinksList;
        }

        @Override
        public void onClick(View v) {
            if (mContainer.getVisibility() == View.VISIBLE) {
                mContainer.setVisibility(View.GONE);
            } else {
                mContainer.setVisibility(View.VISIBLE);
                showImages();
            }
        }

        private void showImages() {
            mContainer.removeAllViews();

            for (int i = 0; i < mImgLinksList.length; i++) {
                final int imgIndex = i;

                final ViewAnimator viewAnim = (ViewAnimator) LayoutInflater.from(mContainer.getContext()).inflate(R.layout.item_image, mContainer, false);
                ImageView imageView = (ImageView) viewAnim.findViewById(R.id.image);
                mContainer.addView(viewAnim);
                viewAnim.setDisplayedChild(0);

                // Trigger the download of the URL asynchronously into the image view.
                FutureCallback<ImageView> imageDisplayCallback = new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        if (e != null) {
                            viewAnim.setDisplayedChild(2);
                            return;
                        }
                        viewAnim.setDisplayedChild(1);

                        int height = result.getMaxHeight();
                        int width = result.getMaxWidth();

                        result.setLayoutParams(new FrameLayout.LayoutParams(width, height));
                        viewAnim.setLayoutParams(new LinearLayout.LayoutParams(width, height));
                        mContainer.setLayoutParams(new FrameLayout.LayoutParams(width, height));

                        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
                        alpha.setDuration(300);
                        result.startAnimation(alpha);
                        result.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(mContext, ImageActivity.class);
                                Bundle extras = new Bundle();
                                extras.putStringArray("imgLinksList", mImgLinksList);
                                extras.putInt("imgIndex", imgIndex);
                                intent.putExtras(extras);
                                mContext.startActivity(intent);
                            }
                        });
                    }
                };
                Ion.with(imageView)
                   .resize(imageView.getMaxWidth(), imageView.getMaxHeight())
                   .centerCrop()
                   .load(mImgLinksList[i])
                   .setCallback(imageDisplayCallback);
            }
        }
    }
}