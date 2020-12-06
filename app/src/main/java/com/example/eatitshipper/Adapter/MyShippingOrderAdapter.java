package com.example.eatitshipper.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eatitshipper.Common.Common;
import com.example.eatitshipper.Model.ShippingOrderModel;
import com.example.eatitshipper.R;
import com.google.android.material.button.MaterialButton;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyShippingOrderAdapter extends RecyclerView.Adapter<MyShippingOrderAdapter.MyViewHolder> {

    Context context;
    List<ShippingOrderModel> shippingOrderModelList;
    SimpleDateFormat simpleDateFormat;

    public MyShippingOrderAdapter(Context context, List<ShippingOrderModel> shippingOrderModelList) {
        this.context = context;
        this.shippingOrderModelList = shippingOrderModelList;
        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }

    @NonNull
    @Override
    public MyShippingOrderAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyShippingOrderAdapter.MyViewHolder(LayoutInflater.from(context).
                inflate(R.layout.layout_order_shipper, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyShippingOrderAdapter.MyViewHolder holder, int position) {
        Glide.with(context).load(shippingOrderModelList.get(position).getOrderModel().
                getCartItemList().get(0).getFoodImg())
                .into(holder.img_food);

        holder.txt_date.setText(new StringBuilder(simpleDateFormat.format(shippingOrderModelList.get(position).getOrderModel().getCreateDate())));
        Common.setSpanStringColor("No: ", shippingOrderModelList.get(position).getOrderModel().getKey(), holder.txt_order_number, Color.parseColor("#BA454A"));
        Common.setSpanStringColor("Address: ", shippingOrderModelList.get(position).getOrderModel().getShippingAddress(), holder.txt_order_address, Color.parseColor("#BA454A"));
        Common.setSpanStringColor("Payment: ", shippingOrderModelList.get(position).getOrderModel().getTransactionId(), holder.txt_payment, Color.parseColor("#BA454A"));

        if (shippingOrderModelList.get(position).isStartTrip()) {
            holder.btn_ship_now.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return shippingOrderModelList.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {

        @SuppressLint("NonConstantResourceId")
        @BindView(R.id.txt_date)
        TextView txt_date;
        @SuppressLint("NonConstantResourceId")
        @BindView(R.id.txt_order_address)
        TextView txt_order_address;
        @SuppressLint("NonConstantResourceId")
        @BindView(R.id.txt_order_number)
        TextView txt_order_number;
        @SuppressLint("NonConstantResourceId")
        @BindView(R.id.txt_payment)
        TextView txt_payment;
        @SuppressLint("NonConstantResourceId")
        @BindView(R.id.img_food)
        ImageView img_food;
        @SuppressLint("NonConstantResourceId")
        @BindView(R.id.btn_ship_now)
        MaterialButton btn_ship_now;

        Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}