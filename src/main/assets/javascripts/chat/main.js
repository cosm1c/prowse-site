define(['angular'], function (angular) {
    'use strict';

    var module = angular.module('prowse.chat', []);

    module.controller('ChatCtrl', ['$scope', function ($scope) {

        var ws = new WebSocket('ws://' + document.location.host + '/websocket');

        $scope.messages = [];
        $scope.sendMessage = function () {
            ws.send($scope.messageText);
            $scope.messageText = "";
        };

        ws.onmessage = function (evt) {
            console.info('WebSocket message:', evt.data);

            $scope.messages.push(evt.data);
            $scope.$apply();

            /*
             var data = JSON.parse(evt.data);
             switch (data.type) {
             default:
             console.error('Unknown message type: "' + data.type + '"');
             break;
             }
             */
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

        $scope.ws = ws;
    }]);

});
