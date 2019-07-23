/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2008, 2009 Anthony Ricaud <rik@webkit.org>
 * Copyright (C) 2011 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

importScript("Varutraproxy/logs/LogsView.js");
importScript("Varutraproxy/logs/LogsDataGridNode.js");

/**
 * @constructor
 * @extends {WebInspector.Panel}
 * @implements {WebInspector.ContextMenu.Provider}
 */
WebInspector.VarutraProxyLogsPanel = function()
{
    WebInspector.Panel.call(this, "network");
    this.registerRequiredCSS("networkPanel.css");
    this._injectStyles();

    this.createSidebarView();
    this.splitView.hideMainElement();

    this._VarutraProxyLogsView = new WebInspector.LogsView(WebInspector.LogsView._defaultColumnsVisivility);
    this._VarutraProxyLogsView.show(this.sidebarElement);

    this._viewsContainerElement = this.splitView.mainElement;
    this._viewsContainerElement.id = "network-views";
    this._viewsContainerElement.addStyleClass("hidden");
    
    this._VarutraProxyLogsView.useLargeRows = false;
    if (!this._VarutraProxyLogsView.useLargeRows)
        this._viewsContainerElement.addStyleClass("small");

    this._VarutraProxyLogsView.addEventListener(WebInspector.LogsView.EventTypes.ViewCleared, this._onViewCleared, this);
    this._VarutraProxyLogsView.addEventListener(WebInspector.LogsView.EventTypes.RowSizeChanged, this._onRowSizeChanged, this);
    this._VarutraProxyLogsView.addEventListener(WebInspector.LogsView.EventTypes.RequestSelected, this._onRequestSelected, this);
    this._VarutraProxyLogsView.addEventListener(WebInspector.LogsView.EventTypes.SearchCountUpdated, this._onSearchCountUpdated, this);
    this._VarutraProxyLogsView.addEventListener(WebInspector.LogsView.EventTypes.SearchIndexUpdated, this._onSearchIndexUpdated, this);

    this._closeButtonElement = this._viewsContainerElement.createChild("div", "close-button");
    this._closeButtonElement.id = "network-close-button";
    this._closeButtonElement.addEventListener("click", this._toggleGridMode.bind(this), false);
    this._viewsContainerElement.appendChild(this._closeButtonElement);

    function viewGetter()
    {
        return this.visibleView;
    }
    WebInspector.GoToLineDialog.install(this, viewGetter.bind(this));
    
    
    // TODO button to stop/start snapshots 
    // NetworkAgent.VarutraProxyStopSendingConnSnapshots(onStopSendingConnSnapshots.bind(this));
}

