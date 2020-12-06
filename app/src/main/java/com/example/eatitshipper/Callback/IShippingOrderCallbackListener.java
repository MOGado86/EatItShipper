package com.example.eatitshipper.Callback;

import com.example.eatitshipper.Model.ShippingOrderModel;

import java.util.List;

public interface IShippingOrderCallbackListener {
    void onShippingOrderLoadSuccess(List<ShippingOrderModel> categoryModels);
    void onShippingOrderLoadFailed(String message);
}
