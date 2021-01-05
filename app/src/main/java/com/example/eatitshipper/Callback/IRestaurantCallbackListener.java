package com.example.eatitshipper.Callback;

import com.example.eatitshipper.Model.RestaurantModel;

import java.util.List;

public interface IRestaurantCallbackListener {
    void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList);
    void onRestaurantLoadFailed(String message);
}
