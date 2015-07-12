define(['RingBuffer'], function (RingBuffer) {
    'use strict';

    var ws = new WebSocket('ws://' + document.location.host + '/websocket');

    var displayJitterElem = document.getElementById('displayJitter'),
        tickerCanvas = document.getElementById('tickerCanvas'),
        canvasWidth = tickerCanvas.width,
        canvasHeight = tickerCanvas.height,
        gridMarkerDistance = 100,
        zoom = 20,
        canvasCtx = tickerCanvas.getContext('2d'),
        ringBuffer = new RingBuffer(128);

    canvasCtx.strokeStyle = "grey";
    canvasCtx.fillStyle = "green";

    /**
     * Item for storing in RingBuffer.
     * @param time {!number} Relative time in milliseconds (see window.performance.now).
     * @param value {*} Value to store against this time.
     * @constructor
     */
    function BufferItem(time, value) {
        this.time = time;
        this.value = value;
    }

    function clearTicker() {
        canvasCtx.clearRect(0, 0, canvasWidth, canvasHeight);
    }

    function drawGrid() {
        canvasCtx.save();
        var i = 0;
        while (i < canvasWidth) {
            i += gridMarkerDistance;

            var gridLine = new Path2D();
            gridLine.moveTo(i, 0);
            gridLine.lineTo(i, canvasHeight);
            canvasCtx.stroke(gridLine);
        }
        canvasCtx.save();
    }

    function timeToPx(durationMs) {
        return durationMs / zoom;
    }

    function drawTickerFrame(time) {
        clearTicker();
        drawGrid();

        var pipHeight = canvasHeight / 3,
            y = (canvasHeight / 2) - (pipHeight / 2);

        ringBuffer.forEachReverse(function drawTickerItem(item) {
            var x = canvasWidth - timeToPx(time - item.time);
            canvasCtx.fillRect(x, y, 1, pipHeight);
            return x >= 0;
        });
        canvasCtx.save();
    }

    clearTicker();
    drawGrid();

    /////////

    var previousBrowser = window.performance.now(),
        previousServer = new Date().getTime(),
        jitter = 0,
        lastFrameTime = previousServer - 1;

    function animateFrame(time) {
        if (lastFrameTime == time) {
            console.warn('Frame skipped');
        } else {
            displayJitterElem.innerHTML = jitter;
            lastFrameTime = time;
        }
        drawTickerFrame(time);
        window.requestAnimationFrame(animateFrame);
    }

    window.requestAnimationFrame(animateFrame);

    ws.onmessage = function (evt) {
        var browserNowMs = window.performance.now();

        // Skip payloads that are NaN
        var serverNowNs = parseInt(evt.data);
        if (isNaN(serverNowNs)) return;

        var browserChangeMs = browserNowMs - previousBrowser;

        var serverChangeNs = serverNowNs - previousServer;
        var serverNowMs = serverNowNs / 1000000;
        var serverChangeMs = serverChangeNs / 1000000;

        jitter = Math.abs(serverChangeMs - browserChangeMs);

        ringBuffer.add(new BufferItem(browserNowMs, serverNowMs));

        //console.log('Websocket message\t- diff:', diffDiff, '\t- serverChange:', serverChangeMs, '\t- browserChange:', browserChangeMs);

        previousBrowser = browserNowMs;
        previousServer = serverNowNs;
    };

    ws.onerror = function (evt) {
        console.error("WebSocket error", evt);
    };

    ws.onopen = function (evt) {
        console.info("Websocket connected", evt);
    };

    ws.onclose = function (evt) {
        console.info("WebSocket disconnected", evt);
    };

});