WebInspector.VarutraProxyLogsPanel.prototype = {
    
    get statusBarItems()
    {
        return this._VarutraProxyLogsView.statusBarItems;
    },

    elementsToRestoreScrollPositionsFor: function()
    {
        return this._VarutraProxyLogsView.elementsToRestoreScrollPositionsFor();
    },

    // FIXME: only used by the layout tests, should not be exposed.
    _reset: function()
    {
        this._VarutraProxyLogsView._reset();
    },

    handleShortcut: function(event)
    {
        if (this._viewingRequestMode && event.keyCode === WebInspector.KeyboardShortcut.Keys.Esc.code) {
            this._toggleGridMode();
            event.handled = true;
            return;
        }

        WebInspector.Panel.prototype.handleShortcut.call(this, event);
    },

    wasShown: function()
    {
        WebInspector.Panel.prototype.wasShown.call(this);
    },

    get requests()
    {
        return this._VarutraProxyLogsView.requests;
    },

    requestById: function(id)
    {
        return this._VarutraProxyLogsView.requestById(id);
    },

    _requestByAnchor: function(anchor)
    {
        return anchor.requestId ? this.requestById(anchor.requestId) : this._VarutraProxyLogsView._requestsByURL[anchor.href];
    },

    canShowAnchorLocation: function(anchor)
    {
        return !!this._requestByAnchor(anchor);
    },

    showAnchorLocation: function(anchor)
    {
        var request = this._requestByAnchor(anchor);
        this.revealAndHighlightRequest(request)
    },

    revealAndHighlightRequest: function(request)
    {
        this._toggleGridMode();
        if (request)
            this._VarutraProxyLogsView.revealAndHighlightRequest(request);
    },

    _onViewCleared: function(event)
    {
        this._closeVisibleRequest();
        this._toggleGridMode();
        this._viewsContainerElement.removeChildren();
        this._viewsContainerElement.appendChild(this._closeButtonElement);
    },

    _onRowSizeChanged: function(event)
    {
        this._viewsContainerElement.enableStyleClass("small", !event.data.largeRows);
    },

    _onSearchCountUpdated: function(event)
    {
        WebInspector.searchController.updateSearchMatchesCount(event.data, this);
    },

    _onSearchIndexUpdated: function(event)
    {
        WebInspector.searchController.updateCurrentMatchIndex(event.data, this);
    },

    _onRequestSelected: function(event)
    {
        this._showRequest(event.data);
    },

    _showRequest: function(request)
    {
        if (!request)
            return;

        this._toggleViewingRequestMode();

        if (this.visibleView) {
            this.visibleView.detach();
            delete this.visibleView;
        }

        var view = new WebInspector.NetworkItemView(request);
        view.show(this._viewsContainerElement);
        this.visibleView = view;
    },

    _closeVisibleRequest: function()
    {
        this.element.removeStyleClass("viewing-resource");

        if (this.visibleView) {
            this.visibleView.detach();
            delete this.visibleView;
        }
    },

    _toggleGridMode: function()
    {
        if (this._viewingRequestMode) {
            this._viewingRequestMode = false;
            this.element.removeStyleClass("viewing-resource");
            this.splitView.hideMainElement();
        }

        this._VarutraProxyLogsView.switchToDetailedView();
        this._VarutraProxyLogsView.allowPopover = true;
        this._VarutraProxyLogsView._allowRequestSelection = false;
    },

    _toggleViewingRequestMode: function()
    {
        if (this._viewingRequestMode)
            return;
        this._viewingRequestMode = true;

        this.element.addStyleClass("viewing-resource");
        this.splitView.showMainElement();
        this._VarutraProxyLogsView.allowPopover = false;
        this._VarutraProxyLogsView._allowRequestSelection = true;
        this._VarutraProxyLogsView.switchToBriefView();
    },

    /**
     * @param {string} searchQuery
     */
    performSearch: function(searchQuery)
    {
        this._VarutraProxyLogsView.performSearch(searchQuery);
    },

    /**
     * @return {boolean}
     */
    canFilter: function()
    {
        return true;
    },

    /**
     * @param {string} query
     */    
    performFilter: function(query)
    {
        this._VarutraProxyLogsView.performFilter(query);
    },

    jumpToPreviousSearchResult: function()
    {
        this._VarutraProxyLogsView.jumpToPreviousSearchResult();
    },

    jumpToNextSearchResult: function()
    {
        this._VarutraProxyLogsView.jumpToNextSearchResult();
    },

    searchCanceled: function()
    {
        this._VarutraProxyLogsView.searchCanceled();
    },

    /** 
     * @param {WebInspector.ContextMenu} contextMenu
     * @param {Object} target
     */
    appendApplicableItems: function(event, contextMenu, target)
    {
        if (!(target instanceof WebInspector.NetworkRequest))
            return;
        if (this.visibleView && this.visibleView.isShowing() && this.visibleView.request() === target)
            return;

        function reveal()
        {
            WebInspector.inspectorView.setCurrentPanel(this);
            this.revealAndHighlightRequest(/** @type {WebInspector.NetworkRequest} */ (target));
        }
        contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Reveal in Network panel" : "Reveal in Network Panel"), reveal.bind(this));
    },

    _injectStyles: function()
    {
        var style = document.createElement("style");
        var rules = [];

        var columns = WebInspector.LogsView._defaultColumnsVisivility;

        var hideSelectors = [];
        var bgSelectors = [];
        for (var columnId in columns) {
            hideSelectors.push("#network-container .hide-" + columnId + "-column ." + columnId + "-column");
            bgSelectors.push(".network-log-grid.data-grid td." + columnId + "-column");
        }
        rules.push(hideSelectors.join(", ") + "{border-right: 0 none transparent;}");
        rules.push(bgSelectors.join(", ") + "{background-color: rgba(0, 0, 0, 0.07);}");


        style.textContent = rules.join("\n");
        document.head.appendChild(style);
    },

    __proto__: WebInspector.Panel.prototype
}
