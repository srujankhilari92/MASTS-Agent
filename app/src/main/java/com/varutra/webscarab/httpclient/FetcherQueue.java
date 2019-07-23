package com.varutra.webscarab.httpclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
public class FetcherQueue {
    
    private ConversationHandler _handler;
    
    private Fetcher[] _fetchers;
    private int _requestDelay;
    private long _lastRequest = 0;
    private List<Request> _requestQueue = new ArrayList<Request>();
    private boolean _running = true;
    private int _pending = 0;
    
  
    public FetcherQueue(String name, ConversationHandler handler, int threads, int requestDelay) {
        _handler = handler;
        _fetchers = new Fetcher[threads];
        _requestDelay = requestDelay;
        for (int i=0; i<threads; i++) {
            _fetchers[i] = new Fetcher(name+"-"+i);
        }
        start();
    }
    
    public void stop() {
        _running = false;
    }
    
    public void start() {
        _running = true;
        for (int i=0; i<_fetchers.length; i++) {
            _fetchers[i].start();
        }
        
    }
    
    public boolean isBusy() {
        return _pending > 0 || getRequestsQueued() > 0;
    }
    
    public void submit(Request request) {
        synchronized (_requestQueue) {
            _requestQueue.add(request);
            _requestQueue.notify();
        }
    }
    
    public int getRequestsQueued() {
        synchronized (_requestQueue) {
            return _requestQueue.size();
        }
    }
    
    public void clearRequestQueue() {
        synchronized (_requestQueue) {
            _requestQueue.clear();
        }
    }
    
    private void responseReceived(Response response) {
        _handler.responseReceived(response);
        _pending--;
    }
    
    private void requestError(Request request, IOException ioe) {
        _handler.requestError(request, ioe);
        _pending--;
    }
    
    private Request getNextRequest() {
        Request nextRequest = null;
        synchronized (_requestQueue) {
            while (_requestQueue.size() == 0) {
                try {
                    _requestQueue.wait();
                } catch (InterruptedException ie) {
                   
                }
            }
            nextRequest = (Request) _requestQueue.remove(0);
        }
        if (_requestDelay > 0) {
            long currentTimeMillis = System.currentTimeMillis();
            while (currentTimeMillis < _lastRequest + _requestDelay) {
                try {
                    Thread.sleep(_lastRequest + _requestDelay - currentTimeMillis);
                } catch (InterruptedException ie) {}
                currentTimeMillis = System.currentTimeMillis();
            }
            _lastRequest = currentTimeMillis;
        }
        _pending++;
        return nextRequest;
    }
    
    private class Fetcher extends Thread {
        public Fetcher(String name) {
            super(name);
            setDaemon(true);
            setPriority(Thread.MIN_PRIORITY);
        }
        
        public void run() {
            HTTPClient client = HTTPClientFactory.getValidInstance().getHTTPClient();
            while (_running) {
                Request request = getNextRequest();
                try {
                    Response response = client.fetchResponse(request);
                    response.flushContentStream();
                    responseReceived(response);
                } catch (IOException ioe) {
                    requestError(request, ioe);
                }
            }
        }
    }
}
