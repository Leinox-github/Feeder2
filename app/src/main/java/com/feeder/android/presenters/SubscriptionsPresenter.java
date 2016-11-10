package com.feeder.android.presenters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.feeder.android.mvp.ISubscriptionsView;
import com.feeder.android.mvp.MVPPresenter;
import com.feeder.android.utils.Category;
import com.feeder.android.utils.Constants;
import com.feeder.android.mvp.SubscriptionViewObserver;
import com.feeder.android.views.ArticleListActivity;
import com.feeder.domain.ArticleController;
import com.feeder.domain.DataObserver;
import com.feeder.domain.ResponseState;
import com.feeder.domain.SubscriptionController;
import com.feeder.model.Subscription;

import java.util.ArrayList;
import java.util.List;

import me.zsr.feeder.R;

/**
 * @description:
 * @author: Match
 * @date: 8/4/16
 */
public class SubscriptionsPresenter implements MVPPresenter, DataObserver, SubscriptionViewObserver {
    private ISubscriptionsView mSubscriptionView;
    private Context mContext;
    private List<Subscription> mSubscriptionList;
    private List<Category> mCategoryList = new ArrayList<>();

    public SubscriptionsPresenter(Context context, ISubscriptionsView subscriptionsView) {
        mContext = context;
        mSubscriptionView = subscriptionsView;
        mSubscriptionView.setObserver(this);
    }

    @Override
    public void onCreate() {
        mSubscriptionView.showLoading();
        mSubscriptionList = SubscriptionController.getInstance().getDataSource();
        mCategoryList.add(new Category(mSubscriptionList));
        mSubscriptionView.setDataSource(mCategoryList);
    }

    @Override
    public void onStart() {
        SubscriptionController.getInstance().registerObserver(this);
        ArticleController.getInstance().registerObserver(this);
        SubscriptionController.getInstance().requestData();
    }

    @Override
    public void onStop() {
        SubscriptionController.getInstance().unRegisterObserver(this);
        ArticleController.getInstance().unRegisterObserver(this);
    }

    @Override
    public void onDestroy() {
        ArticleController.getInstance().markReadTrash();
    }

    @Override
    public void onDataResponse(ResponseState state) {
        switch (state) {
            case SUCCESS:
                mCategoryList.clear();
                mCategoryList.add(new Category(mSubscriptionList));
                mSubscriptionView.hideLoading();
                mSubscriptionView.notifyDataChanged();
                break;
            case NO_CHANGE:
                mSubscriptionView.hideLoading();
                break;
        }
    }

    @Override
    public void onItemClick(View view, Subscription data) {
        Intent intent = new Intent(mContext, ArticleListActivity.class);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.KEY_BUNDLE_SUBSCRIPTION_ID, data.getId());
        intent.putExtras(bundle);
        mContext.startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(View view, final Subscription data) {
        List<CharSequence> menuList = new ArrayList<>();
        menuList.add(view.getResources().getString(R.string.mark_as_read));
        menuList.add(view.getResources().getString(R.string.remove_subscription));
        new MaterialDialog.Builder(mContext)
                .title(data.getTitle())
                .items(menuList.toArray(new CharSequence[menuList.size()]))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i,
                                            CharSequence charSequence) {
                        switch (i) {
                            case 0:
                                ArticleController.getInstance().markAllRead(data);
                                break;
                            case 1:
                                SubscriptionController.getInstance().delete(data);
                                break;
                        }
                    }
                }).show();
        return true;
    }
}
