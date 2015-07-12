/*
 * RingBufferTest.js
 */
var RingBuffer = requirejs('./RingBuffer'),
    assert = requirejs('chai').assert;

describe('RingBuffer', function () {
    'use strict';

    var ringBuffer;

    beforeEach(function () {
        ringBuffer = new RingBuffer(3);
    });

    it('initial toArray is empty', function () {
        var toArray = ringBuffer.toArray();

        assert.isArray(toArray);
        assert.deepEqual(toArray, []);
    });

    it('adds items', function () {
        ringBuffer.add(1);
        assert.deepEqual(ringBuffer.toArray(), [1]);

        ringBuffer.add(2);
        assert.deepEqual(ringBuffer.toArray(), [1, 2]);

        ringBuffer.add(3);
        assert.deepEqual(ringBuffer.toArray(), [1, 2, 3]);

        ringBuffer.add(4);
        assert.deepEqual(ringBuffer.toArray(), [2, 3, 4]);

        ringBuffer.add(5);
        assert.deepEqual(ringBuffer.toArray(), [3, 4, 5]);

        ringBuffer.add(6);
        assert.deepEqual(ringBuffer.toArray(), [4, 5, 6]);

        ringBuffer.add(7);
        assert.deepEqual(ringBuffer.toArray(), [5, 6, 7]);

        ringBuffer.add(8);
        assert.deepEqual(ringBuffer.toArray(), [6, 7, 8]);
    });

    describe('forEach', function () {

        it('iterates empty buffer', function () {
            var values = [];
            ringBuffer.forEach(function (item) {
                values.push(item);
                return true;
            });
            assert.equal(values.length, 0);
        });

        it('exits early when iterator does not return true', function () {
            ringBuffer.add(1);
            ringBuffer.add(2);
            ringBuffer.add(3);

            var values = [];
            ringBuffer.forEach(function (item) {
                values.push(item);
                return item < 2;
            });
            assert.deepEqual(values, [1, 2]);
        });

        it('iterates partial buffer', function () {
            ringBuffer.add(1);
            ringBuffer.add(2);

            var values = [];
            ringBuffer.forEach(function (item) {
                values.push(item);
                return true;
            });
            assert.deepEqual(values, [1, 2]);
        });

        it('iterates full buffer', function () {
            ringBuffer.add(1);
            ringBuffer.add(2);
            ringBuffer.add(3);
            ringBuffer.add(4);

            var values = [];
            ringBuffer.forEach(function (item) {
                values.push(item);
                return true;
            });
            assert.deepEqual(values, [2, 3, 4]);
        });
    });

    describe('forEachReverse', function () {
        it('iterates empty buffer', function () {
            var values = [];
            ringBuffer.forEachReverse(function (item) {
                values.push(item);
                return true;
            });
            assert.equal(values.length, 0);
        });

        it('iterates partial buffer', function () {
            ringBuffer.add(1);
            ringBuffer.add(2);

            var values = [];
            ringBuffer.forEachReverse(function (item) {
                values.push(item);
                return true;
            });
            assert.deepEqual(values, [2, 1]);
        });

        it('iterates full buffer', function () {
            ringBuffer.add(1);
            ringBuffer.add(2);
            ringBuffer.add(3);
            ringBuffer.add(4);

            var values = [];
            ringBuffer.forEachReverse(function (item) {
                values.push(item);
                return true;
            });
            assert.deepEqual(values, [4, 3, 2]);
        });
    });

})
;