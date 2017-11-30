package com.corp.bing.addsmsverification;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView Adapter
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MyViewHolder> implements Filterable{
    private Context mContext;
    private List<Message> messages;
    private List<Message> filteredMessage;
    private MessageAdapterListener listener;
    private SparseBooleanArray selectedItems;

    // array used to perform multiple animation at once
    private SparseBooleanArray animationItemsIndex;
    private boolean reverseAllAnimations = false;

    // index is used to animate only the selected row
    // dirty fix, find a better solution
    private static int currentSelectedIndex = -1;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        @BindView(R.id.from)
        TextView from;
        @BindView(R.id.txt_primary)
        TextView subject;
        @BindView(R.id.txt_secondary)
        TextView message;
        @BindView(R.id.icon_text)
        TextView iconText;
        @BindView(R.id.timestamp)
        TextView timestamp;
        @BindView(R.id.icon_star)
        ImageView iconImp;
        @BindView(R.id.icon_profile)
        ImageView imgProfile;
        @BindView(R.id.message_container)
        LinearLayout messageContainer;
        @BindView(R.id.icon_container)
        RelativeLayout iconContainer;
        @BindView(R.id.icon_back)
        RelativeLayout iconBack;
        @BindView(R.id.icon_front)
        RelativeLayout iconFront;
        @BindView(R.id.view_foreground)
        RelativeLayout viewForeground;
        @BindView(R.id.view_background)
        RelativeLayout viewBackground;

        public MyViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            // Long Click Event를 position을 담아서 interface에 전달
            listener.onRowLongClicked(getAdapterPosition());
            // Vibration When Long Press View
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        }
    }

    // Constructor
    public MessagesAdapter(Context mContext, List<Message> messages, MessageAdapterListener listener) {
        this.mContext = mContext;
        this.messages = messages;
        this.filteredMessage = messages;
        this.listener = listener;
        selectedItems = new SparseBooleanArray();
        animationItemsIndex = new SparseBooleanArray();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        Message message = filteredMessage.get(position);

        // displaying text view data
        holder.from.setText(message.getFrom());
        holder.subject.setText(message.getSubject());
        holder.message.setText(message.getMessage());
        holder.timestamp.setText(message.getTimestamp());

        // displaying the first letter of From in icon text
        holder.iconText.setText(message.getFrom().substring(0, 1));

        // change the row state to activated
        holder.itemView.setActivated(selectedItems.get(position, false));

        // change the font style depending on message read status
        applyReadStatus(holder, message);

        // handle message star
        applyImportant(holder, message);

        // handle icon animation
        applyIconAnimation(holder, position);

        // display profile image
        applyProfilePicture(holder, message);

        // apply click events
        applyClickEvents(holder, position);
    }

    // 각각의 View에 맞는 Click Event Listener 등록
    private void applyClickEvents(MyViewHolder holder, final int position) {
        holder.iconContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onIconClicked(position);
            }
        });

        holder.iconImp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onIconImportantClicked(position);
            }
        });

        holder.messageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onMessageRowClicked(position);
            }
        });

        holder.messageContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                listener.onRowLongClicked(position);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
        });
    }

    /**
     * 클릭 이벤트에 따른 ImageView 변경 이벤트 메소드
     * @param holder            RecyclerView HolderPattern
     * @param message           Message Object
     */
    private void applyProfilePicture(MyViewHolder holder, Message message) {
        if (!TextUtils.isEmpty(message.getPicture())) {
            Glide.with(mContext).load(message.getPicture())
                    .thumbnail(0.5f)
                    .crossFade()
                    .transform(new CircleTransform(mContext))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imgProfile);
            holder.imgProfile.setColorFilter(null);
            holder.iconText.setVisibility(View.GONE);
        } else {
            holder.imgProfile.setImageResource(R.drawable.bg_circle);
            holder.imgProfile.setColorFilter(message.getColor());
            holder.iconText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * View Animation 등록
     * @param holder        RecyclerView HolderPattern
     * @param position      RecyclerView Item Position
     */
    private void applyIconAnimation(MyViewHolder holder, int position) {
        if (selectedItems.get(position, false)) {
            holder.iconFront.setVisibility(View.GONE);
            resetIconYAxis(holder.iconBack);
            holder.iconBack.setVisibility(View.VISIBLE);
            holder.iconBack.setAlpha(1);
            if (currentSelectedIndex == position) {
                FlipAnimator.flipView(mContext, holder.iconBack, holder.iconFront, true);
                resetCurrentIndex();
            }
        } else {
            holder.iconBack.setVisibility(View.GONE);
            resetIconYAxis(holder.iconFront);
            holder.iconFront.setVisibility(View.VISIBLE);
            holder.iconFront.setAlpha(1);
            if ((reverseAllAnimations && animationItemsIndex.get(position, false)) || currentSelectedIndex == position) {
                FlipAnimator.flipView(mContext, holder.iconBack, holder.iconFront, false);
                resetCurrentIndex();
            }
        }
    }


    // As the views will be reused, sometimes the icon appears as
    // flipped because older view is reused. Reset the Y-axis to 0
    private void resetIconYAxis(View view) {
        if (view.getRotationY() != 0) {
            view.setRotationY(0);
        }
    }

    // Animation Reset
    public void resetAnimationIndex() {
        reverseAllAnimations = false;
        animationItemsIndex.clear();
    }

    @Override
    public long getItemId(int position) {
        return filteredMessage.get(position).getId();
    }

    // 별표 이미지 변경 동작
    private void applyImportant(MyViewHolder holder, Message message) {
        if (message.isImportant()) {
            holder.iconImp.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_star_black_24dp));
            holder.iconImp.setColorFilter(ContextCompat.getColor(mContext, R.color.icon_tint_selected));
        } else {
            holder.iconImp.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_star_border_black_24dp));
            holder.iconImp.setColorFilter(ContextCompat.getColor(mContext, R.color.icon_tint_normal));
        }
    }

    // Check Mail isRead? or not
    private void applyReadStatus(MyViewHolder holder, Message message) {
        if (message.isRead()) {
            holder.from.setTypeface(null, Typeface.NORMAL);
            holder.subject.setTypeface(null, Typeface.NORMAL);
            holder.from.setTextColor(ContextCompat.getColor(mContext, R.color.subject));
            holder.subject.setTextColor(ContextCompat.getColor(mContext, R.color.message));
        } else {
            holder.from.setTypeface(null, Typeface.BOLD);
            holder.subject.setTypeface(null, Typeface.BOLD);
            holder.from.setTextColor(ContextCompat.getColor(mContext, R.color.from));
            holder.subject.setTextColor(ContextCompat.getColor(mContext, R.color.subject));
        }
    }

    @Override
    public int getItemCount() {
        return filteredMessage.size();
    }

    // item isSelected?
    public void toggleSelection(int pos) {
        currentSelectedIndex = pos;
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            animationItemsIndex.delete(pos);
        } else {
            selectedItems.put(pos, true);
            animationItemsIndex.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    // Clear item selection
    public void clearSelections() {
        reverseAllAnimations = true;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // get Item Selected count
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    // get Item Selected
    public List<Integer> getSelectedItems() {
        List<Integer> items =
                new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    // Remove Data
    public void removeData(int position) {
        filteredMessage.remove(position);
        resetCurrentIndex();
    }

    private void resetCurrentIndex() {
        currentSelectedIndex = -1;
    }

    /**
     * RecyclerView Click event interface
     */
    public interface MessageAdapterListener {
        void onIconClicked(int position);

        void onIconImportantClicked(int position);

        void onMessageRowClicked(int position);

        void onRowLongClicked(int position);
    }

    public void removeItem(int position) {
        filteredMessage.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(Message message, int position) {
        filteredMessage.add(position, message);
        // notify item added by position
        notifyItemInserted(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    filteredMessage = messages;
                } else {
                    List<Message> filteredList = new ArrayList<>();
                    for (Message row : messages) {

                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (row.getSubject().toLowerCase().contains(charString.toLowerCase())
                                || row.getMessage().toLowerCase().contains(charString.toLowerCase())
                                || row.getFrom().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }

                    filteredMessage = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredMessage;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredMessage = (ArrayList<Message>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}