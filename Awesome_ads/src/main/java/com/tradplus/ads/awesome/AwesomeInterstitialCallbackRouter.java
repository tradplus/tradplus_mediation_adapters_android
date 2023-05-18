package com.tradplus.ads.awesome;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class AwesomeInterstitialCallbackRouter {

    private static AwesomeInterstitialCallbackRouter instance;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showlisteners = new HashMap<>();

    public static AwesomeInterstitialCallbackRouter getInstance() {
        if(instance == null){
            instance = new AwesomeInterstitialCallbackRouter();
        }
        return instance;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
        showlisteners.remove(placementId);
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showlisteners;
    }



    public void addListener(String id, TPLoadAdapterListener listener){
        getListeners().put(id,listener);
    }


    public void addShowListener(String id, TPShowAdapterListener listener){
        getShowListeners().put(id,listener);
    }


    public TPLoadAdapterListener getListener(String id){
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id){
        return getShowListeners().get(id);
    }
}